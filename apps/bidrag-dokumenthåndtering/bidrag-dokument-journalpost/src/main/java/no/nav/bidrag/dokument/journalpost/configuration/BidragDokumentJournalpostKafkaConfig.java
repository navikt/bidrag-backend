package no.nav.bidrag.dokument.journalpost.configuration;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.LOCAL;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostKafkaEventProducer;
import no.nav.bidrag.transport.felles.JsonUtilsKt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@Profile("!" + LOCAL)
public class BidragDokumentJournalpostKafkaConfig {

  @Bean
  public JournalpostKafkaEventProducer journalpostKafkaEventProducer(
      KafkaTemplate<String, String> kafkaTemplate,
      @Value("${TOPIC_JOURNALPOST}") String topic
  ) {
    return new JournalpostKafkaEventProducer(kafkaTemplate, JsonUtilsKt.getCommonObjectmapper(), topic);
  }
}
