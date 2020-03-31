package no.nav.helse.prosessering.v1.asynkron.arbeidstaker

import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.PreprosseseringV1Service
import no.nav.helse.prosessering.v1.asynkron.Topics
import no.nav.helse.prosessering.v1.asynkron.process
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class ArbeidstakerutbetalingPreprosseseringStream(
    preprosseseringV1Service: PreprosseseringV1Service,
    kafkaConfig: KafkaConfig
) {
    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(preprosseseringV1Service),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {

        private const val NAME = "ArbeidstakerutbetalingPreprosesseringV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(preprosseseringV1Service: PreprosseseringV1Service): Topology {
            val builder = StreamsBuilder()
            val fromMottatt = Topics.ARBEIDSTAKERUTBETALING_MOTTATT
            val tilPreprossesert = Topics.ARBEIDSTAKERUTBETALING_PREPROSSESERT

            builder
                .stream(
                    fromMottatt.name,
                    Consumed.with(fromMottatt.keySerde, fromMottatt.valueSerde)
                )
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {
                        logger.info("Preprosesserer søknad om utbetaling av omsorgspenger for arbeidstakere.")
                        val preprossesertMelding = preprosseseringV1Service.preprosseser(
                            melding = entry.data,
                            metadata = entry.metadata
                        )
                        logger.info("Preprossesering ferdig.")
                        preprossesertMelding
                    }
                }
                .to(tilPreprossesert.name, Produced.with(tilPreprossesert.keySerde, tilPreprossesert.valueSerde))
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}
