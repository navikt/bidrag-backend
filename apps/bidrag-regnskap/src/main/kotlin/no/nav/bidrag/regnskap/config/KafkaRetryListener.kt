package no.nav.bidrag.regnskap.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.RetryListener

private val LOGGER = KotlinLogging.logger { }

class KafkaRetryListener : RetryListener {

    override fun failedDelivery(record: ConsumerRecord<*, *>, exception: Exception?, deliveryAttempt: Int) {
        LOGGER.error(exception) { "Håndtering av kafka melding ${record.value()} feilet. Dette er $deliveryAttempt. forsøk" }
    }

    override fun recovered(record: ConsumerRecord<*, *>, exception: Exception?) {
        LOGGER.error(exception) { "Håndtering av kafka melding ${record.value()} er enten suksess eller ignorert pågrunn av ugyldig data" }
    }

    override fun recoveryFailed(record: ConsumerRecord<*, *>, original: Exception?, failure: Exception) {
    }
}
