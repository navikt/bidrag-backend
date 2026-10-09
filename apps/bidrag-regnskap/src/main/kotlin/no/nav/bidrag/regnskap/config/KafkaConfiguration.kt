package no.nav.bidrag.regnskap.config

import no.nav.bidrag.commons.util.secureLogger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.ExponentialBackOff
import org.springframework.util.backoff.FixedBackOff
import org.springframework.web.client.RestClientResponseException

@Configuration
class KafkaConfiguration {

    companion object {
        private const val BACKOFF_MULTIPLIER = 1.2
        private const val MAX_INTERVAL_MS = 300000L // 5 mins
    }

    @Bean
    fun defaultErrorHandler(): DefaultErrorHandler {
        val errorHandler = opprettErrorHandler()
        errorHandler.setBackOffFunction { _, e -> if (e.erIkkeFunnet()) FixedBackOff(0, 0) else null }
        errorHandler.setRetryListeners(KafkaRetryListener())
        return errorHandler
    }

    private fun opprettErrorHandler(): DefaultErrorHandler = DefaultErrorHandler({ rec, e ->
        val key = rec.key()
        val value = rec.value()
        val offset = rec.offset()
        val topic = rec.topic()
        val partition = rec.partition()
        secureLogger.error(e) {
            "Kafka melding med nøkkel $key, partition $partition og topic $topic feilet på offset $offset. Melding som feilet: $value"
        }
    }, opprettBackoffPolicy())

    private fun opprettBackoffPolicy(): ExponentialBackOff = ExponentialBackOff().apply {
        multiplier = BACKOFF_MULTIPLIER
        maxInterval = MAX_INTERVAL_MS
    }
}

// En 404 blir ikke rettet av seg selv, så retry ville blokkert partisjonen for alltid
internal fun Throwable.erIkkeFunnet(): Boolean = generateSequence(this) { it.cause }
    .any { it is RestClientResponseException && it.statusCode == HttpStatus.NOT_FOUND }
