package no.nav.helse

import no.nav.helse.prosessering.v1.*
import java.io.File
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.test.Test

class PdfV1GeneratorTest {

    private companion object {
        private val generator = PdfV1Generator()
        private val fødselsdato = LocalDate.now()

        private val gyldigFodselsnummerA = "02119970078"
        private val gyldigFodselsnummerB = "19066672169"
        private val gyldigFodselsnummerC = "20037473937"
    }

    private fun fullGyldigMelding(
        start: LocalDate = LocalDate.parse("2020-01-01"),
        søknadId: String,
        fødselsnummerSoker: String = gyldigFodselsnummerA
    ): MeldingV1 {
        return MeldingV1(
            søknadId = søknadId,
            språk = "nb",
            mottatt = ZonedDateTime.now(),
            søker = Søker(
                aktørId = "123456",
                fødselsnummer = fødselsnummerSoker,
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
                    landnavn = "Iran",
                    landkode = "IRN",
                    erEØSLand = JaNei.Nei
                )
            ),
            spørsmål = listOf(
                SpørsmålOgSvar(
                    spørsmål = "Har du vært hjemme?",
                    svar = JaNei.Ja
                ),
                SpørsmålOgSvar(
                    spørsmål = "Skal du være hjemme?",
                    svar = JaNei.Ja
                )
            ),
            utbetalingsperioder = listOf(
                Utbetalingsperiode(
                    fraOgMed = start,
                    tilOgMed = start.plusDays(10),
                    lengde = Duration.ofHours(5).plusMinutes(0)
                ),
                Utbetalingsperiode(
                    fraOgMed = start.plusDays(20),
                    tilOgMed = start.plusDays(20)
                ),
                Utbetalingsperiode(
                    fraOgMed = start.plusDays(30),
                    tilOgMed = start.plusDays(35),
                    lengde = Duration.ofHours(5).plusMinutes(30)
                )
            ),
            vedlegg = listOf(
                URI("http://localhost:8080/vedlegg/1"),
                URI("http://localhost:8080/vedlegg/2"),
                URI("http://localhost:8080/vedlegg/3")
            ),
            frilans = Frilans(
                startdato = LocalDate.now().minusYears(3),
                jobberFortsattSomFrilans = true
            ),
            selvstendigVirksomheter = listOf(
                Virksomhet(
                    næringstyper = listOf(Næringstyper.ANNEN),
                    fraOgMed = LocalDate.now(),
                    tilOgMed = LocalDate.now().plusDays(10),
                    navnPåVirksomheten = "Kjells Møbelsnekkeri",
                    registrertINorge = JaNei.Ja,
                    organisasjonsnummer = "111111",
                    fiskerErPåBladB = JaNei.Nei,
                    revisor = Revisor(
                        navn = "Kjell Revisor",
                        telefon = "9999",
                        kanInnhenteOpplysninger = JaNei.Ja
                    )
                ),
                Virksomhet(
                    næringstyper = listOf(Næringstyper.JORDBRUK_SKOGBRUK, Næringstyper.DAGMAMMA, Næringstyper.FISKE),
                    fiskerErPåBladB = JaNei.Ja,
                    fraOgMed = LocalDate.now(),
                    næringsinntekt = 1111,
                    navnPåVirksomheten = "Tull Og Tøys",
                    registrertINorge = JaNei.Nei,
                    registrertILand = "Bahamas",
                    yrkesaktivSisteTreFerdigliknedeÅrene = YrkesaktivSisteTreFerdigliknedeÅrene(LocalDate.now()),
                    varigEndring = VarigEndring(
                        dato = LocalDate.now().minusDays(20),
                        inntektEtterEndring = 234543,
                        forklaring = "Forklaring som handler om varig endring"
                    ),
                    regnskapsfører = Regnskapsfører(
                        navn = "Bjarne Regnskap",
                        telefon = "65484578"
                    )
                )
            ),
            fosterbarn = listOf(
                FosterBarn(
                    fødselsnummer = gyldigFodselsnummerB,
                    fornavn = "Jarle",
                    etternavn = "Nordmann"
                ),
                FosterBarn(
                    fødselsnummer = gyldigFodselsnummerC,
                    fornavn = "Sara",
                    etternavn = "Nordmann"
                )
            ),
            bekreftelser = Bekreftelser(
                harBekreftetOpplysninger = JaNei.Ja,
                harForståttRettigheterOgPlikter = JaNei.Ja
            )
        )
    }

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full-søknad"
        var pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(søknadId = id)
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

    }

    private fun pdfPath(soknadId: String) = "${System.getProperty("user.dir")}/generated-pdf-$soknadId.pdf"

    @Test
    fun `generering av oppsummerings-PDF fungerer`() {
        genererOppsummeringsPdfer(false)
    }

    @Test
    //@Ignore
    fun `opprett lesbar oppsummerings-PDF`() {
        genererOppsummeringsPdfer(true)
    }
}
