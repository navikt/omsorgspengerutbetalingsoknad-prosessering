package no.nav.helse.prosessering.v1

import com.fasterxml.jackson.annotation.JsonFormat
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
    val vedlegg: List<URI>
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
    val lengde: Duration?
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

