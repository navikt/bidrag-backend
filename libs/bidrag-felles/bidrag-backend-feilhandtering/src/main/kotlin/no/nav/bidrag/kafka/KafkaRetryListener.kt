package no.nav.bidrag.kafka

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.RetryListener

class KafkaRetryListener : RetryListener {
    override fun failedDelivery(
        record: ConsumerRecord<*, *>,
        ex: Exception?,
        deliveryAttempt: Int,
    ) {
        logFailure(record, ex, deliveryAttempt)
    }

    fun logFailure(record: ConsumerRecord<*, *>, e: Exception?, deliveryAttempt: Int? = null) {
        val cause = e?.let { generateSequence(it as Throwable) { error -> error.cause }.last() }
        val attempt = deliveryAttempt?.let { ", forsøk=$it" } ?: ""
        LOGGER.error {
            "Kafka-behandling feilet: topic=${record.topic()}, partisjon=${record.partition()}, " +
                "offset=${record.offset()}$attempt, feiltype=${cause?.javaClass?.simpleName ?: "Ukjent"}"
        }
        SECURE_LOGGER.error(e) {
            "Kafka-behandling feilet: topic=${record.topic()}, partisjon=${record.partition()}, offset=${record.offset()}$attempt, value=${record.value()}"
        }
    }

    override fun recovered(record: ConsumerRecord<*, *>, e: Exception?) {
        LOGGER.error {
            "Kafka-melding håndtert av recoverer etter feil: topic=${record.topic()}, partisjon=${record.partition()}, offset=${record.offset()}"
        }
        SECURE_LOGGER.error(e) {
            "Kafka-melding håndtert av recoverer etter feil: topic=${record.topic()}, partisjon=${record.partition()}, offset=${record.offset()}, value=${record.value()}"
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger {}
        private val SECURE_LOGGER = KotlinLogging.logger("secureLogger")
    }
}
