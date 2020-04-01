package no.nav.omsorgspengerutbetaling.arbeidstakerutbetaling

import no.nav.helse.aktoer.AktørId
import no.nav.helse.prosessering.v1.*
import java.net.URI
import java.time.ZonedDateTime

data class PreprosessertArbeidstakerutbetalingMelding(
    val soknadId: String,
    val mottatt: ZonedDateTime,
    val språk: String?,
    val søker: PreprossesertSøker,
    val arbeidsgivere: ArbeidsgiverDetaljer,
    val bosteder: List<Bosted>,
    val opphold: List<Opphold>,
    val spørsmål: List<SpørsmålOgSvar>,
    val utbetalingsperioder: List<Utbetalingsperiode>,
    val fosterbarn: List<FosterBarn>? = listOf(),
    val bekreftelser: Bekreftelser,
    val dokumentUrls: List<List<URI>>
) {
    internal constructor(
        melding: ArbeidstakerutbetalingMelding,
        dokumentUrls: List<List<URI>>,
        søkerAktørId: AktørId
    ) : this(
        soknadId = melding.søknadId,
        mottatt = melding.mottatt,
        språk = melding.språk,
        søker = PreprossesertSøker(melding.søker, søkerAktørId),
        arbeidsgivere = melding.arbeidsgivere,
        bosteder = melding.bosteder,
        opphold = melding.opphold,
        spørsmål = melding.spørsmål,
        utbetalingsperioder = melding.utbetalingsperioder,
        fosterbarn = melding.fosterbarn,
        bekreftelser = melding.bekreftelser,
        dokumentUrls = dokumentUrls
    )
}
