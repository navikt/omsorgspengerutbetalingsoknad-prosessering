package no.nav.helse.prosessering.v1.asynkron

import no.nav.helse.CorrelationId
import no.nav.helse.aktoer.AktørId
import no.nav.helse.erEtter
import no.nav.helse.joark.JoarkGateway
import no.nav.helse.joark.JoarkNavn
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.kafka.ManagedKafkaStreams
import no.nav.helse.kafka.ManagedStreamHealthy
import no.nav.helse.kafka.ManagedStreamReady
import no.nav.helse.prosessering.v1.*
import no.nav.k9.søknad.felles.*
import no.nav.k9.søknad.felles.Søker
import no.nav.k9.søknad.omsorgspenger.utbetaling.snf.OmsorgspengerUtbetalingSøknad
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory
import java.math.BigDecimal
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
            val fraPreprossesert: Topic<TopicEntry<PreprossesertMeldingV1>> = Topics.PREPROSSESERT
            val tilCleanup: Topic<TopicEntry<Cleanup>> = Topics.CLEANUP

            val mapValues = builder
                .stream(
                    fraPreprossesert.name,
                    Consumed.with(fraPreprossesert.keySerde, fraPreprossesert.valueSerde)
                )
                .filter { _, entry -> entry.data.mottatt.erEtter(gittDato) }
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
                            dokumenter = dokumenter
                        )
                        logger.info("Dokumenter journalført med ID = ${journaPostId.journalpostId}.")
                        val journalfort = Journalfort(
                            journalpostId = journaPostId.journalpostId,
                            søknad = entry.data.tilKOmsorgspengerUtbetalingSøknad()
                        )
                        Cleanup(
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

private fun PreprossesertMeldingV1.tilKOmsorgspengerUtbetalingSøknad(): OmsorgspengerUtbetalingSøknad {
    val builder = OmsorgspengerUtbetalingSøknad.builder()
        .søknadId(SøknadId.of(soknadId))
        .mottattDato(mottatt)
        .søker(søker.tilK9Søker())

    fosterbarn?.let { builder.fosterbarn(it.tilK9Barn()) }

    frilans?.let { builder.frilanser(it.tilK9Frilanser()) }

    selvstendigVirksomheter?.let { builder.selvstendigNæringsdrivende(it.tilK9SelvstendingNæringsdrivende()) }

    return builder.build()
}

private fun List<Virksomhet>.tilK9SelvstendingNæringsdrivende(): List<SelvstendigNæringsdrivende> = map {
    val builder = SelvstendigNæringsdrivende.builder()
        .virksomhetNavn(it.navnPåVirksomheten)
        .periode(
            Periode(it.fraOgMed, it.tilOgMed),
            it.tilK9SelvstendingNæringsdrivendeInfo()
        )

    it.organisasjonsnummer?.let { builder.organisasjonsnummer(Organisasjonsnummer.of(it)) }

    builder.build()
}

private fun Virksomhet.tilK9SelvstendingNæringsdrivendeInfo(): SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo {
    val infoBuilder = SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo.builder()
    infoBuilder
        .virksomhetstyper(næringstyper.tilK9Virksomhetstyper())
        .erNyoppstartet(false)
        .registrertIUtlandet(registrertINorge.boolean)

    if (registrertINorge.boolean) infoBuilder.landkode(Landkode.NORGE)
    else if (registrertIUtlandet != null) infoBuilder.landkode(Landkode.of(registrertIUtlandet.landkode))
    else infoBuilder.landkode(Landkode.NORGE) //TODO: Når frontend har vært prodsatt i mer enn 24t, kan dette fjernes.

    næringsinntekt?.let { infoBuilder.bruttoInntekt(BigDecimal.valueOf(it.toLong())) }

    yrkesaktivSisteTreFerdigliknedeÅrene?.let {
        infoBuilder.erNyoppstartet(true)
    }
    regnskapsfører?.let {
        infoBuilder
            .regnskapsførerNavn(it.navn)
            .regnskapsførerTelefon(it.telefon)
    }
    infoBuilder.erVarigEndring(false)
    varigEndring?.let {
        infoBuilder
            .erVarigEndring(true)
            .endringDato(it.dato)
            .endringBegrunnelse(it.forklaring)
    }
    return infoBuilder.build()
}

private fun List<Næringstyper>.tilK9Virksomhetstyper(): List<VirksomhetType> = map {
    when (it) {
        Næringstyper.FISKE -> VirksomhetType.FISKE
        Næringstyper.JORDBRUK_SKOGBRUK -> VirksomhetType.JORDBRUK_SKOGBRUK
        Næringstyper.DAGMAMMA -> VirksomhetType.DAGMAMMA
        Næringstyper.ANNEN -> VirksomhetType.ANNEN
    }
}

private fun Frilans.tilK9Frilanser(): Frilanser = Frilanser.builder()
    .startdato(startdato)
    .jobberFortsattSomFrilans(jobberFortsattSomFrilans)
    .build()

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
