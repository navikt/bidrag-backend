package no.nav.bidrag.dokument.journalpost.hendelse;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.TEST;
import static no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger.enJournalpost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Objects;
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest;
import no.nav.bidrag.dokument.journalpost.model.InitJournalpostHendelseKt;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@ActiveProfiles({TEST})
@EmbeddedKafka
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BidragDokumentJournalpostLocalTest.class)
@EnableWireMock(value = @ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
class JournalpostKafkaEventProducerTest {

  @Autowired
  private JournalpostKafkaEventProducer journalpostKafkaEventProducer;

  @MockitoBean
  private KafkaTemplate<String, String> kafkaTemplateMock;

  @Value("${TOPIC_JOURNALPOST}")
  private String topic;

  @Test
  @DisplayName("skal publisere journalpost hendelser")
  void skalPublisereJournalpostHendelser() {
    var jp101 = enJournalpost().medJournalpostId(101).hent().tilJournalpostIntern();
    journalpostKafkaEventProducer.publish(InitJournalpostHendelseKt.initJournalpostHendelse(jp101));

    var jsonCaptor = ArgumentCaptor.forClass(String.class);
    var journalpostId1 = Objects.requireNonNull(jp101.getJournalpostId());
    verify(kafkaTemplateMock).send(eq(topic), eq(journalpostId1), jsonCaptor.capture());

    var journalpostId = """
        "journalpostId":"BID-101"
        """.trim();

    assertThat(jsonCaptor.getValue()).containsSequence(journalpostId);
  }
}
