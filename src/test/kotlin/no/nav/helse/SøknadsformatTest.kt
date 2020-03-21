package no.nav.helse

import no.nav.helse.dokument.Søknadsformat
import no.nav.helse.prosessering.v1.*
import org.skyscreamer.jsonassert.JSONAssert
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.Test

class SøknadsformatTest {

    @Test
    fun `Soknaden journalfoeres som JSON uten vedlegg`() {
        val søknadId = UUID.randomUUID().toString()
        val mottatt = ZonedDateTime.of(2018, 1, 2, 3, 4, 5, 6, ZoneId.of("UTC"))
        val json = Søknadsformat.somJson(
            melding(
                søknadId = søknadId,
                mottatt = mottatt
            )
        )
        println(String(json))
        JSONAssert.assertEquals(
            """{
            "søknadId": "$søknadId",
            "språk": "nb",
            "mottatt": "2018-01-02T03:04:05.000000006Z",
            "søker": {
                "aktørId": "123456",
                "fødselsnummer": "02119970078",
                "fødselsdato": "1999-11-02",
                "etternavn": "Nordmann",
                "mellomnavn" : null,
                "fornavn" : "Ola"
            },
            "bosteder": [{
                "fraOgMed": "2020-01-01",
                "tilOgMed": "2020-01-06",
                "landkode": "SWE",
                "landnavn": "Sverige",
                "erEØSLand": true
            }, {
                "fraOgMed": "2020-01-11",
                "tilOgMed": "2020-01-11",
                "landkode": "NOR",
                "landnavn": "Norge",
                "erEØSLand": true
            }],
            "opphold": [{
                "fraOgMed": "2020-01-16",
                "tilOgMed": "2020-01-21",
                "landkode": "Eng",
                "landnavn": "England",
                "erEØSLand": true
            }, {
                "fraOgMed": "2019-12-22",
                "tilOgMed": "2019-12-27",
                "landkode": "CRO",
                "landnavn": "Kroatia",
                "erEØSLand": true
            }],
            "spørsmål": [{
                "spørsmål": "Har du vært hjemme?",
                "svar": false
            }, {
                "spørsmål": "Skal du være hjemme?",
                "svar": true
            }],
            "utbetalingsperioder": [{
                "fraOgMed": "2020-01-01",
                "tilOgMed": "2020-01-11",
                "lengde": "PT5H30M"
            }, {
                "fraOgMed": "2020-01-21",
                "tilOgMed": "2020-01-21",
                "lengde": "PT5H30M"
            }, {
                "fraOgMed": "2020-01-31",
                "tilOgMed": "2020-02-05",
                "lengde": "PT5H30M"
            }],
            "vedlegg": [
                "http://localhost:8080/vedlegg/1",
                "http://localhost:8080/vedlegg/2",
                "http://localhost:8080/vedlegg/3"
            ],
            "frilans": null,
            "selvstendigVirksomheter": null,
            "bekreftelser": {
            "harBekreftetOpplysninger": true,
            "harForståttRettigheterOgPlikter": true
        }
    }
        """.trimIndent(), String(json), true
        )
    }

    private fun melding(
        søknadId: String,
        fødselsnummerSoker: String? = "02119970078",
        start: LocalDate? = LocalDate.parse("2020-01-01"),
        mottatt: ZonedDateTime
    ): MeldingV1 = MeldingV1(
        søknadId = søknadId,
        språk = "nb",
        mottatt = mottatt,
        søker = Søker(
            aktørId = "123456",
            fødselsnummer = fødselsnummerSoker!!,
            fødselsdato = LocalDate.parse("1999-11-02"),
            etternavn = "Nordmann",
            mellomnavn = null,
            fornavn = "Ola"
        ),
        bosteder = listOf(
            Bosted(
                fraOgMed = start!!,
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
                svar = JaNei.Nei
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
        vedlegg = listOf(
            URI("http://localhost:8080/vedlegg/1"),
            URI("http://localhost:8080/vedlegg/2"),
            URI("http://localhost:8080/vedlegg/3")
        ),
        bekreftelser = Bekreftelser(
            harBekreftetOpplysninger = JaNei.Ja,
            harForståttRettigheterOgPlikter = JaNei.Ja
        )
    )
}
