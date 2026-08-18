package no.nav.bidrag.regnskap.hendelse.kafka.personhendelse

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.regnskap.service.PersonhendelseService
import no.nav.bidrag.transport.person.hendelse.Endringsmelding
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class PersonhendelseListener(
    private val personhendelseService: PersonhendelseService,
    private val objectMapper: ObjectMapper,
) {
    @KafkaListener(
        groupId = "\${PERSON_HENDELSE_KAFKA_GROUP_ID_SISTE}",
        topics = ["\${TOPIC_PERSONHENDELSE}"],
        properties = ["auto.offset.reset=latest"],
    )
    fun lesHendelse(
        hendelse: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.GROUP_ID) groupId: String,
        acknowledgment: Acknowledgment,
    ) {
        secureLogger.debug { "Leser hendelse fra topic: $topic, offset: $offset, partition: $partition, groupId: $groupId" }

        val endringsmelding = deserialiserEndringsmelding(hendelse)
        personhendelseService.behandlePersonhendelse(endringsmelding)
        acknowledgment.acknowledge()
    }

    private fun deserialiserEndringsmelding(hendelse: String): Endringsmelding = objectMapper.readValue(hendelse, Endringsmelding::class.java)
}
