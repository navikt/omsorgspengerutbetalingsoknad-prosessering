package no.nav.helse.prosessering.v1

import no.nav.helse.aktoer.AktørId
import no.nav.k9.søknad.Søknad
import java.time.ZonedDateTime

data class PreprossesertMeldingV1(
    val soknadId: String,
    val mottatt: ZonedDateTime,
    val søker: PreprossesertSøker,
    val språk: String?,
    val harDekketTiFørsteDagerSelv: Boolean?,
    val bosteder: List<Bosted>,
    val opphold: List<Opphold>,
    val spørsmål: List<SpørsmålOgSvar>,
    val dokumentId: List<List<String>>,
    val utbetalingsperioder: List<Utbetalingsperiode>,
    val andreUtbetalinger: List<AndreUtbetalinger>? = null,
    val barn: List<Barn>,
    val frilans: Frilans? = null,
    val selvstendigNæringsdrivende: SelvstendigNæringsdrivende? = null,
    val bekreftelser: Bekreftelser,
    val k9FormatSøknad: Søknad
) {
    internal constructor(
        melding: MeldingV1,
        dokumentId: List<List<String>>,
    ) : this(
        soknadId = melding.søknadId,
        mottatt = melding.mottatt,
        søker = PreprossesertSøker(melding.søker, AktørId(melding.søker.aktørId)),
        språk = melding.språk,
        bosteder = melding.bosteder,
        opphold = melding.opphold,
        spørsmål = melding.spørsmål,
        dokumentId = dokumentId,
        harDekketTiFørsteDagerSelv = melding.harDekketTiFørsteDagerSelv,
        utbetalingsperioder = melding.utbetalingsperioder,
        andreUtbetalinger = melding.andreUtbetalinger,
        barn = melding.barn,
        frilans = melding.frilans,
        selvstendigNæringsdrivende = melding.selvstendigNæringsdrivende,
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
