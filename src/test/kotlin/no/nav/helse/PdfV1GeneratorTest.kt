package no.nav.helse

import no.nav.helse.prosessering.v1.*
import java.io.File
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
                etternavn = "NORDMANN",
                mellomnavn = "MELLOMNAVN",
                fornavn = "OLA"
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
                    årsak = FraværÅrsak.STENGT_SKOLE_ELLER_BARNEHAGE
                ),
                Utbetalingsperiode(
                    fraOgMed = start.plusDays(20),
                    tilOgMed = start.plusDays(20),
                    årsak = FraværÅrsak.SMITTEVERNHENSYN
                ),
                Utbetalingsperiode(
                    fraOgMed = start.plusDays(30),
                    tilOgMed = start.plusDays(35),
                    årsak = FraværÅrsak.ORDINÆRT_FRAVÆR
                ),
                Utbetalingsperiode(
                    fraOgMed = start.plusDays(1),
                    tilOgMed = start.plusDays(1),
                    antallTimerPlanlagt = Duration.ofHours(24).plusMinutes(10),
                    antallTimerBorte = Duration.ofHours(18).plusMinutes(0),
                    årsak = FraværÅrsak.STENGT_SKOLE_ELLER_BARNEHAGE
                ),
                Utbetalingsperiode(
                    fraOgMed = start.plusDays(2),
                    tilOgMed = start.plusDays(2),
                    antallTimerPlanlagt = Duration.ofHours(5).plusMinutes(30),
                    antallTimerBorte = Duration.ofHours(5).plusMinutes(0),
                    årsak = FraværÅrsak.SMITTEVERNHENSYN
                ),
                Utbetalingsperiode(
                    fraOgMed = start.plusDays(3),
                    tilOgMed = start.plusDays(3),
                    lengde = Duration.ofHours(2),
                    antallTimerPlanlagt = Duration.ofHours(4).plusMinutes(30),
                    antallTimerBorte = Duration.ofHours(5).plusMinutes(0),
                    årsak = FraværÅrsak.ORDINÆRT_FRAVÆR
                ),
                Utbetalingsperiode(
                    fraOgMed = start.plusDays(3),
                    tilOgMed = start.plusDays(3),
                    lengde = Duration.ofHours(24),
                    årsak = FraværÅrsak.STENGT_SKOLE_ELLER_BARNEHAGE
                )
            ),
            andreUtbetalinger = listOf("dagpenger", "sykepenger", "midlertidigkompensasjonsnfri"),
            vedlegg = listOf(
            ),
            frilans = Frilans(
                startdato = LocalDate.now().minusYears(3),
                sluttdato = LocalDate.now().minusYears(1),
                jobberFortsattSomFrilans = false
            ),
            selvstendigVirksomheter = listOf(
                Virksomhet(
                    næringstyper = listOf(Næringstyper.ANNEN, Næringstyper.FISKE),
                    fraOgMed = LocalDate.now(),
                    erNyoppstartet = true,
                    tilOgMed = LocalDate.now().plusDays(10),
                    navnPåVirksomheten = "Kjells Møbelsnekkeri",
                    registrertINorge = JaNei.Ja,
                    organisasjonsnummer = "111111",
                    næringsinntekt = 123456789,
                    fiskerErPåBladB = JaNei.Ja,
                    varigEndring = VarigEndring(
                        dato = LocalDate.now().minusDays(20),
                        inntektEtterEndring = 234543,
                        forklaring = "Forklaring som handler om varig endring"
                    )
                ),
                Virksomhet(
                    næringstyper = listOf(Næringstyper.JORDBRUK_SKOGBRUK, Næringstyper.DAGMAMMA, Næringstyper.FISKE),
                    fiskerErPåBladB = JaNei.Ja,
                    fraOgMed = LocalDate.now(),
                    erNyoppstartet = true,
                    næringsinntekt = 1111,
                    navnPåVirksomheten = "Tull Og Tøys",
                    registrertINorge = JaNei.Nei,
                    registrertIUtlandet = Land(
                        landkode = "DEU",
                        landnavn = "Tyskland"
                    ),
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
            barn = listOf(
                Barn(
                    identitetsnummer = "02119970078",
                    aktørId = "123456",
                    navn = "Barn Barnesen",
                    aleneOmOmsorgen = true
                )
            ),
            fosterbarn = listOf(
                FosterBarn(
                    fødselsnummer = gyldigFodselsnummerB
                ),
                FosterBarn(
                    fødselsnummer = gyldigFodselsnummerC
                )
            ),
            bekreftelser = Bekreftelser(
                harBekreftetOpplysninger = JaNei.Ja,
                harForståttRettigheterOgPlikter = JaNei.Ja
            ),
            erArbeidstakerOgså = true,
            k9FormatSøknad = SøknadUtils.defaultK9FormatOmsorgspengerutbetaling(),
            hjemmePgaSmittevernhensyn = null,
            hjemmePgaStengtBhgSkole = null
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
