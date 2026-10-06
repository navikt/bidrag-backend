package no.nav.bidrag.arbeidsflyt.hendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.sanitizeForLog
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.RetryListener
import java.lang.Exception

class KafkaRetryListener : RetryListener {
    companion object {
        @JvmStatic
        private val LOGGER = KotlinLogging.logger {}
    }

    override fun failedDelivery(
        record: ConsumerRecord<*, *>,
        ex: Exception?,
        deliveryAttempt: Int,
    ) {
        LOGGER.warn(ex) { "Håndtering av kafka melding ${record.value().sanitizeForLog()} feilet. Dette er $deliveryAttempt. forsøk" }
    }

    override fun recovered(
        record: ConsumerRecord<*, *>,
        ex: Exception?,
    ) {
        LOGGER.warn(ex) { "Håndtering av kafka melding ${record.value().sanitizeForLog()} er enten suksess eller ignorert pågrunn av ugyldig data" }
    }

    override fun recoveryFailed(
        record: ConsumerRecord<*, *>,
        original: Exception?,
        failure: Exception,
    ) {}
}
