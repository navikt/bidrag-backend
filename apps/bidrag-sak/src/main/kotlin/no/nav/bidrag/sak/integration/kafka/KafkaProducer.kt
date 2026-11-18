package no.nav.bidrag.sak.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.sak.config.KafkaConfig
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.bidrag.transport.sak.SakHendelse
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service

@Service
class KafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val kafkaConfig: KafkaConfig,
) {
    private val logger = KotlinLogging.logger {}

    @Retryable(
        value = [Exception::class],
        maxAttempts = 10,
        backoff = Backoff(delay = 1000, maxDelay = 12000, multiplier = 2.0),
    )
    fun sendSakshendelse(sakshendelse: SakHendelse) {
        sendKafkamelding(kafkaConfig.topicSak, sakshendelse.saksnummer.toString(), sakshendelse)
    }

    private fun sendKafkamelding(
        topic: String,
        key: String,
        request: Any,
    ) {
        val melding = commonObjectmapper.writeValueAsString(request)
        kafkaTemplate
            .send(topic, key, melding)
            .thenAccept {
                logger.info { "Melding på topic $topic for saksnummer $key er sendt. Fikk offset ${it?.recordMetadata?.offset()}" }
            }.exceptionally {
                val feilmelding =
                    "Melding på topic $topic kan ikke sendes for saksnummer $key. Feiler med ${it.message}"
                logger.warn { feilmelding }
                error(feilmelding)
            }
    }
}
