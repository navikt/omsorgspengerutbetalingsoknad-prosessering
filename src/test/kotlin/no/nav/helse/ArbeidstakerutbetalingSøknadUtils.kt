package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.aktoer.AktørId
import no.nav.helse.prosessering.v1.*
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.ArbeidsgiverDetaljer
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.ArbeidstakerutbetalingMelding
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.OrganisasjonDetaljer
import no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling.PreprosessertArbeidstakerutbetalingMelding
import java.net.URI
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

internal object ArbeidstakerutbetalingSøknadUtils {
    internal val objectMapper = jacksonObjectMapper().omsorgspengerKonfiguert()
    private val start = LocalDate.parse("2020-01-01")
    private const val GYLDIG_ORGNR = "917755736"

    internal val defaultSøknad = ArbeidstakerutbetalingMelding(
        søknadId = UUID.randomUUID().toString(),
        språk = "nb",
        mottatt = ZonedDateTime.now(),
        søker = Søker(
            aktørId = "123456",
            fødselsnummer = "02119970078",
            fødselsdato = LocalDate.parse("1999-11-02"),
            etternavn = "Nordmann",
            mellomnavn = null,
            fornavn = "Ola"
        ),
        arbeidsgivere = ArbeidsgiverDetaljer(
            organisasjoner = listOf(
                OrganisasjonDetaljer(
                    navn = "Arbeidsgiver 1",
                    organisasjonsnummer = GYLDIG_ORGNR,
                    harHattFraværHosArbeidsgiver = true,
                    arbeidsgiverHarUtbetaltLønn = false
                ),
                OrganisasjonDetaljer(
                    navn = "Arbeidsgiver 2",
                    organisasjonsnummer = GYLDIG_ORGNR,
                    harHattFraværHosArbeidsgiver = true,
                    arbeidsgiverHarUtbetaltLønn = false
                ),
                OrganisasjonDetaljer(
                    navn = "Arbeidsgiver 3",
                    organisasjonsnummer = GYLDIG_ORGNR,
                    harHattFraværHosArbeidsgiver = true,
                    arbeidsgiverHarUtbetaltLønn = false
                ),
                OrganisasjonDetaljer(
                    organisasjonsnummer = GYLDIG_ORGNR,
                    harHattFraværHosArbeidsgiver = true,
                    arbeidsgiverHarUtbetaltLønn = false
                )
            )
        ),
        bosteder = listOf(
            Bosted(
                fraOgMed = start.minusDays(20),
                tilOgMed = start.minusDays(10),
                landkode = "GB",
                landnavn = "Great Britain",
                erEØSLand = JaNei.Ja
            )
        ),
        opphold = listOf(
            Opphold(
                fraOgMed = start.minusDays(20),
                tilOgMed = start.minusDays(10),
                landkode = "GB",
                landnavn = "Great Britain",
                erEØSLand = JaNei.Ja
            )
        ),
        spørsmål = listOf(
            SpørsmålOgSvar(
                spørsmål = "Et spørsmål",
                svar = JaNei.Nei
            )
        ),
        bekreftelser = Bekreftelser(
            harForståttRettigheterOgPlikter = JaNei.Ja,
            harBekreftetOpplysninger = JaNei.Ja
        ),
        utbetalingsperioder = listOf(
            Utbetalingsperiode(
                fraOgMed = start,
                tilOgMed = start.plusDays(5)
            )
        ),
        fosterbarn = listOf(
            FosterBarn(
                fødselsnummer = "02119970078",
                fornavn = "fornavn",
                etternavn = "etternavn"
            )
        )
    )

    internal val defaultKomplettSøknad = PreprosessertArbeidstakerutbetalingMelding(
        søkerAktørId = AktørId("123456"),
        melding = defaultSøknad,
        dokumentUrls = listOf(
            listOf(URI("http://localhost:8080/vedlegg/1"), URI("http://localhost:8080/vedlegg/2")),
            listOf(URI("http://localhost:8080/vedlegg/3"), URI("http://localhost:8080/vedlegg/4"))
        )
    )
}

internal fun ArbeidstakerutbetalingMelding.somJson() = ArbeidstakerutbetalingSøknadUtils.objectMapper.writeValueAsString(this)
internal fun PreprosessertArbeidstakerutbetalingMelding.somJson() = ArbeidstakerutbetalingSøknadUtils.objectMapper.writeValueAsString(this)
