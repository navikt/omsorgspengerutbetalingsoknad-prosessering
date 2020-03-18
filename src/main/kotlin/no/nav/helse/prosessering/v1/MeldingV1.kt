package no.nav.helse.prosessering.v1

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
    val språk: Språk,
    val bosteder: List<Bosted>,
    val opphold: List<Opphold>,
    val spørsmål: List<SpørsmålOgSvar>,
    val utbetalingsperioder: List<UtbetalingsperiodeUtenVedlegg>,
    val vedlegg: List<URI>
)

data class Søker(
    val fødselsnummer: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    @JsonFormat(pattern = "yyyy-MM-dd") val fødselsdato: LocalDate?,
    val aktørId: String,
    val myndig: Boolean
) {
    override fun toString(): String {
        return "Soker(fornavn='$fornavn', mellomnavn=$mellomnavn, etternavn='$etternavn', fødselsdato=$fødselsdato, aktørId='$aktørId')"
    }
}

data class UtbetalingsperiodeUtenVedlegg(
    @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate,
    val lengde: Duration?
)

enum class Språk(@JsonValue val språk: String) {
    BOKMÅL("nb"),
    NYNORSK("nn");
}

data class Bosted(
    @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate,
    val landkode: String,
    val landnavn: String
)

typealias Opphold = Bosted

data class Medlemskap(
    val harBoddIUtlandetSiste12Mnd: Boolean,
    val utenlandsoppholdSiste12Mnd: List<Utenlandsopphold> = listOf(),
    val skalBoIUtlandetNeste12Mnd: Boolean,
    val utenlandsoppholdNeste12Mnd: List<Utenlandsopphold> = listOf()
)

data class Utenlandsopphold(
    @JsonFormat(pattern = "yyyy-MM-dd") val fraOgMed: LocalDate,
    @JsonFormat(pattern = "yyyy-MM-dd") val tilOgMed: LocalDate,
    val landkode: String,
    val landnavn: String
) {
    override fun toString(): String {
        return "Utenlandsopphold(fraOgMed=$fraOgMed, tilOgMed=$tilOgMed, landkode='$landkode', landnavn='$landnavn')"
    }
}

data class SpørsmålOgSvar(
    val id: SpørsmålId? = null,
    val spørsmål: Spørsmål,
    val svar: Svar,
    val fritekst: Fritekst? = null
)

/**
 * Trenger bare lage ID'er på ting vi eksplisitt
 * må gjøre noen sjekker på eller vi trenger å ha
 * Et eksplisitt forhold til senere i prosesseringen.
 * Default bør være at det ikke er noen ID.
 */
enum class SpørsmålId {
    HarBekreftetOpplysninger,
    HarForståttRettigheterOgPlikter
}

typealias Spørsmål = String
typealias Fritekst = String

enum class Svar {
    Ja,
    Nei,
    VetIkke
}

