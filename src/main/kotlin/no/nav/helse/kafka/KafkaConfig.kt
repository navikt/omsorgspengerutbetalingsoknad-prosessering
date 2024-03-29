package no.nav.helse.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.streams.StreamsConfig.*
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Duration
import java.util.*

private val logger: Logger = LoggerFactory.getLogger(KafkaConfig::class.java)
private const val ID_PREFIX = "omsut-prs-"

class KafkaConfig(
    bootstrapServers: String,
    keyStore: Pair<String, String>?,
    trustStore: Pair<String, String>?,
    exactlyOnce: Boolean,
    autoOffsetReset: String,
    internal val unreadyAfterStreamStoppedIn: Duration
) {
    private val streams = Properties().apply {
        put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler::class.java)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset)
        medTrustStore(trustStore)
        medKeyStore(keyStore)
        medProcessingGuarantee(exactlyOnce)
    }

    internal fun stream(name: String) = streams.apply {
        put(APPLICATION_ID_CONFIG, "$ID_PREFIX$name")
    }
}

private fun Properties.medProcessingGuarantee(exactlyOnce: Boolean) {
    if (exactlyOnce) {
        logger.info("$PROCESSING_GUARANTEE_CONFIG=$EXACTLY_ONCE_V2")
        put(PROCESSING_GUARANTEE_CONFIG, EXACTLY_ONCE_V2)
        put(REPLICATION_FACTOR_CONFIG, "3")
    } else {
        logger.info("$PROCESSING_GUARANTEE_CONFIG=$AT_LEAST_ONCE")
        put(PROCESSING_GUARANTEE_CONFIG, AT_LEAST_ONCE)
    }
}

private fun Properties.medTrustStore(trustStore: Pair<String, String>?) {
    trustStore?.let {
        try {
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks")
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(it.first).absolutePath)
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, it.second)
            logger.info("Truststore på '${it.first}' konfigurert.")
        } catch (cause: Throwable) {
            logger.error(
                "Feilet for konfigurering av truststore på '${it.first}'",
                cause
            )
        }
    }
}

private fun Properties.medKeyStore(keyStore: Pair<String, String>?) {
    keyStore?.let {
        try {
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, File(it.first).absolutePath)
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, it.second)
            logger.info("Keystore på '${it.first}' konfigurert.")
        } catch (cause: Throwable) {}
    }
}
