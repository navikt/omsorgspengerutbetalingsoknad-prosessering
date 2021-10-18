package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.erEtter
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.v1.PreprosesseringV1Service
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime

internal class PreprosesseringStream(
    preprosesseringV1Service: PreprosesseringV1Service,
    kafkaConfig: KafkaConfig,
    datoMottattEtter: ZonedDateTime
) {
    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(preprosesseringV1Service, datoMottattEtter),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {

        private const val NAME = "PreprosesseringV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(preprosesseringV1Service: PreprosesseringV1Service, gittDato: ZonedDateTime): Topology {
            val builder = StreamsBuilder()
            val fromMottatt = Topics.MOTTATT_V2
            val tilPreprossesert = Topics.PREPROSSESERT

            builder
                .stream(fromMottatt.name, fromMottatt.consumed)
                .filter { _, entry -> entry.deserialiserTilMelding().mottatt.erEtter(gittDato) }
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info("Preprosesserer søknad.")
                        val melding = entry.deserialiserTilMelding()
                        val preprossesertMelding = preprosesseringV1Service.preprosesser(
                            melding = melding,
                            metadata = entry.metadata
                        )
                        logger.info("Preprossesering ferdig.")
                        preprossesertMelding.serialiserTilData()
                    }
                }
                .to(tilPreprossesert.name, tilPreprossesert.produced)
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}
