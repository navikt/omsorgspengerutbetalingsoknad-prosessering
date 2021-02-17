package no.nav.helse.k9format

import no.nav.helse.prosessering.v1.Bosted
import no.nav.helse.prosessering.v1.FosterBarn
import no.nav.helse.prosessering.v1.Frilans
import no.nav.helse.prosessering.v1.MeldingV1
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
import java.math.BigDecimal
import java.time.LocalDate


fun PreprossesertMeldingV1.tilKOmsorgspengerUtbetalingSøknad(): Søknad {

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

fun MeldingV1.tilKOmsorgspengerUtbetalingSøknad(): Søknad {

    return Søknad(
        SøknadId.of(søknadId),
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
    return Utenlandsopphold(perioder)
}

private fun List<Bosted>.tilK9Bosteder(): Bosteder {
    val perioder = mutableMapOf<Periode, Bosteder.BostedPeriodeInfo>()
    forEach {
        val periode = Periode(it.fraOgMed, it.tilOgMed)
        perioder[periode] = Bosteder.BostedPeriodeInfo(Landkode.of(it.landkode))
    }

    return Bosteder(perioder)
}

private fun List<Utbetalingsperiode>.tilFraværsperiode(): List<FraværPeriode> = map {
    FraværPeriode(Periode(it.fraOgMed, it.tilOgMed), it.lengde)
}

private fun MeldingV1.arbeidAktivitet() = ArbeidAktivitet.builder()
    .frilanser(frilans?.tilK9Frilanser())
    .selvstendigNæringsdrivende(selvstendigVirksomheter.tilK9SelvstendingNæringsdrivende())
    .build()

private fun PreprossesertMeldingV1.arbeidAktivitet() = ArbeidAktivitet.builder()
    .frilanser(frilans?.tilK9Frilanser())
    .selvstendigNæringsdrivende(selvstendigVirksomheter.tilK9SelvstendingNæringsdrivende())
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
        .registrertIUtlandet(!registrertINorge.boolean)

    if (registrertINorge.boolean) infoBuilder.landkode(Landkode.NORGE)
    else infoBuilder.landkode(Landkode.of(registrertIUtlandet!!.landkode))

    næringsinntekt?.let { infoBuilder.bruttoInntekt(BigDecimal.valueOf(it.toLong())) }

    infoBuilder.erNyoppstartet(this.erNyoppstartet)

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

private fun Virksomhet.erEldreEnn3År() =
    fraOgMed.isBefore(LocalDate.now().minusYears(3)) || fraOgMed.isEqual(LocalDate.now().minusYears(3))


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
        Barn(NorskIdentitetsnummer.of(it.fødselsnummer), null)
    }
}

private fun no.nav.helse.prosessering.v1.Søker.tilK9Søker() = Søker(NorskIdentitetsnummer.of(fødselsnummer))
private fun PreprossesertSøker.tilK9Søker() = Søker(NorskIdentitetsnummer.of(fødselsnummer))
