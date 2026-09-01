package no.nav.bidrag.automatiskjobb.kafka

import no.nav.bidrag.automatiskjobb.service.SakService
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.bidrag.transport.sak.SakHendelse
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

@Component
class SakHendelseListener(
    private val sakService: SakService,
) {
    @KafkaListener(
        topics = ["\${KAFKA_SAK_HENDELSE_TOPIC}"],
        groupId = "\${SAK_HENDELSE_KAFKA_GROUP_ID:bidrag-automatisk-jobb-sak}",
        properties = ["auto.offset.reset=earliest"],
    )
    fun behandleSakHendelse(
        hendelse: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.GROUP_ID) groupId: String,
    ) {
        secureLogger.info { "Leser hendelse fra topic: $topic, offset: $offset, partition: $partition, groupId: $groupId" }
        try {
            val sakHendelse = commonObjectmapper.readValue(hendelse, SakHendelse::class.java)
            secureLogger.info { "Behandler sakhendelse $sakHendelse" }
            sakService.behandleSakHendelse(sakHendelse)
        } catch (e: Exception) {
            secureLogger.error(e) { "Det skjedde en feil ved behandling av sakhendelse" }
        }
    }
}
