package no.nav.bidrag.dokument.journalpost.hendelse

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpost.SECURE_LOGGER
import no.nav.bidrag.dokument.journalpost.exception.JournalpostHendelseException
import no.nav.bidrag.transport.dokument.DokumentHendelse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class DokumentKafkaHendelseProdusent(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules(),
    @Value($$"${TOPIC_DOKUMENT}") val topic: String,
) {
    fun publish(dokumentHendelse: DokumentHendelse) {
        LOGGER.info("Publiserer dokumenthendelse med dokumentreferanse {}", dokumentHendelse.dokumentreferanse)
        SECURE_LOGGER.info("Publiserer dokumentHendelse {}", dokumentHendelse)
        try {
            kafkaTemplate.send(topic, dokumentHendelse.dokumentreferanse, objectMapper.writeValueAsString(dokumentHendelse))
        } catch (e: JsonProcessingException) {
            throw JournalpostHendelseException(e.message!!, e)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DokumentKafkaHendelseProdusent::class.java)
    }
}
