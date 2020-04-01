package no.nav.helse.prosessering.v1.asynkron.arbeidstaker

import io.prometheus.client.Counter
import no.nav.helse.prosessering.v1.Utbetalingsperiode
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.ArbeidstakerutbetalingMelding
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

private val antallUtbetalingsperioderCounter = Counter.build()
    .name("antallArbeidstakerUtbetalingsperioderCounter")
    .help("Teller for utbetalingsperiode")
    .labelNames("antallPerioder", "antallHeleDager", "antallDelDager")
    .register()

private val utbetalingsperioderCounter = Counter.build()
    .name("arbeidstakerUtbetalingsPeriodeCounter")
    .help("Teller for utbetalingsperiode")
    .labelNames("sokerOmTimer", "sokerOmDager", "sokerOmDagerOgTimer")
    .register()

private val antallarbeidsgivereCounter = Counter.build()
    .name("arbeidsgivereCounter")
    .help("Teller for antall arbeidsgivere")
    .labelNames("antall")
    .register()

private val arbeidsgiverDetaljerCounter = Counter.build()
    .name("arbeidsgiverDetaljerCounter")
    .help("Teller for info om arbeidsgiver")
    .labelNames("harUtbetaltlonn", "harHattFraver")
    .register()

internal fun ArbeidstakerutbetalingMelding.reportMetrics() {

    antallarbeidsgivereCounter.labels(arbeidsgivere.organisasjoner.size.toString()).inc()
    arbeidsgivere.organisasjoner.forEach {
        arbeidsgiverDetaljerCounter.labels(
            it.arbeidsgiverHarUtbetaltLønn.tilJaEllerNei(),
            it.harHattFraværHosArbeidsgiver.tilJaEllerNei()
        )
    }


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
        antallDelDager += TimeUnit.HOURS.toDays(it.lengde?.toHours() ?: 0)
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
