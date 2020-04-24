package no.nav.helse

import no.nav.k9.søknad.omsorgspenger.utbetaling.snf.OmsorgspengerUtbetalingSøknad
import no.nav.k9.søknad.omsorgspenger.utbetaling.arbeidstaker.OmsorgspengerUtbetalingSøknad as ArbeidstakerutbetalingSøknad
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.assertNotNull

internal fun String.assertJournalførtFormat() {
    val rawJson = JSONObject(this)

    val metadata = assertNotNull(rawJson.getJSONObject("metadata"))
    assertNotNull(metadata.getString("correlationId"))

    val data = assertNotNull(rawJson.getJSONObject("data"))

    assertNotNull(data.getString("journalpostId"))
    val søknad = assertNotNull(data.getJSONObject("søknad"))

    val rekonstruertSøknad = OmsorgspengerUtbetalingSøknad
        .builder()
        .json(søknad.toString())
        .build()

    JSONAssert.assertEquals(søknad.toString(), OmsorgspengerUtbetalingSøknad.SerDes.serialize(rekonstruertSøknad), true)
}

internal fun String.assertArbeidstakerutbetalingJournalførtFormat() {
    val rawJson = JSONObject(this)

    val metadata = assertNotNull(rawJson.getJSONObject("metadata"))
    assertNotNull(metadata.getString("correlationId"))

    val data = assertNotNull(rawJson.getJSONObject("data"))

    assertNotNull(data.getString("journalpostId"))
    val søknad = assertNotNull(data.getJSONObject("søknad"))

    val rekonstruertSøknad = ArbeidstakerutbetalingSøknad
        .builder()
        .json(søknad.toString())
        .build()

    JSONAssert.assertEquals(søknad.toString(), ArbeidstakerutbetalingSøknad.SerDes.serialize(rekonstruertSøknad), true)
}
