package no.nav.bidrag.dokument.journalpost;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.TEST;
import static no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.ENHETSNUMMER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import no.nav.bidrag.commons.CorrelationId;
import no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger;
import no.nav.bidrag.dokument.journalpost.model.InitJournalpostHendelseKt;
import no.nav.bidrag.dokument.journalpost.utils.TestUtilsKt;
import no.nav.bidrag.transport.dokument.AvvikType;
import no.nav.bidrag.transport.dokument.Avvikshendelse;
import no.nav.bidrag.transport.felles.JsonUtilsKt;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@ActiveProfiles(TEST)
@DisplayName("JsonMapping")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BidragDokumentJournalpostLocalTest.class)
@EnableWireMock(value = @ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
class JsonMappingTest {

  @Test
  @DisplayName("skal mappe Avvikshendelse")
  void skalMappeAvvikshendelse() throws IOException {
    var avvikshendelse = """
        {
          "avvikType":"BESTILL_ORIGINAL",
          "detaljer": {
            "enhetsnummer":"4806"
          }
        }
        """;

    Avvikshendelse hendelsen = JsonUtilsKt.getCommonObjectmapper().readValue(avvikshendelse, Avvikshendelse.class);

    assertAll(
        () -> assertThat(hendelsen).as("avvikshendelse").isNotNull(),
        () -> assertThat(hendelsen.getDetaljer()).as("avvikshendelse.detaljer.enhetsnummer")
            .extracting(ENHETSNUMMER).isEqualTo("4806"),
        () -> assertThat(hendelsen.getAvvikType()).as("avvikshendelse.avvikType").isEqualTo(AvvikType.BESTILL_ORIGINAL.name()),
        () -> assertThat(hendelsen.hent()).as("avvikshendelse.avvikType (som enum)").isEqualTo(AvvikType.BESTILL_ORIGINAL)
    );
  }

  @Test
  @DisplayName("skal mappe kafka meldingsobjekt for mottaksregistrering til json")
  void skalMappeKafkaObjektForMottaksregistreringTilJson() throws JsonProcessingException {
    var jp69 = JournalpostBygger.enJournalpost().medJournalpostId(69).hent().tilJournalpostIntern();
    var mottaksregistrertJournalpost = InitJournalpostHendelseKt.initJournalpostHendelse(jp69);

    var json = JsonUtilsKt.getCommonObjectmapper().writeValueAsString(mottaksregistrertJournalpost);

    var journalpostId = """
        "journalpostId":"BID-69"
        """.trim();

    assertThat(json).as("journalpostId").containsSequence(journalpostId);
  }

  @Test
  @DisplayName("skal legge ved sporingsdata på json meldinger")
  void skalLeggeVedSporingsdataPaJsonTilKafka() throws JsonProcessingException {
    TestUtilsKt.timestampCorrelationIdForThread("junit");
    var jp101 = JournalpostBygger.enJournalpost().medJournalpostId(101).hent().tilJournalpostIntern();
    var journalforJournalpost = InitJournalpostHendelseKt.initJournalpostHendelse(jp101);
    var json = JsonUtilsKt.getCommonObjectmapper().writeValueAsString(journalforJournalpost);

    var correlationId = String.format("""
        "correlationId":"%s"
        """.trim(), CorrelationId.Companion.fetchCorrelationIdForThread());

    assertThat(json).as("correlationId").containsSequence(correlationId);
  }

}
