package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.typesafe.config.ConfigFactory
import io.ktor.config.ApplicationConfig
import io.ktor.config.HoconApplicationConfig
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.stop
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.CollectorRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import no.nav.common.KafkaEnvironment
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.helse.prosessering.v1.*
import no.nav.helse.prosessering.v1.asynkron.TopicEntry
import org.junit.AfterClass
import org.junit.BeforeClass
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals


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

        private val journalføringsKonsumer = kafkaEnvironment.journalføringsKonsumer()
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
            journalføringsKonsumer.close()
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
        val melding = gyldigMelding(
            fødselsnummerSoker = gyldigFodselsnummerA
        )

        kafkaTestProducer.leggTilMottak(melding)
        journalføringsKonsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `En feilprosessert melding vil bli prosessert etter at tjenesten restartes`() {
        val melding = gyldigMelding(
            fødselsnummerSoker = gyldigFodselsnummerA
        )

        wireMockServer.stubJournalfor(500) // Simulerer feil ved journalføring

        kafkaTestProducer.leggTilMottak(melding)
        ventPaaAtRetryMekanismeIStreamProsessering()
        readyGir200HealthGir503()

        wireMockServer.stubJournalfor(201) // Simulerer journalføring fungerer igjen
        restartEngine()
        journalføringsKonsumer
            .hentJournalførtMelding(melding.søknadId)
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
        val melding = gyldigMelding(
            fødselsnummerSoker = dNummerA
        )

        kafkaTestProducer.leggTilMottak(melding)
        journalføringsKonsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    @Test
    fun `Melding lagt til prosessering selv om sletting av vedlegg feiler`() {
        val melding = gyldigMelding(
            fødselsnummerSoker = gyldigFodselsnummerA
        )

        kafkaTestProducer.leggTilMottak(melding)
        journalføringsKonsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()    }

    @Test
    fun `Melding lagt til prosessering selv om oppslag paa aktør ID for barn feiler`() {
        val melding = gyldigMelding(
            fødselsnummerSoker = gyldigFodselsnummerA
        )

        kafkaTestProducer.leggTilMottak(melding)
        preprossesertKonsumer.hentPreprossesertMelding(melding.søknadId)
    }


    @Test
    fun `Forvent 2 legeerklæringer og 2 samværsavtaler dersom den er satt i melding`() {
        val melding = gyldigMelding(
            fødselsnummerSoker = gyldigFodselsnummerA
        )

        kafkaTestProducer.leggTilMottak(melding)
        val preprossesertMelding: TopicEntry<PreprossesertMeldingV1> =
            preprossesertKonsumer.hentPreprossesertMelding(melding.søknadId)
        assertEquals(4, preprossesertMelding.data.dokumentUrls.size)
        // 2 legeerklæringsvedlegg, 2, to samværsavtalevedlegg, og 1 søknadPdf.
    }

    @Test
    fun `Forvent riktig format på journalført melding`() {
        val melding = gyldigMelding(
            fødselsnummerSoker = gyldigFodselsnummerA
        )

        kafkaTestProducer.leggTilMottak(melding)
        journalføringsKonsumer
            .hentJournalførtMelding(melding.søknadId)
            .assertJournalførtFormat()
    }

    private fun gyldigMelding(
        start: LocalDate = LocalDate.parse("2020-01-01"),
        fødselsnummerSoker: String,
        sprak: String = "nb"
    ): MeldingV1 = MeldingV1(
        søknadId = UUID.randomUUID().toString(),
        språk = sprak,
        mottatt = ZonedDateTime.now(),
        søker = Søker(
            aktørId = "123456",
            fødselsnummer = fødselsnummerSoker,
            fødselsdato = LocalDate.now().minusDays(1000),
            etternavn = "Nordmann",
            mellomnavn = "Mellomnavn",
            fornavn = "Ola"
        ),
        bosteder = listOf(
            Bosted(
                fraOgMed = start,
                tilOgMed = start.plusDays(5),
                landnavn = "Sverige",
                landkode = "SWE",
                erEØSLand = JaNei.Ja
            ),
            Bosted(
                fraOgMed = start.plusDays(10),
                tilOgMed = start.plusDays(10),
                landnavn = "Norge",
                landkode = "NOR",
                erEØSLand = JaNei.Ja
            )
        ),
        opphold = listOf(
            Bosted(
                fraOgMed = start.plusDays(15),
                tilOgMed = start.plusDays(20),
                landnavn = "England",
                landkode = "Eng",
                erEØSLand = JaNei.Ja
            ),
            Bosted(
                fraOgMed = start.minusDays(10),
                tilOgMed = start.minusDays(5),
                landnavn = "Kroatia",
                landkode = "CRO",
                erEØSLand = JaNei.Ja
            )
        ),
        spørsmål = listOf(
            SpørsmålOgSvar(
                spørsmål = "Har du vært hjemme?",
                svar = JaNei.Ja
            ),
            SpørsmålOgSvar(
                spørsmål = "Skal du være hjemme?",
                svar = JaNei.Nei
            )
        ),
        utbetalingsperioder = listOf(
            Utbetalingsperiode(
                fraOgMed = start,
                tilOgMed = start.plusDays(10),
                antallTimerBorte = Duration.ofHours(5).plusMinutes(30),
                antallTimerPlanlagt = Duration.ofHours(5).plusMinutes(30),
                årsak = FraværÅrsak.STENGT_SKOLE_ELLER_BARNEHAGE
            ),
            Utbetalingsperiode(
                fraOgMed = start.plusDays(20),
                tilOgMed = start.plusDays(20),
                antallTimerBorte = Duration.ofHours(5).plusMinutes(30),
                antallTimerPlanlagt = Duration.ofHours(5).plusMinutes(30),
                årsak = FraværÅrsak.SMITTEVERNHENSYN
            ),
            Utbetalingsperiode(
                fraOgMed = start.plusDays(30),
                tilOgMed = start.plusDays(35),
                antallTimerBorte = Duration.ofHours(5).plusMinutes(30),
                antallTimerPlanlagt = Duration.ofHours(5).plusMinutes(30),
                årsak = FraværÅrsak.ANNET
            )
        ),
        andreUtbetalinger = listOf("dagpenger", "sykepenger"),
        barn = listOf(
            Barn(
                identitetsnummer = "02119970078",
                aktørId = "123456",
                navn = "Barn Barnesen",
                aleneOmOmsorgen = true
            )
        ),
        andreBarn = listOf(
            FosterBarn(
                fødselsnummer = "02119970078"
            )
        ),
        vedlegg = listOf(
            URI("http://localhost:8080/vedlegg/1"),
            URI("http://localhost:8080/vedlegg/2"),
            URI("http://localhost:8080/vedlegg/3")
        ),
        bekreftelser = Bekreftelser(
            harBekreftetOpplysninger = JaNei.Ja,
            harForståttRettigheterOgPlikter = JaNei.Ja
        ),
        selvstendigVirksomheter = listOf(
            Virksomhet(
                næringstyper = listOf(Næringstyper.ANNEN, Næringstyper.FISKE),
                fraOgMed = LocalDate.now(),
                tilOgMed = LocalDate.now().plusDays(10),
                navnPåVirksomheten = "Kjells Møbelsnekkeri",
                registrertINorge = JaNei.Ja,
                organisasjonsnummer = "111111",
                varigEndring = VarigEndring(
                    dato = LocalDate.now().minusDays(20),
                    inntektEtterEndring = 234543,
                    forklaring = "Forklaring som handler om varig endring"
                )
            ),
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
                varigEndring = VarigEndring(
                    dato = LocalDate.now().minusDays(20),
                    inntektEtterEndring = 234543,
                    forklaring = "Forklaring som handler om varig endring"
                ),
                regnskapsfører = Regnskapsfører(
                    navn = "Bjarne Regnskap",
                    telefon = "65484578"
                )
            )
        ),
        erArbeidstakerOgså = true,
        hjemmePgaStengtBhgSkole = null,
        hjemmePgaSmittevernhensyn = null
    )

    private fun ventPaaAtRetryMekanismeIStreamProsessering() = runBlocking { delay(Duration.ofSeconds(30)) }
}
