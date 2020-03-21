package no.nav.helse.prosessering.v1

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonValue
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

data class MeldingV1(
    val søknadId: String,
    val mottatt: ZonedDateTime,
    val søker: Søker,
    val språk: String,
    val bosteder: List<Bosted>,
    val opphold: List<Opphold>,
    val spørsmål: List<SpørsmålOgSvar>,
    val utbetalingsperioder: List<Utbetalingsperiode>,
    val vedlegg: List<URI>,
    val frilans: Frilans? = null,
    val selvstendigVirksomheter: List<Virksomhet>? = null,
    val bekreftelser: Bekreftelser
)

data class Bekreftelser(
    val harBekreftetOpplysninger: JaNei,
    val harForståttRettigheterOgPlikter: JaNei
)

data class Frilans(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startdato: LocalDate,
    val jobberFortsattSomFrilans: Boolean
)

data class Virksomhet(
    val næringstyper: List<Næringstyper> = listOf(),
    val fiskerErPåBladB: JaNei?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tilOgMed: LocalDate? = null,
    val næringsinntekt: Int? = null,
    val navnPåVirksomheten: String,
    val organisasjonsnummer: String? = null,
    val registrertINorge: JaNei,
    val registrertILand: String? = null,
    val yrkesaktivSisteTreFerdigliknedeÅrene: YrkesaktivSisteTreFerdigliknedeÅrene? = null,
    val varigEndring: VarigEndring? = null,
    val regnskapsfører: Regnskapsfører? = null,
    val revisor: Revisor? = null
)

/**
 * Unngå `Boolean` default-verdi null -> false
 */
enum class JaNei (@get:JsonValue val boolean: Boolean) {
    Ja(true),
    Nei(false);

    companion object {
        @JsonCreator
        @JvmStatic
        fun fraBoolean(boolean: Boolean?) = when(boolean) {
            true -> Ja
            false -> Nei
            else -> throw IllegalStateException("Kan ikke være null")
        }
    }
}

data class YrkesaktivSisteTreFerdigliknedeÅrene(
    val oppstartsdato: LocalDate
)

enum class Næringstyper {
    FISKE,
    JORDBRUK_SKOGBRUK,
    DAGMAMMA,
    ANNEN
}

data class VarigEndring(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val dato: LocalDate,
    val inntektEtterEndring: Int,
    val forklaring: String
)

data class Revisor(
    val navn: String,
    val telefon: String,
    val erNærVennFamilie: JaNei,
    val kanInnhenteOpplysninger: JaNei
)

data class Regnskapsfører(
    val navn: String,
    val telefon: String,
    val erNærVennFamilie: JaNei
)

data class Søker(
    val fødselsnummer: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    @JsonFormat(pattern = "yyyy-MM-dd") val fødselsdato: LocalDate?,
    val aktørId: String
) {
    override fun toString(): String {
        return "Soker(fornavn='$fornavn', mellomnavn=$mellomnavn, etternavn='$etternavn', fødselsdato=$fødselsdato, aktørId='$aktørId')"
    }
}

data class Utbetalingsperiode(
    @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate,
    val lengde: Duration? = null
)

data class Bosted(
    @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate,
    val landkode: String,
    val landnavn: String
)

typealias Opphold = Bosted

data class SpørsmålOgSvar(
    val spørsmål: Spørsmål,
    val svar: JaNei
)

typealias Spørsmål = String


