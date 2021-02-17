package no.nav.helse

import no.nav.k9.søknad.JsonUtils
import no.nav.k9.søknad.Søknad
import org.json.JSONObject
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.assertNotNull

internal fun String.assertJournalførtFormat() {
    val rawJson = JSONObject(this)

    val metadata = assertNotNull(rawJson.getJSONObject("metadata"))
    assertNotNull(metadata.getString("correlationId"))

    val data = assertNotNull(rawJson.getJSONObject("data"))

    assertNotNull(data.getJSONObject("journalførtMelding")).getString("journalpostId")
    val søknad = assertNotNull(data.getJSONObject("melding")).getJSONObject("k9FormatSøknad")

    val rekonstruertSøknad = JsonUtils.getObjectMapper().readValue(søknad.toString(), Søknad::class.java)
    JSONAssert.assertEquals(søknad.toString(), JsonUtils.toString(rekonstruertSøknad), true)
}
