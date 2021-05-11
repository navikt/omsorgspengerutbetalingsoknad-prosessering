package no.nav.helse.prosessering.v1

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.k9.søknad.Søknad
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

data class MeldingV1(
    val søknadId: String,
    val mottatt: ZonedDateTime,
    val søker: Søker,
    val språk: String,
    val harDekketTiFørsteDagerSelv: Boolean? = null, // TODO: 08/04/2021 Fjern nullable etter prodsetting.
    val bosteder: List<Bosted>,
    val opphold: List<Opphold>,
    val spørsmål: List<SpørsmålOgSvar>,
    val utbetalingsperioder: List<Utbetalingsperiode>,
    val andreUtbetalinger: List<String>?, //TODO: Fjern ? når dette er prodsatt.
    val fosterbarn: List<FosterBarn>? = listOf(),
    val vedlegg: List<URI>,
    val frilans: Frilans? = null,
    val selvstendigVirksomheter: List<Virksomhet> = listOf(),
    val erArbeidstakerOgså: Boolean,
    val hjemmePgaSmittevernhensyn: Boolean? = null, // TODO: 15/03/2021 utgår.
    val hjemmePgaStengtBhgSkole: Boolean? = null, // TODO: 15/03/2021 utgår
    val bekreftelser: Bekreftelser,
    val k9FormatSøknad: Søknad
)

data class Bekreftelser(
    val harBekreftetOpplysninger: JaNei,
    val harForståttRettigheterOgPlikter: JaNei
)

data class Frilans(
    @JsonFormat(pattern = "yyyy-MM-dd") val startdato: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val sluttdato: LocalDate? = null,
    val jobberFortsattSomFrilans: Boolean
)

data class Virksomhet(
    val næringstyper: List<Næringstyper> = listOf(),
    val fiskerErPåBladB: JaNei? = JaNei.Nei,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tilOgMed: LocalDate? = null,
    val næringsinntekt: Int? = null,
    val navnPåVirksomheten: String,
    val organisasjonsnummer: String? = null,
    val registrertINorge: JaNei,
    val registrertIUtlandet: Land? = null,
    val erNyoppstartet: Boolean,
    val yrkesaktivSisteTreFerdigliknedeÅrene: YrkesaktivSisteTreFerdigliknedeÅrene? = null,
    val varigEndring: VarigEndring? = null,
    val regnskapsfører: Regnskapsfører? = null
) {
    override fun toString(): String {
        return "Virksomhet()"
    }
}

/**
 * ISO 3166 alpha-3 landkode.
 *
 * @see https://en.wikipedia.org/wiki/ISO_3166-1_alpha-3
 */
data class Land(val landkode: String, val landnavn: String)

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

data class FosterBarn(
    val fødselsnummer: String
) {
    override fun toString(): String {
        return "FosterBarn()"
    }
}


data class YrkesaktivSisteTreFerdigliknedeÅrene(
    val oppstartsdato: LocalDate
)

enum class Næringstyper(val beskrivelse: String) {
    FISKE("Fiske"),
    JORDBRUK_SKOGBRUK("Jordbruk/skogbruk"),
    DAGMAMMA("Dagmamma eller familiebarnehage i eget hjem"),
    ANNEN("Annen");
}

data class VarigEndring(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val dato: LocalDate,
    val inntektEtterEndring: Int,
    val forklaring: String
)

data class Regnskapsfører(
    val navn: String,
    val telefon: String
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
        return "Soker()"
    }
}

data class Utbetalingsperiode(
    @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate,
    val lengde: Duration? = null, //TODO: beholde lengde i en periode slik at vi ikke mister info i overgangen
    val antallTimerBorte: Duration? = null,
    val antallTimerPlanlagt: Duration? = null,
    val årsak: FraværÅrsak? = null,
    val aktivitetFravær: List<AktivitetFravær> = listOf()
)

enum class FraværÅrsak {
    STENGT_SKOLE_ELLER_BARNEHAGE,
    SMITTEVERNHENSYN,
    ORDINÆRT_FRAVÆR
}

enum class AktivitetFravær {
    FRILANSER,
    SELVSTENDIG_VIRKSOMHET
}

data class Bosted(
    @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate,
    val landkode: String,
    val landnavn: String,
    val erEØSLand: JaNei
)

typealias Opphold = Bosted

data class SpørsmålOgSvar(
    val spørsmål: Spørsmål,
    val svar: JaNei
)

typealias Spørsmål = String
