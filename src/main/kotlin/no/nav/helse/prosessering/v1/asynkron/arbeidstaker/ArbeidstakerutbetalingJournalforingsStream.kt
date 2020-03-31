package no.nav.helse.prosessering.v1.asynkron.arbeidstaker

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktørId
import no.nav.helse.joark.Arbeidstype
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.joark.JoarkNavn
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.v1.FosterBarn
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import no.nav.helse.prosessering.v1.PreprossesertSøker
import no.nav.helse.prosessering.v1.asynkron.*
import no.nav.helse.prosessering.v1.asynkron.Topic
import no.nav.helse.prosessering.v1.asynkron.Topics
import no.nav.k9.søknad.felles.Barn
import no.nav.k9.søknad.felles.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.Søker
import no.nav.k9.søknad.felles.SøknadId
import no.nav.k9.søknad.omsorgspenger.utbetaling.OmsorgspengerUtbetalingSøknad
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.PreprosessertArbeidstakerutbetalingMelding
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

internal class ArbeidstakerutbetalingJournalforingsStream(
    joarkGateway: JoarkGateway,
    kafkaConfig: KafkaConfig
) {

    private val stream = ManagedKafkaStreams(
        name = NAME,
        properties = kafkaConfig.stream(NAME),
        topology = topology(joarkGateway),
        unreadyAfterStreamStoppedIn = kafkaConfig.unreadyAfterStreamStoppedIn
    )

    internal val ready = ManagedStreamReady(stream)
    internal val healthy = ManagedStreamHealthy(stream)

    private companion object {
        private const val NAME = "ArbeidstakerutbetalingJournalforingV1"
        private val logger = LoggerFactory.getLogger("no.nav.$NAME.topology")

        private fun topology(joarkGateway: JoarkGateway): Topology {
            val builder = StreamsBuilder()
            val fraPreprossesert = Topics.ARBEIDSTAKERUTBETALING_PREPROSSESERT
            val tilCleanup = Topics.ARBEIDSTAKERUTBETALING_CLEANUP

            val mapValues = builder
                .stream(fraPreprossesert.name, Consumed.with(fraPreprossesert.keySerde, fraPreprossesert.valueSerde))
                .filter { _, entry -> 1 == entry.metadata.version }
                .mapValues { soknadId, entry ->
                    process(NAME, soknadId, entry) {

                        val dokumenter = entry.data.dokumentUrls
                        logger.info("Journalfører dokumenter: {}", dokumenter)
                        val journaPostId = joarkGateway.journalfør(
                            mottatt = entry.data.mottatt,
                            aktørId = AktørId(entry.data.søker.aktørId),
                            norskIdent = entry.data.søker.fødselsnummer,
                            navn = JoarkNavn(
                                fornavn = entry.data.søker.fornavn,
                                mellomnanvn = entry.data.søker.mellomnavn,
                                etternavn = entry.data.søker.etternavn
                            ),
                            correlationId = CorrelationId(entry.metadata.correlationId),
                            dokumenter = dokumenter,
                            arbeidstype = Arbeidstype.ARBEIDSTAKER
                        )
                        logger.info("Dokumenter journalført med ID = ${journaPostId.journalpostId}.")
                        val journalfort = ArbeidstakerutbetalingJournalfort(
                            journalpostId = journaPostId.journalpostId,
                            søknad = entry.data.tilKOmsorgspengerUtbetalingSøknad()
                        )
                        ArbeidstakerutbetalingCleanup(
                            metadata = entry.metadata,
                            melding = entry.data,
                            journalførtMelding = journalfort
                        )
                    }
                }
            mapValues
                .to(tilCleanup.name, Produced.with(tilCleanup.keySerde, tilCleanup.valueSerde))
            return builder.build()
        }
    }

    internal fun stop() = stream.stop(becauseOfError = false)
}

private fun PreprosessertArbeidstakerutbetalingMelding.tilKOmsorgspengerUtbetalingSøknad(): OmsorgspengerUtbetalingSøknad {
    val builder = OmsorgspengerUtbetalingSøknad.builder()
        .søknadId(SøknadId.of(soknadId))
        .mottattDato(mottatt)
        .søker(søker.tilK9Søker())

    fosterbarn?.let { builder.barn(it.tilK9Barn()) }

    return builder.build()
}

private fun List<FosterBarn>.tilK9Barn(): List<Barn> {
    return map {
        Barn.builder()
            .norskIdentitetsnummer(NorskIdentitetsnummer.of(it.fødselsnummer))
            .build()
    }
}

private fun PreprossesertSøker.tilK9Søker() = Søker.builder()
    .norskIdentitetsnummer(NorskIdentitetsnummer.of(fødselsnummer))
    .build()
