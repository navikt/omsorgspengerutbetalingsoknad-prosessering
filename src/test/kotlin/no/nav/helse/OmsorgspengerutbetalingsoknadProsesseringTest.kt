package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import no.nav.common.KafkaEnvironment
import no.nav.helse.SøknadUtils.defaultSøknad
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.prosessering.v1.JaNei
import no.nav.helse.prosessering.v1.Land
import no.nav.helse.prosessering.v1.Næringstyper
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import no.nav.helse.prosessering.v1.Regnskapsfører
import no.nav.helse.prosessering.v1.VarigEndring
import no.nav.helse.prosessering.v1.Virksomhet
import no.nav.helse.prosessering.v1.YrkesaktivSisteTreFerdigliknedeÅrene
import no.nav.helse.prosessering.v1.asynkron.TopicEntry
import org.json.JSONObject
import org.junit.AfterClass
import org.junit.BeforeClass
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


@KtorExperimentalAPI
class OmsorgspengerutbetalingsoknadProsesseringTest {

    @KtorExperimentalAPI
    private companion object {

        private val logger: Logger = LoggerFactory.getLogger(OmsorgspengerutbetalingsoknadProsesseringTest::class.java)

        private val wireMockServer: WireMockServer = WireMockBuilder()
            .withAzureSupport()
            .build()
            .stubK9DokumentHealth()
            .stubK9JoarkHealth()
            .stubJournalfor()
            .stubLagreDokument()
            .stubSlettDokument()

        private val kafkaEnvironment = KafkaWrapper.bootstrap()
        private val kafkaTestProducer = kafkaEnvironment.meldingsProducer()

        private val cleanupKonsumer = kafkaEnvironment.cleanupKonsumer()
        private val preprossesertKonsumer = kafkaEnvironment.preprossesertKonsumer()

        // Se https://github.com/navikt/dusseldorf-ktor#f%C3%B8dselsnummer
        private val gyldigFodselsnummerA = "02119970078"
        private val gyldigFodselsnummerB = "19066672169"
        private val gyldigFodselsnummerC = "20037473937"
        private val dNummerA = "55125314561"

        private var engine = newEngine(kafkaEnvironment).apply {
            start(wait = true)
        }

        private fun getConfig(kafkaEnvironment: KafkaEnvironment?): ApplicationConfig {
            val fileConfig = ConfigFactory.load()
            val testConfig = ConfigFactory.parseMap(
                TestConfiguration.asMap(
                    wireMockServer = wireMockServer,
                    kafkaEnvironment = kafkaEnvironment
                )
            )
            val mergedConfig = testConfig.withFallback(fileConfig)
            return HoconApplicationConfig(mergedConfig)
        }

        private fun newEngine(kafkaEnvironment: KafkaEnvironment?) = TestApplicationEngine(createTestEnvironment {
            config = getConfig(kafkaEnvironment)
        })

        private fun stopEngine() = engine.stop(5, 60, TimeUnit.SECONDS)

        internal fun restartEngine() {
            stopEngine()
            CollectorRegistry.defaultRegistry.clear()
            engine = newEngine(kafkaEnvironment)
            engine.start(wait = true)
        }

        @BeforeClass
        @JvmStatic
        fun buildUp() {
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            logger.info("Tearing down")
            wireMockServer.stop()
            cleanupKonsumer.close()
            kafkaTestProducer.close()
            stopEngine()
            kafkaEnvironment.tearDown()
            logger.info("Tear down complete")
        }
    }

    @Test
    fun `test isready, isalive, health og metrics`() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                handleRequest(HttpMethod.Get, "/isalive") {}.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    handleRequest(HttpMethod.Get, "/metrics") {}.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                        handleRequest(HttpMethod.Get, "/health") {}.apply {
                            assertEquals(HttpStatusCode.OK, response.status())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `Gylding melding blir prosessert av journalføringskonsumer`() {
        val melding = defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            søker = defaultSøknad.søker.copy(
                fødselsnummer = gyldigFodselsnummerA
            )
        )

        kafkaTestProducer.leggTilMottak(melding)
        cleanupKonsumer
            .hentCleanupMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `En feilprosessert melding vil bli prosessert etter at tjenesten restartes`() {

        val melding = defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            søker = defaultSøknad.søker.copy(
                fødselsnummer = gyldigFodselsnummerA
            )
        )

        wireMockServer.stubJournalfor(500) // Simulerer feil ved journalføring

        kafkaTestProducer.leggTilMottak(melding)
        ventPaaAtRetryMekanismeIStreamProsessering()
        readyGir200HealthGir503()

        wireMockServer.stubJournalfor(201) // Simulerer journalføring fungerer igjen
        restartEngine()
        cleanupKonsumer
            .hentCleanupMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    private fun readyGir200HealthGir503() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                handleRequest(HttpMethod.Get, "/health") {}.apply {
                    assertEquals(HttpStatusCode.ServiceUnavailable, response.status())
                }
            }
        }
    }

    @Test
    fun `Melding som gjeder søker med D-nummer`() {
        val melding = defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            søker = defaultSøknad.søker.copy(
                fødselsnummer = dNummerA
            )
        )


        kafkaTestProducer.leggTilMottak(melding)
        cleanupKonsumer
            .hentCleanupMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Melding lagt til prosessering selv om sletting av vedlegg feiler`() {

        val melding = defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            søker = defaultSøknad.søker.copy(
                fødselsnummer = gyldigFodselsnummerA
            )
        )

        kafkaTestProducer.leggTilMottak(melding)
        cleanupKonsumer
            .hentCleanupMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Melding lagt til prosessering selv om oppslag paa aktør ID for barn feiler`() {

        val melding = defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            søker = defaultSøknad.søker.copy(
                fødselsnummer = gyldigFodselsnummerA
            )
        )

        kafkaTestProducer.leggTilMottak(melding)
        preprossesertKonsumer.hentPreprossesertMelding(melding.søknadId)
    }


    @Test
    fun `Forvent 2 legeerklæringer og 2 samværsavtaler dersom den er satt i melding`() {

        val melding = defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            søker = defaultSøknad.søker.copy(
                fødselsnummer = gyldigFodselsnummerA
            )
        )

        kafkaTestProducer.leggTilMottak(melding)
        val preprossesertMelding: TopicEntry<PreprossesertMeldingV1> =
            preprossesertKonsumer.hentPreprossesertMelding(melding.søknadId)
        assertEquals(4, preprossesertMelding.data.dokumentUrls.size)
        // 2 legeerklæringsvedlegg, 2, to samværsavtalevedlegg, og 1 søknadPdf.

    }

    @Test
    fun `Forvent riktig format på journalført melding`() {
        val melding = defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            søker = defaultSøknad.søker.copy(
                fødselsnummer = gyldigFodselsnummerA
            )
        )

        kafkaTestProducer.leggTilMottak(melding)
        cleanupKonsumer
            .hentCleanupMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Tester virksomhet registrert i utlandet`() {
        val melding = defaultSøknad.copy(
            søknadId = UUID.randomUUID().toString(),
            søker = defaultSøknad.søker.copy(
                fødselsnummer = gyldigFodselsnummerA
            ),
            selvstendigVirksomheter = listOf(
                Virksomhet(
                    næringstyper = listOf(Næringstyper.JORDBRUK_SKOGBRUK, Næringstyper.DAGMAMMA, Næringstyper.FISKE),
                    fiskerErPåBladB = JaNei.Ja,
                    fraOgMed = LocalDate.now(),
                    næringsinntekt = 1111,
                    navnPåVirksomheten = "Tull Og Tøys",
                    registrertINorge = JaNei.Nei,
                    registrertIUtlandet = Land(
                        landkode = "DK",
                        landnavn = "Danmark"
                    ),
                    yrkesaktivSisteTreFerdigliknedeÅrene = YrkesaktivSisteTreFerdigliknedeÅrene(LocalDate.now()),
                    erNyoppstartet = true,
                    varigEndring = VarigEndring(
                        dato = LocalDate.now().minusDays(20),
                        inntektEtterEndring = 234543,
                        forklaring = "Forklaring som handler om varig endring"
                    ),
                    regnskapsfører = Regnskapsfører(
                        navn = "Bjarne Regnskap",
                        telefon = "65484578"
                    ),
                    harFlereAktiveVirksomheter = true
                )
            )
        )

        kafkaTestProducer.leggTilMottak(melding)
        cleanupKonsumer
            .hentCleanupMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    private fun ventPaaAtRetryMekanismeIStreamProsessering() = runBlocking { delay(Duration.ofSeconds(30)) }
}
