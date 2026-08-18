package no.nav.bidrag.statistikk.hendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.statistikk.SECURE_LOGGER
import no.nav.bidrag.transport.behandling.statistikk.BidragHendelse
import no.nav.bidrag.transport.behandling.statistikk.ForskuddHendelse
import no.nav.bidrag.transport.behandling.statistikk.SærbidragHendelse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

private val LOGGER = KotlinLogging.logger {}

@Service
class StatistikkKafkaEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${TOPIC_STATISTIKK}") private val topicForskudd: String,
    @Value("\${TOPIC_STATISTIKK_BIDRAG}") private val topicBidrag: String,
    @Value("\${TOPIC_STATISTIKK_SAERBIDRAG}") private val topicSærbidrag: String,
) {

    fun publishForskudd(forskuddHendelse: ForskuddHendelse): Long? {
        val headers = listOf(RecordHeader("stønadstype", "FORSKUDD".toByteArray()))
        val record = ProducerRecord(
            topicForskudd,
            null,
            forskuddHendelse.vedtaksid.toString() + forskuddHendelse.kravhaver,
            commonObjectmapper.writeValueAsString(forskuddHendelse),
            headers,
        )

        try {
            return kafkaTemplate.send(
                record,
            ).get().recordMetadata.offset()
        } catch (e: Exception) {
            SECURE_LOGGER.error("Det skjedde en feil ved sending av kafkamelding med forskuddsvedtak, $record. Exception: $e")
            throw IllegalStateException(e.message, e)
        }
    }

    fun publishBidrag(bidragHendelse: BidragHendelse): Long? {
        val headers = listOf(RecordHeader("stønadstype", bidragHendelse.stønadstype.toString().toByteArray()))
        val record = ProducerRecord(
            topicBidrag,
            null,
            bidragHendelse.vedtaksid.toString() + bidragHendelse.kravhaver + bidragHendelse.stønadstype,
            commonObjectmapper.writeValueAsString(bidragHendelse),
            headers,
        )

        try {
            return kafkaTemplate.send(
                record,
            ).get().recordMetadata.offset()
        } catch (e: Exception) {
            SECURE_LOGGER.error("Det skjedde en feil ved sending av kafkamelding med bidragsvedtak, $record. Exception: $e")
            throw IllegalStateException(e.message, e)
        }
    }

    fun publishSærbidrag(særbidragHendelse: SærbidragHendelse): Long? {
        val headers = listOf(RecordHeader("kategori", særbidragHendelse.kategori.toString().toByteArray()))
        val record = ProducerRecord(
            topicSærbidrag,
            null,
            særbidragHendelse.vedtaksid.toString() + særbidragHendelse.kravhaver + særbidragHendelse.kategori,
            commonObjectmapper.writeValueAsString(særbidragHendelse),
            headers,
        )

        try {
            return kafkaTemplate.send(
                record,
            ).get().recordMetadata.offset()
        } catch (e: Exception) {
            SECURE_LOGGER.error("Det skjedde en feil ved sending av kafkamelding med særbidragsvedtak, $record. Exception: $e")
            throw IllegalStateException(e.message, e)
        }
    }
}
