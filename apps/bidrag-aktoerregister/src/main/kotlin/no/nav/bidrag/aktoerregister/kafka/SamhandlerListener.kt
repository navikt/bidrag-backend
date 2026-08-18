package no.nav.bidrag.aktoerregister.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.aktoerregister.service.SamhandlerhendelseService
import no.nav.bidrag.transport.samhandler.Samhandlerhendelse
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class SamhandlerListener(private val samhandlerhendelseService: SamhandlerhendelseService, private val objectMapper: ObjectMapper) {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SamhandlerListener::class.java)
    }

    @KafkaListener(topics = ["\${TOPIC_SAMHANDLER}"], groupId = "\${KAFKA_GROUP_ID}")
    fun listen(
        hendelse: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.GROUP_ID) groupId: String,
    ) {
        val samhandlerhendelse = objectMapper.readValue(hendelse, Samhandlerhendelse::class.java)
        LOGGER.info(
            "Behandler samhandlerhendelse for samhandler: ${samhandlerhendelse.samhandlerId} med offset: $offset i consumergroup: $groupId for topic: $topic",
        )
        samhandlerhendelseService.behandleHendelse(samhandlerhendelse)
    }
}
