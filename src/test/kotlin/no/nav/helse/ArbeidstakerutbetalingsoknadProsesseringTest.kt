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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import no.nav.common.KafkaEnvironment
import no.nav.helse.ArbeidstakerutbetalingSøknadUtils.defaultArbeidstakerutbetalingMelding
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import org.junit.AfterClass
import org.junit.BeforeClass
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals


@KtorExperimentalAPI
class ArbeidstakerutbetalingsoknadProsesseringTest {

    @KtorExperimentalAPI
    private companion object {

        private val logger: Logger = LoggerFactory.getLogger(ArbeidstakerutbetalingsoknadProsesseringTest::class.java)

        private val wireMockServer: WireMockServer = WireMockBuilder()
            .withAzureSupport()
            .build()
            .stubK9DokumentHealth()
            .stubK9JoarkHealth()
            .stubJournalforArbeidstaker()
            .stubLagreDokument()
            .stubSlettDokument()

        private val kafkaEnvironment = KafkaWrapper.bootstrap()
        private val kafkaProducer = kafkaEnvironment.arbeidstakerutbetalingMeldingProducer()
        private val journalføringsKonsumer = kafkaEnvironment.arbeidstakerutbetalingJournalføringsKonsumer()

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
            kafkaProducer.close()
            journalføringsKonsumer.close()
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
    fun `En feilprosessert melding vil bli prosessert etter at tjenesten restartes`() {
        val melding = defaultArbeidstakerutbetalingMelding.copy(
            søknadId = UUID.randomUUID().toString(),
            søker = defaultArbeidstakerutbetalingMelding.søker.copy(fødselsnummer = gyldigFodselsnummerA)
        )

        wireMockServer.stubJournalforArbeidstaker(500) // Simulerer feil ved journalføring

        kafkaProducer.leggTilMottak(melding)
        ventPaaAtRetryMekanismeIStreamProsessering()
        readyGir200HealthGir503()

        wireMockServer.stubJournalforArbeidstaker(201) // Simulerer journalføring fungerer igjen
        restartEngine()
        journalføringsKonsumer
            .hentJournalførArbeidstakerutbetalingtMelding(melding.søknadId)
            .assertArbeidstakerutbetalingJournalførtFormat()
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
        val melding = defaultArbeidstakerutbetalingMelding.copy(
            søknadId = UUID.randomUUID().toString(),
            søker = defaultArbeidstakerutbetalingMelding.søker.copy(fødselsnummer = dNummerA)
        )

        kafkaProducer.leggTilMottak(melding)
        journalføringsKonsumer
            .hentJournalførArbeidstakerutbetalingtMelding(melding.søknadId)
            .assertArbeidstakerutbetalingJournalførtFormat()
    }

    @Test
    fun `Forvent riktig format på journalført melding`() {
        val melding = defaultArbeidstakerutbetalingMelding.copy(
            søknadId = UUID.randomUUID().toString(),
            søker = defaultArbeidstakerutbetalingMelding.søker.copy(fødselsnummer = gyldigFodselsnummerA)
        )

        kafkaProducer.leggTilMottak(melding)
        journalføringsKonsumer
            .hentJournalførArbeidstakerutbetalingtMelding(melding.søknadId)
            .assertArbeidstakerutbetalingJournalførtFormat()
    }

    private fun ventPaaAtRetryMekanismeIStreamProsessering() = runBlocking { delay(Duration.ofSeconds(30)) }
}
