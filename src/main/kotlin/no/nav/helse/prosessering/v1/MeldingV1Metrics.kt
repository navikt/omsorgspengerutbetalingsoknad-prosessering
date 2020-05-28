package no.nav.helse.prosessering.v1

import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

private val opplastedeVedleggHistogram = Histogram.build()
    .buckets(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
    .name("antall_oppplastede_vedlegg_histogram")
    .help("Antall vedlegg lastet opp i søknader")
    .register()

private val frilansCounter = Counter.build()
    .name("frilansCounter")
    .help("Teller for frilans")
    .register()

private val virksomhetsCounter = Counter.build()
    .name("virksomhetsCounter")
    .help("Teller for virksomheter pr. søknad.")
    .labelNames(
        "naringstyper",
        "fiskerErPaBladB",
        "registrertINorge",
        "harRegnskapsforer",
        "harRevisor",
        "harVarigEndring",
        "nyligOppstartetVirksomhet"
    )
    .register()

private val selvstendigNæringsdrivendeOgFrilans = Counter.build()
    .name("selvstendigNaringsdrivendeOgFrilans")
    .help("Teller for selvstending næringsdrivende og frilans")
    .register()

private val selvstendigNæringsdrivendeFrilansOgArbeidstaker = Counter.build()
    .name("selvstendigNaringsdrivendeFrilansOgArbeidstaker")
    .help("Teller for selvstending næringsdrivende, frilans og arbeidstaker")
    .register()

private val selvstendingNæringsdrivendeOgArbeidstaker = Counter.build()
    .name("selvstendigNaringsdrivendeOgArbeidstaker")
    .help("Teller for selvstending næringsdrivende og arbeidstaker")
    .register()

private val frilansOgArbeidstaker = Counter.build()
    .name("frilansOgArbeidstaker")
    .help("Teller for frilans og arbeidstaker")
    .register()

private val selvstendigVirksomhetCounter = Counter.build()
    .name("selvstendigNaringsdrivendeCounter")
    .help("Teller for selvstending næringsdrivende")
    .register()


private val antallUtbetalingsperioderCounter = Counter.build()
    .name("antallUtbetalingsperioderCounter")
    .help("Teller for utbetalingsperiode")
    .labelNames("antallPerioder", "antallHeleDager", "antallDelDager")
    .register()

private val utbetalingsperioderCounter = Counter.build()
    .name("utbetalingsPeriodeCounter")
    .help("Teller for utbetalingsperiode")
    .labelNames("sokerOmTimer", "sokerOmDager", "sokerOmDagerOgTimer")
    .register()

private val særligeSmittevernhensynCounter = Counter.build()
    .name("serligeSmittevernhensynCounter")
    .help("Teller for info om særlige smittevernhensyn")
    .labelNames("blirHjemme", "harVedleggLastetOpp")
    .register()

internal fun MeldingV1.reportMetrics() {
    opplastedeVedleggHistogram.observe(vedlegg.size.toDouble())

    utbetalingsperioderCounter.labels(
        this.utbetalingsperioder.søkerBareOmTimer(),
        this.utbetalingsperioder.søkerBareOmDager(),
        this.utbetalingsperioder.søkerOmBådeDagerOgTimer()
    ).inc()

    antallUtbetalingsperioderCounter.labels(
        this.utbetalingsperioder.size.toString(),
        this.utbetalingsperioder.tilAntallHeleDager().toString(),
        this.utbetalingsperioder.tilAntallDelDager().toString()
    ).inc()

    when {
        erFrilanser() ->
            frilansCounter.inc()

        erSelvstendigNæringsdrivende() -> {
            selvstendigVirksomhetCounter.inc()
            virksomheterMetric()
        }

        erFrilanserOgSelvstendigNæringsdrivende() ->
            selvstendigNæringsdrivendeOgFrilans.inc()

        erFrilanserOgArbeidstaker() ->
            frilansOgArbeidstaker.inc()

        erSelvstendingNæringsdrivendeOgArbeidstaker() -> {
            selvstendingNæringsdrivendeOgArbeidstaker.inc()
            virksomheterMetric()
        }

        erSelvstendingNæringsdrivendeFrilansOgArbeidstaker() -> {
            selvstendigNæringsdrivendeFrilansOgArbeidstaker.inc()
            virksomheterMetric()
        }
    }

    særligeSmittevernhensynCounter
        .labels(hjemmePgaSmittevernhensyn.tilJaEllerNei(), vedlegg.isNotEmpty().tilJaEllerNei())
        .inc()
}

private fun MeldingV1.virksomheterMetric() {

    selvstendigVirksomheter?.forEach {
        val næringsTypeSomString = it.næringstyper.sortedDescending().joinToString(" , ")
        virksomhetsCounter
            .labels(
                næringsTypeSomString,
                it.fiskerErPåBladB?.boolean?.tilJaEllerNei() ?: "Nei",
                it.registrertINorge.boolean.tilJaEllerNei(),
                if (it.regnskapsfører == null) "Nei" else "Ja",
                if (it.revisor == null) "Nei" else "Ja",
                if (it.varigEndring == null) "Nei" else "Ja",
                if (it.yrkesaktivSisteTreFerdigliknedeÅrene == null) "Nei" else "Ja"
            )
            .inc()
    }
}

private fun MeldingV1.erFrilanser() = selvstendigVirksomheter == null && frilans != null && erArbeidstakerOgså == false
private fun MeldingV1.erSelvstendigNæringsdrivende() =
    selvstendigVirksomheter != null && frilans == null && erArbeidstakerOgså == false

private fun MeldingV1.erFrilanserOgSelvstendigNæringsdrivende() =
    selvstendigVirksomheter != null && frilans != null && erArbeidstakerOgså == false

private fun MeldingV1.erFrilanserOgArbeidstaker() =
    selvstendigVirksomheter == null && frilans != null && erArbeidstakerOgså == true

private fun MeldingV1.erSelvstendingNæringsdrivendeOgArbeidstaker() =
    selvstendigVirksomheter != null && frilans == null && erArbeidstakerOgså

private fun MeldingV1.erSelvstendingNæringsdrivendeFrilansOgArbeidstaker() =
    selvstendigVirksomheter != null && frilans != null && erArbeidstakerOgså

private fun List<Utbetalingsperiode>.søkerBareOmTimer(): String {
    val antallTimePerioder = filter { it.antallTimerBorte !== null }
        .count()

    return if (antallTimePerioder > 0 && antallTimePerioder == size) "Ja" else "Nei"
}

fun List<Utbetalingsperiode>.tilAntallHeleDager(): Double {
    var antallDager = 0L
    filter { it.antallTimerBorte === null }
    map {
        antallDager += ChronoUnit.DAYS.between(it.fraOgMed, it.tilOgMed)
    }
    return antallDager.absoluteValue.toDouble()
}

fun List<Utbetalingsperiode>.tilAntallDelDager(): Double {
    var antallDelDager = 0L
    filter { it.antallTimerBorte !== null }
    map {
        antallDelDager += TimeUnit.HOURS.toDays(it.antallTimerBorte?.toHours() ?: 0)
    }
    return antallDelDager.toDouble()
}


private fun List<Utbetalingsperiode>.søkerBareOmDager(): String {
    val antallDagPerioder = filter { it.antallTimerBorte == null }
        .count()

    return if (antallDagPerioder > 0 && antallDagPerioder == size) "Ja" else "Nei"
}

private fun List<Utbetalingsperiode>.søkerOmBådeDagerOgTimer(): String {
    // Hvis søker, ikke bare søker om bare hele dager, eller søker om bare timer, er det en kombinasjon av begge.
    return if (søkerBareOmDager() === "Nei" && søkerBareOmTimer() === "Nei") "Ja" else "Nei"
}


private fun Boolean.tilJaEllerNei(): String = if (this) "Ja" else "Nei"
