package no.nav.helse.prosessering.v1.asynkron

import de.huxhorn.sulky.ulid.ULID
import no.nav.helse.kafka.KafkaConfig
import no.nav.helse.prosessering.Metadata
import no.nav.helse.prosessering.v1.MeldingV1
import no.nav.k9.rapid.behov.AleneOmOmsorgenBehov
import no.nav.k9.rapid.behov.Behovssekvens
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.json.JSONObject
import org.slf4j.LoggerFactory

class AleneOmOmsorgenProducer(
    private val kafkaConfig: KafkaConfig
) {

    private companion object {
        private val NAME = "AleneOmOmsorgenProducer"
        private val K9_RAPID_V2_TOPIC = TopicUse(
            name = Topics.K9_RAPID_V2.name,
            valueSerializer = SøknadSerializer()
        )
        private val logger = LoggerFactory.getLogger(AleneOmOmsorgenProducer::class.java)
    }


    private val producer = KafkaProducer(
        kafkaConfig.producer(NAME),
        K9_RAPID_V2_TOPIC.keySerializer(),
        K9_RAPID_V2_TOPIC.valueSerializer
    )

    fun leggPåKø(
        melding: MeldingV1,
        metadata: Metadata,
        ulid: String = ULID().nextULID()
    ) {
        val (id: String, løsning: String) = melding.tilK9RapidBehovssekvens(metadata, ulid).keyValue

        val recordMetaData = producer.send(
            ProducerRecord(
                K9_RAPID_V2_TOPIC.name,
                id,
                TopicEntry(
                    metadata = metadata,
                    data = JSONObject(løsning)
                )
            )
        ).get()

        logger.info("Registrering av barn med aleneomsorg sendt til ${K9_RAPID_V2_TOPIC.name} med offset ${recordMetaData.offset()}")
    }

    internal fun stop() = producer.close()
}

fun MeldingV1.tilK9RapidBehovssekvens(metadata: Metadata, ulid: String = ULID().nextULID()) : Behovssekvens {

    val aleneOmOmsorgenBarn = barn
        .filter { it.aleneOmOmsorgen }
        .map { AleneOmOmsorgenBehov.Barn(identitetsnummer = it.identitetsnummer) }

    return Behovssekvens(
        id = ulid,
        correlationId = metadata.correlationId,
        behov = arrayOf(
            AleneOmOmsorgenBehov(
                identitetsnummer = søker.fødselsnummer,
                mottaksdato = mottatt.toLocalDate(),
                barn = aleneOmOmsorgenBarn
            )
        )
    )
}


class SøknadSerializer : Serializer<TopicEntry<JSONObject>> {
    override fun serialize(topic: String, data: TopicEntry<JSONObject>): ByteArray {

        return data
            .data
            .toString()
            .toByteArray()
    }

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}
