package no.nav.helse.prosessering.v1

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
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
    val selvstendigVirksomheter: List<Virksomhet>? = null
)

data class Frilans(
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startdato: LocalDate,
    val jobberFortsattSomFrilans: Boolean
)

data class Virksomhet(
    val naringstype: List<Naringstype>,
    @JsonProperty("fisker_er_pa_blad_b")
    val fiskerErPåBladB: Boolean? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate? = null,
    val erPagaende: Boolean,
    val naringsinntekt: Int? = null,
    val navnPaVirksomheten: String,
    val organisasjonsnummer: String? = null,
    @JsonProperty("registrert_i_norge")
    val registrertINorge: Boolean,
    @JsonProperty("registrert_i_land")
    val registrertILand: String? = null,
    val harBlittYrkesaktivSisteTreFerdigliknendeArene: Boolean? = null,
    val yrkesaktivSisteTreFerdigliknedeArene: YrkesaktivSisteTreFerdigliknedeArene? = null,
    @JsonProperty("har_varig_endring_av_inntekt_siste_4_kalenderar")
    val harVarigEndringAvInntektSiste4Kalenderar: Boolean? = null,
    val varigEndring: VarigEndring? = null,
    val harRegnskapsforer: Boolean,
    val regnskapsforer: Regnskapsforer? = null,
    val harRevisor: Boolean? = null,
    val revisor: Revisor? = null
)

data class Revisor(
    val navn: String,
    val telefon: String,
    val kanInnhenteOpplysninger: Boolean?
)

data class Regnskapsforer(
    val navn: String,
    val telefon: String
)

data class VarigEndring(
    val dato: LocalDate? = null,
    val inntektEtterEndring: Int? = null,
    val forklaring: String? = null
)

enum class Naringstype(val detaljert: String) {
    @JsonProperty("FISKE") FISKER("FISKE"),
    @JsonProperty("JORDBRUK_SKOGBRUK") JORDBRUK("JORDBRUK_SKOGBRUK"),
    @JsonProperty("ANNEN") ANNET("ANNEN"),
    DAGMAMMA("DAGMAMMA")
}

data class YrkesaktivSisteTreFerdigliknedeArene(
    val oppstartsdato: LocalDate?
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
    val svar: Svar,
    val fritekst: Fritekst? = null
)

typealias Spørsmål = String
typealias Fritekst = String

enum class Svar {
    Ja,
    Nei,
    VetIkke
}

