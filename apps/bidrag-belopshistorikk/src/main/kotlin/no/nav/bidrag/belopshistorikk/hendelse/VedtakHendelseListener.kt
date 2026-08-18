package no.nav.bidrag.belopshistorikk.hendelse

import com.fasterxml.jackson.core.JacksonException
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.belopshistorikk.service.BehandleHendelseService
import no.nav.bidrag.belopshistorikk.service.JsonMapperService
import no.nav.bidrag.commons.util.secureLogger
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class VedtakHendelseListener(private val jsonMapperService: JsonMapperService, private val behandeHendelseService: BehandleHendelseService) {

    @KafkaListener(
        groupId = "bidrag-belopshistorikk-8",
        topics = [$$"${TOPIC_VEDTAK}"],
        properties = ["auto.offset.reset=earliest"],
    )
    fun lesHendelse(
        hendelse: String,
        @Header(KafkaHeaders.OFFSET) offset: Long = 1,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String = "",
        @Header(KafkaHeaders.GROUP_ID) groupId: String = "",
    ) {
        try {
            LOGGER.info { "Behandler hendelse for offset: $offset, topic: $topic, groupId: $groupId" }
            val vedtakHendelse = jsonMapperService.mapHendelse(hendelse)
            behandeHendelseService.behandleHendelse(vedtakHendelse)
        } catch (e: JacksonException) {
            LOGGER.error { "Mapping av hendelse feilet for kafkamelding, se sikker logg for mer info" }
            secureLogger.error { "Mapping av hendelse feilet for kafkamelding: $hendelse" }
            throw e
        }
    }
}
