package no.nav.bidrag.regnskap.hendelse.kafka.vedtak

import com.fasterxml.jackson.core.JacksonException
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.regnskap.controller.KafkaOffsettController
import no.nav.bidrag.regnskap.service.VedtakshendelseService
import org.apache.kafka.common.TopicPartition
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.AbstractConsumerSeekAware
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger { }

@Component
class VedtakshendelseListener(
    private val vedtakshendelseService: VedtakshendelseService,
    private val kafkaOffsettController: KafkaOffsettController,
) : AbstractConsumerSeekAware() {

    @KafkaListener(
        groupId = "bidrag-regnskap",
        topics = [$$"${TOPIC_VEDTAK}"],
        properties = [$$"auto.offset.reset=${KAFKA_AUTO_OFFSET_RESET:earliest}"],
    )
    fun lesHendelse(
        hendelse: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.GROUP_ID) groupId: String,
        acknowledgment: Acknowledgment,
    ) {
        try {
            secureLogger.info { "Starter behandling av vedtakhendelse med offset: $offset. Hendelse: $hendelse" }

            if (kafkaOffsettController.skalHoppeOverNesteMelding()) {
                hoppOverHendele(offset, acknowledgment)
                return
            }

            if (kafkaOffsettController.skalSetteNyOffset()) {
                settNyOffsett(topic, partition)
                return
            }

            val opprettedeOppdrag = vedtakshendelseService.behandleHendelse(hendelse)

            try {
                vedtakshendelseService.sendKrav(opprettedeOppdrag)
            } catch (e: Exception) {
                LOGGER.error(e) { "Oversending av krav feilet for oppdrag: $opprettedeOppdrag med offset: $offset!" }
            } finally {
                acknowledgment.acknowledge()
            }
        } catch (e: JacksonException) {
            secureLogger.error(e) {
                "Mapping av hendelse feilet for kafkamelding med offsett: $offset, topic: $topic, recieved_partition: $partition, groupId: $groupId " +
                    "\n\nHendelse: $hendelse"
            }
            throw e
        }
    }

    private fun settNyOffsett(topic: String, partition: Int) {
        val nyOffsett = kafkaOffsettController.hentNyOffset()
        LOGGER.warn { "Setter ny offsett til: $nyOffsett!" }
        this.getSeekCallbacksFor(TopicPartition(topic, partition))?.forEach { it.seek(topic, partition, nyOffsett) }
        kafkaOffsettController.tilbakestillOffsettEndring()
    }

    private fun hoppOverHendele(offset: Long, acknowledgment: Acknowledgment) {
        LOGGER.info { "Hopper over behandling av vedtakhendelse med offset: $offset" }
        acknowledgment.acknowledge()
        kafkaOffsettController.tilbakestillHoppOverNesteMelding()
    }
}
