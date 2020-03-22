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

private val selvstendigNæringsdrivendeCounter = Counter.build()
    .name("selvstendigNaringsdrivendeCounter")
    .help("Teller for selvstendig næringsdrivende")
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
        )
        .inc()


    frilans?.apply {
        frilansCounter.inc()
    }

    selvstendigVirksomheter?.apply {

        this.forEach {
            val næringsTypeSomString = it.næringstyper.sortedDescending().joinToString(" , ")

            selvstendigNæringsdrivendeCounter
                .labels(
                    næringsTypeSomString,
                    it.fiskerErPåBladB.boolean.tilJaEllerNei(),
                    it.registrertINorge.boolean.tilJaEllerNei(),
                    if (it.regnskapsfører == null) "Nei" else "Ja",
                    if (it.revisor == null) "Nei" else "Ja",
                    if (it.varigEndring == null) "Nei" else "Ja",
                    if (it.yrkesaktivSisteTreFerdigliknedeÅrene == null) "Nei" else "Ja"
                )
                .inc()
        }
    }
}

private fun List<Utbetalingsperiode>.søkerBareOmTimer(): String {
    val antallTimePerioder = filter { it.lengde !== null }
        .count()

    return if (antallTimePerioder > 0 && antallTimePerioder == size) "Ja" else "Nei"
}

fun List<Utbetalingsperiode>.tilAntallHeleDager(): Double {
    var antallDager = 0L
    filter { it.lengde === null }
    map {
        antallDager += ChronoUnit.DAYS.between(it.fraOgMed, it.tilOgMed)
    }
    return antallDager.absoluteValue.toDouble()
}

fun List<Utbetalingsperiode>.tilAntallDelDager(): Double {
    var antallDelDager = 0L
    filter { it.lengde !== null }
    map {
        antallDelDager += TimeUnit.HOURS.toDays(it.lengde!!.toHours())
    }
    return antallDelDager.toDouble()
}


private fun List<Utbetalingsperiode>.søkerBareOmDager(): String {
    val antallDagPerioder = filter { it.lengde == null }
        .count()

    return if (antallDagPerioder > 0 && antallDagPerioder == size) "Ja" else "Nei"
}

private fun List<Utbetalingsperiode>.søkerOmBådeDagerOgTimer(): String {
    // Hvis søker, ikke bare søker om bare hele dager, eller søker om bare timer, er det en kombinasjon av begge.
    return if (søkerBareOmDager() === "Nei" && søkerBareOmTimer() === "Nei") "Ja" else "Nei"
}


private fun Boolean.tilJaEllerNei(): String = if (this) "Ja" else "Nei"
