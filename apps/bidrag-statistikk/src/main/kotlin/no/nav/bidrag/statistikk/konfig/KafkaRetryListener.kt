package no.nav.bidrag.statistikk.konfig

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.statistikk.SECURE_LOGGER
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.RetryListener

private val LOGGER = KotlinLogging.logger { }

class KafkaRetryListener : RetryListener {

    override fun failedDelivery(record: ConsumerRecord<*, *>, exception: Exception?, deliveryAttempt: Int) {
        SECURE_LOGGER.error("Håndtering av kafkamelding ${record.value()} feilet. Dette er $deliveryAttempt. forsøk")
    }

    override fun recovered(record: ConsumerRecord<*, *>, ex: Exception?) {
        SECURE_LOGGER.error(
            "Håndtering av kafkamelding ${record.value()} kastet en enxception på grunn av ugyldige data",
            ex,
        )
    }

    override fun recoveryFailed(record: ConsumerRecord<*, *>, original: Exception?, failure: Exception) {
    }
}
