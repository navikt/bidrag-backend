package no.nav.bidrag.dokument.journalpost.hendelse;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpost.SECURE_LOGGER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.bidrag.transport.dokument.JournalpostHendelse;
import no.nav.bidrag.dokument.journalpost.exception.JournalpostHendelseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

public class JournalpostKafkaEventProducer {

  private static final Logger LOGGER = LoggerFactory.getLogger(JournalpostKafkaEventProducer.class);

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final String topic;

  public JournalpostKafkaEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.topic = topic;
  }

  public void publish(JournalpostHendelse journalpostHendelse) {
    LOGGER.info("Publiserer JournalpostHendelse for journalpostId {}", journalpostHendelse.getJournalpostId());
    SECURE_LOGGER.info("Publiserer JournalpostHendelse {}", journalpostHendelse);

    try {
      kafkaTemplate.send(topic, journalpostHendelse.getJournalpostId(), objectMapper.writeValueAsString(journalpostHendelse));
    } catch (JsonProcessingException e) {
      throw new JournalpostHendelseException(e.getMessage(), e);
    }
  }
}
