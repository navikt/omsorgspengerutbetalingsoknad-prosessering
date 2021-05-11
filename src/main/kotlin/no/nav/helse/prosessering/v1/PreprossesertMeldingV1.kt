package no.nav.helse.prosessering.v1

import no.nav.helse.aktoer.AktørId
import no.nav.k9.søknad.Søknad
import java.net.URI
import java.time.ZonedDateTime

data class PreprossesertMeldingV1(
    val soknadId: String,
    val mottatt: ZonedDateTime,
    val søker: PreprossesertSøker,
    val språk: String?,
    val harDekketTiFørsteDagerSelv: Boolean,
    val bosteder: List<Bosted>,
    val opphold: List<Opphold>,
    val spørsmål: List<SpørsmålOgSvar>,
    val dokumentUrls: List<List<URI>>,
    val utbetalingsperioder: List<Utbetalingsperiode>,
    val andreUtbetalinger: List<String>,
    val fosterbarn: List<FosterBarn>? = listOf(),
    val frilans: Frilans? = null,
    val selvstendigVirksomheter: List<Virksomhet> = listOf(),
    val bekreftelser: Bekreftelser,
    val k9FormatSøknad: Søknad
) {
    internal constructor(
        melding: MeldingV1,
        dokumentUrls: List<List<URI>>,
        søkerAktørId: AktørId
    ) : this(
        soknadId = melding.søknadId,
        mottatt = melding.mottatt,
        søker = PreprossesertSøker(melding.søker, søkerAktørId),
        språk = melding.språk,
        bosteder = melding.bosteder,
        opphold = melding.opphold,
        spørsmål = melding.spørsmål,
        dokumentUrls = dokumentUrls,
        harDekketTiFørsteDagerSelv = melding.harDekketTiFørsteDagerSelv,
        utbetalingsperioder = melding.utbetalingsperioder,
        andreUtbetalinger = melding.andreUtbetalinger,
        fosterbarn = melding.fosterbarn,
        frilans = melding.frilans,
        selvstendigVirksomheter = melding.selvstendigVirksomheter,
        bekreftelser = melding.bekreftelser,
        k9FormatSøknad = melding.k9FormatSøknad
    )
}

data class PreprossesertSøker(
    val fødselsnummer: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val aktørId: String
) {
    internal constructor(søker: Søker, aktørId: AktørId) : this(
        fødselsnummer = søker.fødselsnummer,
        fornavn = søker.fornavn,
        mellomnavn = søker.mellomnavn,
        etternavn = søker.etternavn,
        aktørId = aktørId.id
    )

    override fun toString(): String {
        return "PreprossesertSøker()"
    }


}
