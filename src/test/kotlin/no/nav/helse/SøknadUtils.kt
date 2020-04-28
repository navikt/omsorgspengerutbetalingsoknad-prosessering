package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.prosessering.v1.*
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

internal object SøknadUtils {
    internal val objectMapper = jacksonObjectMapper().omsorgspengerKonfiguert()
    private val start = LocalDate.parse("2020-01-01")
    private const val GYLDIG_ORGNR = "917755736"

    internal val defaultSøknad = MeldingV1(
        søknadId = UUID.randomUUID().toString(),
        språk = "nb",
        mottatt = ZonedDateTime.now(),
        søker = Søker(
            aktørId = "123456",
            fødselsnummer = "02119970078",
            fødselsdato = LocalDate.now().minusDays(1000),
            etternavn = "Nordmann",
            mellomnavn = "Mellomnavn",
            fornavn = "Ola"
        ),
        bosteder = listOf(
            Bosted(
                fraOgMed = start,
                tilOgMed = start.plusDays(5),
                landnavn = "Sverige",
                landkode = "SWE",
                erEØSLand = JaNei.Ja
            ),
            Bosted(
                fraOgMed = start.plusDays(10),
                tilOgMed = start.plusDays(10),
                landnavn = "Norge",
                landkode = "NOR",
                erEØSLand = JaNei.Ja
            )
        ),
        opphold = listOf(
            Bosted(
                fraOgMed = start.plusDays(15),
                tilOgMed = start.plusDays(20),
                landnavn = "England",
                landkode = "Eng",
                erEØSLand = JaNei.Ja
            ),
            Bosted(
                fraOgMed = start.minusDays(10),
                tilOgMed = start.minusDays(5),
                landnavn = "Kroatia",
                landkode = "CRO",
                erEØSLand = JaNei.Ja
            )
        ),
        spørsmål = listOf(
            SpørsmålOgSvar(
                spørsmål = "Har du vært hjemme?",
                svar = JaNei.Ja
            ),
            SpørsmålOgSvar(
                spørsmål = "Skal du være hjemme?",
                svar = JaNei.Nei
            )
        ),
        utbetalingsperioder = listOf(
            Utbetalingsperiode(
                fraOgMed = start,
                tilOgMed = start.plusDays(10),
                lengde = Duration.ofHours(5).plusMinutes(30)
            ),
            Utbetalingsperiode(
                fraOgMed = start.plusDays(20),
                tilOgMed = start.plusDays(20),
                lengde = Duration.ofHours(5).plusMinutes(30)
            ),
            Utbetalingsperiode(
                fraOgMed = start.plusDays(30),
                tilOgMed = start.plusDays(35),
                lengde = Duration.ofHours(5).plusMinutes(30)
            )
        ),
        andreUtbetalinger = listOf("dagpenger", "sykepenger"),
        fosterbarn = listOf(
            FosterBarn(
                fødselsnummer = "02119970078"
            )
        ),
        vedlegg = listOf(
            URI("http://localhost:8080/vedlegg/1"),
            URI("http://localhost:8080/vedlegg/2"),
            URI("http://localhost:8080/vedlegg/3")
        ),
        bekreftelser = Bekreftelser(
            harBekreftetOpplysninger = JaNei.Ja,
            harForståttRettigheterOgPlikter = JaNei.Ja
        ),
        selvstendigVirksomheter = listOf(
            Virksomhet(
                næringstyper = listOf(Næringstyper.ANNEN, Næringstyper.FISKE),
                fraOgMed = LocalDate.now(),
                tilOgMed = LocalDate.now().plusDays(10),
                navnPåVirksomheten = "Kjells Møbelsnekkeri",
                registrertINorge = JaNei.Ja,
                organisasjonsnummer = "111111",
                varigEndring = VarigEndring(
                    dato = LocalDate.now().minusDays(20),
                    inntektEtterEndring = 234543,
                    forklaring = "Forklaring som handler om varig endring"
                ),
                revisor = Revisor(
                    navn = "Kjell Revisor",
                    telefon = "9999",
                    kanInnhenteOpplysninger = JaNei.Ja
                )
            )
        ),
        erArbeidstakerOgså = true,
        hjemmePgaSmittevernhensyn = true
    )
}

internal fun MeldingV1.somJson() = SøknadUtils.objectMapper.writeValueAsString(this)
