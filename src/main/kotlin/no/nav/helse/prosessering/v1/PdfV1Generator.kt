package no.nav.helse.prosessering.v1

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.util.XRLog
import no.nav.helse.dusseldorf.ktor.core.fromResources
import no.nav.helse.omsorgspengerKonfiguert
import no.nav.helse.prosessering.v1.PdfV1Generator.Companion.DATE_FORMATTER
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level

internal class PdfV1Generator {
    companion object {
        private val mapper = jacksonObjectMapper().omsorgspengerKonfiguert()

        private const val ROOT = "handlebars"
        private const val SOKNAD = "soknad"

        private val REGULAR_FONT = "$ROOT/fonts/SourceSansPro-Regular.ttf".fromResources().readBytes()
        private val BOLD_FONT = "$ROOT/fonts/SourceSansPro-Bold.ttf".fromResources().readBytes()
        private val ITALIC_FONT = "$ROOT/fonts/SourceSansPro-Italic.ttf".fromResources().readBytes()

        private val sRGBColorSpace = "$ROOT/sRGB.icc".fromResources().readBytes()

        private val handlebars = Handlebars(ClassPathTemplateLoader("/$ROOT")).apply {
            registerHelper("eq", Helper<String> { context, options ->
                if (context == options.param(0)) options.fn() else options.inverse()
            })
            registerHelper("enumNæringstyper", Helper<String> { context, _ ->
                Næringstyper.valueOf(context).beskrivelse
            })
            registerHelper("fritekst", Helper<String> { context, _ ->
                if (context == null) "" else {
                    val text = Handlebars.Utils.escapeExpression(context)
                        .toString()
                        .replace(Regex("\\r\\n|[\\n\\r]"), "<br/>")
                    Handlebars.SafeString(text)
                }
            })
            registerHelper("dato", Helper<String> { context, _ ->
                DATE_FORMATTER.format(LocalDate.parse(context))
            })
            registerHelper("storForbokstav", Helper<String> { context, _ ->
                context.capitalize()
            })
            registerHelper("tidspunkt", Helper<String> { context, _ ->
                DATE_TIME_FORMATTER.format(ZonedDateTime.parse(context))
            })
            registerHelper("varighet", Helper<String> { context, _ ->
                Duration.parse(context).tilString()
            })
            registerHelper("jaNeiSvar", Helper<Boolean> { context, _ ->
                if (context == true) "Ja" else "Nei"
            })
            registerHelper("årsak", Helper<String> { context, _ ->
                when(FraværÅrsak.valueOf(context)) {
                    FraværÅrsak.ORDINÆRT_FRAVÆR -> "Ordinært fravær"
                    FraværÅrsak.STENGT_SKOLE_ELLER_BARNEHAGE -> "Stengt skole eller barnehage"
                    FraværÅrsak.SMITTEVERNHENSYN -> "Smittevernhensyn"
                }
            })
            registerHelper("aktivitetFravær", Helper<String> { context, _ ->
                when(AktivitetFravær.valueOf(context)) {
                    AktivitetFravær.FRILANSER -> "frilanser"
                    AktivitetFravær.SELVSTENDIG_VIRKSOMHET -> "selvstendig næringsdrivende"
                }
            })
            registerHelper("fraværSomFormat", Helper<List<String>> { context, _ ->
                when(context.size) {
                    1 -> "Fravær som"
                    2 -> "Fravær både som"
                    else -> ""
                }
            })

            infiniteLoops(true)
        }

        private val soknadTemplate = handlebars.compile(SOKNAD)

        private val ZONE_ID = ZoneId.of("Europe/Oslo")
        val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZONE_ID)
        val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZONE_ID)
    }

    internal fun genererSøknadOppsummeringPdf(
        melding: MeldingV1
    ): ByteArray {
        val mottatt = melding.mottatt.toLocalDate()
        XRLog.listRegisteredLoggers().forEach { logger -> XRLog.setLevel(logger, Level.WARNING) }
        soknadTemplate.apply(
            Context
                .newBuilder(
                    mapOf(
                        "søknad" to melding.somMap(),
                        "språk" to melding.språk.sprakTilTekst(),
                        "mottaksUkedag" to melding.mottatt.withZoneSameInstant(ZONE_ID).norskDag(),
                        "søker" to mapOf(
                            "formatertNavn" to melding.søker.formatertNavn().capitalizeName()
                        ),
                        "medlemskap" to mapOf(
                            "siste12" to melding.bosteder.any {
                                it.fraOgMed.isBefore(mottatt) || it.tilOgMed.isEqual(mottatt)
                            },
                            "neste12" to melding.bosteder.any {
                                it.fraOgMed.isEqual(mottatt) || it.fraOgMed.isAfter(mottatt)
                            }
                        ),
                        "harFosterbarn" to melding.fosterbarn?.isNotEmpty(),
                        "harOpphold" to melding.opphold.isNotEmpty(),
                        "harSøktAndreYtelser" to melding.andreUtbetalinger.isNotEmpty(),
                        "ikkeHarSendtInnVedlegg" to melding.vedleggId.isEmpty(),
                        "harBosteder" to melding.bosteder.isNotEmpty(),
                        "bekreftelser" to melding.bekreftelser.bekreftelserSomMap(),
                        "selvstendigNæringsdrivende" to melding.selvstendigNæringsdrivende?.somMap()
                    )
                )
                .resolver(MapValueResolver.INSTANCE)
                .build()
        ).let { html ->
            val outputStream = ByteArrayOutputStream()

            PdfRendererBuilder()
                .useFastMode()
                .usePdfUaAccessbility(true)
                .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_1_B)
                .useColorProfile(sRGBColorSpace)
                .withHtmlContent(html, "")
                .medFonter()
                .toStream(outputStream)
                .buildPdfRenderer()
                .createPDF()

            return outputStream.use {
                it.toByteArray()
            }
        }
    }

    private fun PdfRendererBuilder.medFonter() =
        useFont(
            { ByteArrayInputStream(REGULAR_FONT) },
            "Source Sans Pro",
            400,
            BaseRendererBuilder.FontStyle.NORMAL,
            false
        )
            .useFont(
                { ByteArrayInputStream(BOLD_FONT) },
                "Source Sans Pro",
                700,
                BaseRendererBuilder.FontStyle.NORMAL,
                false
            )
            .useFont(
                { ByteArrayInputStream(ITALIC_FONT) },
                "Source Sans Pro",
                400,
                BaseRendererBuilder.FontStyle.ITALIC,
                false
            )

    private fun MeldingV1.somMap() = mapper.convertValue(
        this,
        object :
            TypeReference<MutableMap<String, Any?>>() {}
    )

}

private fun Bekreftelser.bekreftelserSomMap(): Map<String, Boolean> {
    return mapOf(
        "harBekreftetOpplysninger" to harBekreftetOpplysninger.boolean,
        "harForståttRettigheterOgPlikter" to harForståttRettigheterOgPlikter.boolean
    )
}

private fun Duration.tilString(): String = when (this.toMinutesPart()) {
    0 -> "${this.toHours()} timer"
    else -> "${this.toHoursPart()} timer og ${this.toMinutesPart()} minutter"
}

private fun Søker.formatertNavn() = if (mellomnavn != null) "$fornavn $mellomnavn $etternavn" else "$fornavn $etternavn"

fun String.capitalizeName(): String = split(" ").joinToString(" ") { it.lowercase().capitalize() }

private fun String.sprakTilTekst() = when (this.lowercase()) {
    "nb" -> "Bokmål"
    "nn" -> "Nynorsk"
    else -> this
}

private fun SelvstendigNæringsdrivende.somMap(): Map<String, Any?> = mapOf(
    "næringsinntekt" to næringsinntekt,
    "næringstyper" to næringstyper.somMapNæringstyper(),
    "fiskerErPåBladB" to fiskerErPåBladB,
    "yrkesaktivSisteTreFerdigliknedeÅrene" to yrkesaktivSisteTreFerdigliknedeÅrene?.somMap(),
    "varigEndring" to varigEndring?.somMap(),
    "harFlereAktiveVirksomheter" to harFlereAktiveVirksomheter,
    "navnPåVirksomheten" to navnPåVirksomheten,
    "fraOgMed" to DATE_FORMATTER.format(fraOgMed),
    "tilOgMed" to if(tilOgMed != null) DATE_FORMATTER.format(tilOgMed) else null,
    "registrertINorge" to registrertINorge.boolean,
    "organisasjonsnummer" to organisasjonsnummer,
    "registrertIUtlandet" to registrertIUtlandet?.somMap(),
    "regnskapsfører" to regnskapsfører?.somMap()
)

private fun YrkesaktivSisteTreFerdigliknedeÅrene.somMap() : Map<String, Any?> = mapOf(
    "oppstartsdato" to DATE_FORMATTER.format(oppstartsdato)
)

private fun VarigEndring.somMap() : Map<String, Any?> = mapOf(
    "dato" to DATE_FORMATTER.format(dato),
    "inntektEtterEndring" to inntektEtterEndring,
    "forklaring" to forklaring
)

private fun Land.somMap() = mapOf<String, Any?>(
    "landnavn" to landnavn,
    "landkode" to landkode
)

private fun Regnskapsfører.somMap() = mapOf<String, Any?>(
    "navn" to navn,
    "telefon" to telefon
)

private fun List<Næringstyper>.somMapNæringstyper() = map {
    mapOf(
        "navn" to it.beskrivelse
    )
}