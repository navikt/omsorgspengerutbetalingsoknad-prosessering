package no.nav.helse

import no.nav.helse.prosessering.v1.*
import java.io.File
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.Ignore
import kotlin.test.Test

class PdfV1GeneratorTest {

    private companion object {
        private val generator = PdfV1Generator()
        private val fødselsdato = LocalDate.now()
    }

    private fun fullGyldigMelding(
        start: LocalDate = LocalDate.parse("2020-01-01"),
        søknadId: String,
        fødselsnummerSoker: String = "02119970078"
    ): MeldingV1 {
        return MeldingV1(
            søknadId = søknadId,
            språk = Språk.BOKMÅL,
            mottatt = ZonedDateTime.now(),
            søker = Søker(
                aktørId = "123456",
                fødselsnummer = fødselsnummerSoker,
                fødselsdato = LocalDate.now().minusDays(1000),
                etternavn = "Nordmann",
                mellomnavn = "Mellomnavn",
                fornavn = "Ola",
                myndig = true
            ),
            bosteder = listOf(
                Bosted(
                    fraOgMed = start,
                    tilOgMed = start.plusDays(5),
                    landnavn = "Sverige",
                    landkode = "SWE"
                ),
                Bosted(
                    fraOgMed = start.plusDays(10),
                    tilOgMed = start.plusDays(10),
                    landnavn = "Norge",
                    landkode = "NOR"
                )
            ),
            opphold = listOf(
                Bosted(
                    fraOgMed = start.plusDays(15),
                    tilOgMed = start.plusDays(20),
                    landnavn = "England",
                    landkode = "Eng"
                ),
                Bosted(
                    fraOgMed = start.minusDays(10),
                    tilOgMed = start.minusDays(5),
                    landnavn = "Kroatia",
                    landkode = "CRO"
                )
            ),
            spørsmål = listOf(
                SpørsmålOgSvar(
                    id = SpørsmålId.HarForståttRettigheterOgPlikter,
                    spørsmål = "HarForståttRettigheterOgPlikter?",
                    svar = Svar.Ja
                ),
                SpørsmålOgSvar(
                    id = SpørsmålId.HarBekreftetOpplysninger,
                    spørsmål = "HarBekreftetOpplysninger?",
                    svar = Svar.Ja
                ),
                SpørsmålOgSvar(
                    spørsmål = "Har du vært hjemme?",
                    svar = Svar.Nei
                ),
                SpørsmålOgSvar(
                    spørsmål = "Skal du være hjemme?",
                    svar = Svar.VetIkke,
                    fritekst = "Umulig å si"
                )
            ),
            utbetalingsperioder = listOf(
                UtbetalingsperiodeUtenVedlegg(
                    fraOgMed = start,
                    tilOgMed = start.plusDays(10),
                    lengde = Duration.ofHours(5).plusMinutes(30)
                ),
                UtbetalingsperiodeUtenVedlegg(
                    fraOgMed = start.plusDays(20),
                    tilOgMed = start.plusDays(20),
                    lengde = Duration.ofHours(5).plusMinutes(30)
                ),
                UtbetalingsperiodeUtenVedlegg(
                    fraOgMed = start.plusDays(30),
                    tilOgMed = start.plusDays(35),
                    lengde = Duration.ofHours(5).plusMinutes(30)
                )
            ),
            vedlegg = listOf(
                URI("http://localhost:8080/vedlegg/1"),
                URI("http://localhost:8080/vedlegg/2"),
                URI("http://localhost:8080/vedlegg/3")
            )
        )
    }

    private fun genererOppsummeringsPdfer(writeBytes: Boolean) {
        var id = "1-full-søknad"
        var pdf = generator.generateSoknadOppsummeringPdf(
            melding = fullGyldigMelding(søknadId = id)
        )
        if (writeBytes) File(pdfPath(soknadId = id)).writeBytes(pdf)

        id = "2-full-søknad-barnets-fødsesldato"
        pdf = generator.generateSoknadOppsummeringPdf(
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
