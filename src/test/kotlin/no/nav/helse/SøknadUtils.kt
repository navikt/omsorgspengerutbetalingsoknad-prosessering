package no.nav.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.prosessering.v1.AktivitetFravær.*
import no.nav.helse.prosessering.v1.Bekreftelser
import no.nav.helse.prosessering.v1.Bosted
import no.nav.helse.prosessering.v1.FosterBarn
import no.nav.helse.prosessering.v1.JaNei
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.helse.prosessering.v1.Næringstyper
import no.nav.helse.prosessering.v1.SpørsmålOgSvar
import no.nav.helse.prosessering.v1.Søker
import no.nav.helse.prosessering.v1.Utbetalingsperiode
import no.nav.helse.prosessering.v1.VarigEndring
import no.nav.helse.prosessering.v1.Virksomhet
import no.nav.k9.søknad.Søknad
import no.nav.k9.søknad.felles.Versjon
import no.nav.k9.søknad.felles.fravær.AktivitetFravær
import no.nav.k9.søknad.felles.fravær.FraværPeriode
import no.nav.k9.søknad.felles.fravær.FraværÅrsak
import no.nav.k9.søknad.felles.opptjening.Frilanser
import no.nav.k9.søknad.felles.opptjening.OpptjeningAktivitet
import no.nav.k9.søknad.felles.opptjening.SelvstendigNæringsdrivende
import no.nav.k9.søknad.felles.personopplysninger.Bosteder
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD
import no.nav.k9.søknad.felles.personopplysninger.Utenlandsopphold.UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING
import no.nav.k9.søknad.felles.type.Landkode
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer
import no.nav.k9.søknad.felles.type.Organisasjonsnummer
import no.nav.k9.søknad.felles.type.Periode
import no.nav.k9.søknad.felles.type.SøknadId
import no.nav.k9.søknad.felles.type.VirksomhetType
import no.nav.k9.søknad.ytelse.omsorgspenger.v1.OmsorgspengerUtbetaling
import java.math.BigDecimal
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*
import no.nav.k9.søknad.felles.personopplysninger.Barn as K9Barn

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
        harDekketTiFørsteDagerSelv = true,
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
                aktivitetFravær = listOf(FRILANSER),
                årsak = no.nav.helse.prosessering.v1.FraværÅrsak.ORDINÆRT_FRAVÆR
            ),
            Utbetalingsperiode(
                fraOgMed = start.plusDays(20),
                tilOgMed = start.plusDays(20),
                aktivitetFravær = listOf(SELVSTENDIG_VIRKSOMHET),
                årsak = no.nav.helse.prosessering.v1.FraværÅrsak.SMITTEVERNHENSYN
            ),
            Utbetalingsperiode(
                fraOgMed = start.plusDays(30),
                tilOgMed = start.plusDays(35),
                aktivitetFravær = listOf(FRILANSER, SELVSTENDIG_VIRKSOMHET),
                årsak = no.nav.helse.prosessering.v1.FraværÅrsak.STENGT_SKOLE_ELLER_BARNEHAGE
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
                erNyoppstartet = true,
                tilOgMed = LocalDate.now().plusDays(10),
                navnPåVirksomheten = "Kjells Møbelsnekkeri",
                registrertINorge = JaNei.Ja,
                næringsinntekt = 123456789,
                organisasjonsnummer = "111111",
                varigEndring = VarigEndring(
                    dato = LocalDate.now().minusDays(20),
                    inntektEtterEndring = 234543,
                    forklaring = "Forklaring som handler om varig endring"
                ),
                harFlereAktiveVirksomheter = true
            )
        ),
        erArbeidstakerOgså = true,
        k9FormatSøknad = defaultK9FormatOmsorgspengerutbetaling()
    )

    fun defaultK9FormatOmsorgspengerutbetaling(søknadId: UUID = UUID.randomUUID()) = Søknad(
        SøknadId.of(søknadId.toString()),
        Versjon.of("1.0"),
        ZonedDateTime.parse("2020-01-01T10:00:00Z"),
        no.nav.k9.søknad.felles.personopplysninger.Søker(NorskIdentitetsnummer.of("12345678910")),
        OmsorgspengerUtbetaling(
            listOf(
                K9Barn(NorskIdentitetsnummer.of("10987654321"), null)
            ),
            OpptjeningAktivitet(
                null,
                listOf(
                    SelvstendigNæringsdrivende(
                        mapOf(
                            Periode(
                                LocalDate.parse("2018-01-01"),
                                LocalDate.parse("2020-01-01")
                            ) to SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo.builder()
                                .erNyoppstartet(true)
                                .registrertIUtlandet(false)
                                .bruttoInntekt(BigDecimal(5_000_000))
                                .erVarigEndring(true)
                                .endringDato(LocalDate.parse("2020-01-01"))
                                .endringBegrunnelse("Grunnet Covid-19")
                                .landkode(Landkode.NORGE)
                                .regnskapsførerNavn("Regnskapsfører Svensen")
                                .regnskapsførerTelefon("+4799887766")
                                .virksomhetstyper(listOf(VirksomhetType.DAGMAMMA, VirksomhetType.ANNEN))
                                .build()
                        ),
                        Organisasjonsnummer.of("12345678910112233444455667"),
                        "Mamsen Bamsen AS"
                    ),
                    SelvstendigNæringsdrivende(
                        mapOf(
                            Periode(
                                LocalDate.parse("2015-01-01"),
                                LocalDate.parse("2017-01-01")
                            ) to SelvstendigNæringsdrivende.SelvstendigNæringsdrivendePeriodeInfo.builder()
                                .erNyoppstartet(false)
                                .registrertIUtlandet(true)
                                .bruttoInntekt(BigDecimal(500_000))
                                .erVarigEndring(false)
                                .endringDato(null)
                                .endringBegrunnelse(null)
                                .landkode(Landkode.SPANIA)
                                .regnskapsførerNavn(null)
                                .regnskapsførerTelefon(null)
                                .virksomhetstyper(listOf(VirksomhetType.FISKE))
                                .build()
                        ),
                        Organisasjonsnummer.of("54549049090490498048940940"),
                        "Something Fishy AS"
                    ),
                ),
                Frilanser(LocalDate.parse("2020-01-01"), null, true),
            ),
            listOf(
                FraværPeriode(
                    Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-05")),
                    Duration.ofHours(7),
                    FraværÅrsak.STENGT_SKOLE_ELLER_BARNEHAGE,
                    listOf(AktivitetFravær.FRILANSER)
                ),
                FraværPeriode(
                    Periode(LocalDate.parse("2020-01-06"), LocalDate.parse("2020-01-10")),
                    Duration.ofHours(4),
                    FraværÅrsak.SMITTEVERNHENSYN,
                    listOf(AktivitetFravær.SELVSTENDIG_VIRKSOMHET)
                ),
            ),
            Bosteder().medPerioder(
                mapOf(
                    Periode(
                        LocalDate.parse("2020-01-01"),
                        LocalDate.parse("2020-01-05")
                    ) to Bosteder.BostedPeriodeInfo().medLand(Landkode.SPANIA),
                    Periode(
                        LocalDate.parse("2020-01-06"),
                        LocalDate.parse("2020-01-10")
                    ) to Bosteder.BostedPeriodeInfo().medLand(Landkode.NORGE)
                )
            ),
            Utenlandsopphold().medPerioder(
                mapOf(
                    Periode(
                        LocalDate.parse("2020-01-01"),
                        LocalDate.parse("2020-01-05")
                    ) to Utenlandsopphold.UtenlandsoppholdPeriodeInfo()
                        .medLand(Landkode.CANADA)
                        .medÅrsak(BARNET_INNLAGT_I_HELSEINSTITUSJON_DEKKET_ETTER_AVTALE_MED_ET_ANNET_LAND_OM_TRYGD),
                    Periode(
                        LocalDate.parse("2020-01-06"),
                        LocalDate.parse("2020-01-10")
                    ) to Utenlandsopphold.UtenlandsoppholdPeriodeInfo()
                        .medLand(Landkode.SVERIGE)
                        .medÅrsak(BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING)
                )
            )
        )
    )
}


internal fun MeldingV1.somJson() = SøknadUtils.objectMapper.writeValueAsString(this)
