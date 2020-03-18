package no.nav.helse.prosessering.v1

import io.prometheus.client.Counter
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

private val ZONE_ID = ZoneId.of("Europe/Oslo")

private val jaNeiCounter = Counter.build()
    .name("ja_nei_counter")
    .help("Teller for svar på ja/nei spørsmål i søknaden")
    .labelNames("spm", "svar")
    .register()

internal fun PreprossesertMeldingV1.reportMetrics() {

}

private fun Boolean.tilJaEllerNei(): String = if (this) "Ja" else "Nei"
