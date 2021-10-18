package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.CorrelationId
import no.nav.helse.erEtter
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.formaterStatuslogging
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal class JournalforingsStream(
    joarkGateway: JoarkGateway,
    kafkaConfig: KafkaConfig,
    datoMottattEtter: ZonedDateTime
) {

    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(joarkGateway, datoMottattEtter),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val NAME = "JournalforingV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(joarkGateway: JoarkGateway, gittDato: ZonedDateTime): Topology {
            val builder = StreamsBuilder()
            val fraPreprosessert = Topics.PREPROSSESERT
            val tilCleanup = Topics.CLEANUP

            val mapValues = builder
                .stream(fraPreprosessert.name, fraPreprosessert.consumed)
                .filter { _, entry -> entry.deserialiserTilPreprosessertMelding().mottatt.erEtter(gittDato) }
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info(formaterStatuslogging(soknadId, "journalføres."))
                        val preprossesertMeldingV1 = entry.deserialiserTilPreprosessertMelding()
                        val dokumenter = preprossesertMeldingV1.dokumentUrls

                        logger.info("Journalfører dokumenter: {}", dokumenter)
                        val journaPostId = joarkGateway.journalfør(
                            mottatt = preprossesertMeldingV1.mottatt,
                            søker = preprossesertMeldingV1.søker,
                            correlationId = CorrelationId(entry.metadata.correlationId),
                            dokumenter = dokumenter
                        )
                        logger.info("Dokumenter journalført med ID = ${journaPostId.journalpostId}.")
                        val journalfort = Journalfort(
                            journalpostId = journaPostId.journalpostId,
                            søknad = preprossesertMeldingV1.k9FormatSøknad
                        )
                        Cleanup(
                            metadata = entry.metadata,
                            preprosessertMelding = preprossesertMeldingV1,
                            journalførtMelding = journalfort
                        ).serialiserTilData()
                    }
                }
            mapValues
                .to(tilCleanup.name, tilCleanup.produced)
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}
