package no.nav.bidrag.aktoerregister.hendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.aktoerregister.service.PersonHendelseService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger { }

@Component
class PersonHendelseListener(
    private val personHendelseService: PersonHendelseService,
) {

    @KafkaListener(groupId = "\${KAFKA_GROUP_ID}", topics = ["\${TOPIC_PERSONHENDELSE}"])
    fun lesHendelse(
        hendelse: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.GROUP_ID) groupId: String,
    ) {
        LOGGER.debug { "Leser hendelse fra topic: $topic, offset: $offset, partition: $partition, groupId: $groupId" }
        personHendelseService.behandleHendelse(hendelse)
    }
}
