package no.nav.bidrag.belopshistorikk.hendelse

import com.fasterxml.jackson.core.JacksonException
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.belopshistorikk.service.BehandleHendelseService
import no.nav.bidrag.belopshistorikk.service.JsonMapperService
import no.nav.bidrag.commons.util.secureLogger
import org.apache.kafka.common.TopicPartition
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.AbstractConsumerSeekAware
import org.springframework.kafka.listener.ConsumerSeekAware.ConsumerSeekCallback
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

private val LOGGER = KotlinLogging.logger {}

@Component
class VedtakHendelseListener(
    private val jsonMapperService: JsonMapperService,
    private val behandeHendelseService: BehandleHendelseService,
    @Value("\${kafka.vedtak.start-offset:}") startOffset: String,
) : AbstractConsumerSeekAware() {
    private val startOffset = startOffset.toLongOrNull()
    private val startOffsetApplied = AtomicBoolean(false)

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
//            throw e
        }
    }

    override fun onPartitionsAssigned(
        assignments: Map<TopicPartition, Long>,
        callback: ConsumerSeekCallback,
    ) {
        val offset = startOffset ?: return
        if (!startOffsetApplied.compareAndSet(false, true)) return

        assignments.forEach { (topicPartition, _) ->
            LOGGER.warn { "Setter Kafka-offset for ${topicPartition.topic()} partition ${topicPartition.partition()} til $offset" }
            callback.seek(topicPartition.topic(), topicPartition.partition(), offset)
        }
    }
}
