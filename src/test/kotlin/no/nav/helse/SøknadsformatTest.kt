package no.nav.helse

import no.nav.helse.dokument.Søknadsformat
import no.nav.k9.søknad.JsonUtils
import org.skyscreamer.jsonassert.JSONAssert
import java.util.*
import kotlin.test.Test

class SøknadsformatTest {

    @Test
    fun `Soknaden journalfoeres som JSON uten vedlegg`() {
        val søknadId = UUID.randomUUID()
        val json = Søknadsformat.somJson(SøknadUtils.defaultK9FormatOmsorgspengerutbetaling(søknadId))

        JsonUtils.toString(SøknadUtils.defaultK9FormatOmsorgspengerutbetaling(søknadId))
        JSONAssert.assertEquals(
            //language=json
            """
                {
                  "søknadId": "$søknadId",
                  "språk": "nb",
                  "versjon": "1.0",
                  "mottattDato": "2020-01-01T10:00:00.000Z",
                  "søker": {
                    "norskIdentitetsnummer": "12345678910"
                  },
                  "ytelse": {
                    "type": "OMP_UT",
                    "fosterbarn": [
                      {
                        "norskIdentitetsnummer": "10987654321",
                        "fødselsdato": null
                      }
                    ],
                    "aktivitet": {
                      "selvstendigNæringsdrivende": [
                        {
                          "perioder": {
                            "2018-01-01/2020-01-01": {
                              "virksomhetstyper": [
                                "DAGMAMMA",
                                "ANNEN"
                              ],
                              "regnskapsførerNavn": "Regnskapsfører Svensen",
                              "regnskapsførerTlf": "+4799887766",
                              "erVarigEndring": true,
                              "endringDato": "2020-01-01",
                              "endringBegrunnelse": "Grunnet Covid-19",
                              "bruttoInntekt": 5000000,
                              "erNyoppstartet": true,
                              "registrertIUtlandet": false,
                              "landkode": "NOR"
                            }
                          },
                          "organisasjonsnummer": "12345678910112233444455667",
                          "virksomhetNavn": "Mamsen Bamsen AS"
                        },
                        {
                          "perioder": {
                            "2015-01-01/2017-01-01": {
                              "virksomhetstyper": [
                                "FISKE"
                              ],
                              "erVarigEndring": false,
                              "bruttoInntekt": 500000,
                              "erNyoppstartet": false,
                              "registrertIUtlandet": true,
                              "landkode": "ESP"
                            }
                          },
                          "organisasjonsnummer": "54549049090490498048940940",
                          "virksomhetNavn": "Something Fishy AS"
                        }
                      ],
                      "frilanser": {
                        "startdato": "2020-01-01",
                        "jobberFortsattSomFrilans": true
                      }
                    },
                    "fraværsperioder": [
                      {
                        "periode": "2020-01-01/2020-01-05",
                        "duration": "PT7H"
                      },
                      {
                        "periode": "2020-01-06/2020-01-10",
                        "duration": "PT4H"
                      }
                    ],
                    "bosteder": null,
                    "utenlandsopphold": {
                      "perioder": {
                        "2020-01-01/2020-01-05": {
                          "land": "CAN",
                          "årsak": "barnetInnlagtIHelseinstitusjonDekketEtterAvtaleMedEtAnnetLandOmTrygd"
                        },
                        "2020-01-06/2020-01-10": {
                          "land": "SWE",
                          "årsak": "barnetInnlagtIHelseinstitusjonForNorskOffentligRegning"
                        }
                      }
                    }
                  }
                }
        """.trimIndent(), String(json), true
        )
    }
}
