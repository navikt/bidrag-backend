package no.nav.bidrag.behandling.kafka

import no.nav.bidrag.kafka.KafkaRetryListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.DefaultErrorHandler

@Configuration
class KafkaRetryConfiguration {
    @Bean
    fun defaultErrorHandler(): DefaultErrorHandler = DefaultErrorHandler().apply {
        setRetryListeners(KafkaRetryListener())
    }
}
