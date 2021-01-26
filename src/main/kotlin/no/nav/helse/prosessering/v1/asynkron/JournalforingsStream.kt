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
import no.nav.helse.prosessering.v1.Bosted
import no.nav.helse.prosessering.v1.FosterBarn
import no.nav.helse.prosessering.v1.Frilans
import no.nav.helse.prosessering.v1.Næringstyper
import no.nav.helse.prosessering.v1.Opphold
import no.nav.helse.prosessering.v1.PreprossesertMeldingV1
import no.nav.helse.prosessering.v1.PreprossesertSøker
import no.nav.helse.prosessering.v1.Utbetalingsperiode
import no.nav.helse.prosessering.v1.Virksomhet
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.aktivitet.ArbeidAktivitet
import no.nav.k9.søknad.felles.aktivitet.Frilanser
import no.nav.k9.søknad.felles.aktivitet.Organisasjonsnummer
import no.nav.k9.søknad.felles.aktivitet.SelvstendigNæringsdrivende
import no.nav.k9.søknad.felles.aktivitet.VirksomhetType
import no.nav.k9.søknad.felles.fravær.FraværPeriode
import no.nav.k9.søknad.felles.personopplysninger.Barn
import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.personopplysninger.Søker
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.type.Landkode
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.ZonedDateTime
import javax.validation.Valid
import javax.validation.constraints.NotNull

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

private fun PreprossesertMeldingV1.tilKOmsorgspengerUtbetalingSøknad(): Søknad {

    return Søknad(
        SøknadId.of(soknadId),
        Versjon.of("2.0"),
        mottatt,
        søker.tilK9Søker(),
        OmsorgspengerUtbetaling(
            fosterbarn?.tilK9Barn(),
            arbeidAktivitet(),
            this.utbetalingsperioder.tilFraværsperiode(),
            this.bosteder.tilK9Bosteder(),
            this.opphold.tilK9Utenlandsopphold()
        )
    )
}

fun List<Opphold>.tilK9Utenlandsopphold(): Utenlandsopphold {
    val perioder = mutableMapOf<Periode, Utenlandsopphold.UtenlandsoppholdPeriodeInfo>()
    forEach {

        val periode = Periode(it.fraOgMed, it.tilOgMed)
        perioder[periode] = Utenlandsopphold.UtenlandsoppholdPeriodeInfo.builder()
            .land(Landkode.of(it.landkode))
            .build()
    }
    return Utenlandsopphold.builder()
        .perioder(perioder)
        .build()
}

private fun List<Bosted>.tilK9Bosteder(): @Valid @NotNull Bosteder {
    val perioder = mutableMapOf<Periode, Bosteder.BostedPeriodeInfo>()
    forEach {
        val periode = Periode(it.fraOgMed, it.tilOgMed)
        perioder[periode] = Bosteder.BostedPeriodeInfo.builder()
            .land(Landkode.of(it.landkode))
            .build()
    }

    return Bosteder(perioder)
}

private fun List<Utbetalingsperiode>.tilFraværsperiode(): List<FraværPeriode> = map {
    FraværPeriode(Periode(it.fraOgMed, it.tilOgMed), it.lengde)
}

private fun PreprossesertMeldingV1.arbeidAktivitet() = ArbeidAktivitet.builder()
    .frilanser(frilans?.tilK9Frilanser())
    .selvstendigNæringsdrivende(selvstendigVirksomheter?.tilK9SelvstendingNæringsdrivende())
    .build()

private fun List<Virksomhet>.tilK9SelvstendingNæringsdrivende(): List<SelvstendigNæringsdrivende> = map { virksomhet ->
    val builder = SelvstendigNæringsdrivende.builder()
        .virksomhetNavn(virksomhet.navnPåVirksomheten)
        .periode(
            Periode(virksomhet.fraOgMed, virksomhet.tilOgMed),
            virksomhet.tilK9SelvstendingNæringsdrivendeInfo()
        )

    virksomhet.organisasjonsnummer?.let { builder.organisasjonsnummer(Organisasjonsnummer.of(it)) }

    builder.build()
}

private fun Virksomhet.tilK9SelvstendingNæringsdrivendeInfo(): SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo {
    val infoBuilder = SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo.builder()
    infoBuilder
        .virksomhetstyper(næringstyper.tilK9Virksomhetstyper())
        .erNyoppstartet(false)
        .registrertIUtlandet(registrertINorge.boolean)

    if (registrertINorge.boolean) infoBuilder.landkode(Landkode.NORGE)
    else infoBuilder.landkode(Landkode.of(registrertIUtlandet!!.landkode))

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
