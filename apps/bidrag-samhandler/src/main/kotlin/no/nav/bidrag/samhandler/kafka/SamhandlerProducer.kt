package no.nav.bidrag.samhandler.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.samhandler.config.KafkaConfig
import no.nav.bidrag.transport.samhandler.Samhandlerhendelse
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service

@Service
class SamhandlerProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val kafkaConfig: KafkaConfig,
    private val objectMapper: ObjectMapper,
) {
    @Retryable(
        value = [Exception::class],
        maxAttempts = 10,
        backoff = Backoff(delay = 1000, maxDelay = 12000, multiplier = 2.0),
    )
    fun sendSamhandlerMelding(samhandlerhendelse: Samhandlerhendelse) {
        sendKafkamelding(kafkaConfig.topicSamhandler, samhandlerhendelse)
    }

    private fun sendKafkamelding(
        topic: String,
        samhandlerhendelse: Samhandlerhendelse,
    ) {
        val melding = objectMapper.writeValueAsString(samhandlerhendelse)
        kafkaTemplate
            .send(topic, samhandlerhendelse.samhandlerId, melding)
            .thenAccept {
                secureLogger.info {
                    "Melding på topic $topic for samhandlerId ${samhandlerhendelse.samhandlerId} er sendt. " +
                        "Fikk offset ${it?.recordMetadata?.offset()}"
                }
            }.exceptionally {
                val feilmelding =
                    "Melding på topic $topic kan ikke sendes for samhandlerId ${samhandlerhendelse.samhandlerId}. " +
                        "Feiler med ${it.message}"
                secureLogger.warn { feilmelding }
                error(feilmelding)
            }
    }
}
