package no.nav.bidrag.dokument.journalpost.hendelse;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.TEST;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest;
import no.nav.bidrag.transport.dokument.HendelseType;
import no.nav.bidrag.transport.dokument.JournalpostHendelse;
import no.nav.bidrag.transport.dokument.JournalpostStatus;
import no.nav.bidrag.transport.dokument.Sporingsdata;
import no.nav.bidrag.transport.felles.JsonUtilsKt;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@ActiveProfiles(TEST)
@DisplayName("JournalpostHendelse og mapping til json")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BidragDokumentJournalpostLocalTest.class, properties = "STS_URL=junit")
@EnableWireMock(value = @ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
public class JournalpostHendelseMapperTest {

  private final ObjectMapper objectMapper = JsonUtilsKt.getCommonObjectmapper();

  @Test
  @DisplayName("skal mappe til json")
  void skalMappeTilJson() throws JsonProcessingException {
    var sporingsdata = new Sporingsdata("correlationId", "jactor-rises", "james bond", "enhetsnummer");
    var journalpostHendelse = new JournalpostHendelse(
        "BID-10101", "aktorId", "fnr", "behtema", "tittel","fagomrade", "tema", "batchid", "I", HendelseType.ENDRING, "enhet", "journalstatus", JournalpostStatus.FERDIGSTILT, sporingsdata, new ArrayList<>(),
        LocalDate.now(), LocalDate.now(), null, null
    );

    var json = objectMapper.writeValueAsString(journalpostHendelse);

    assertThat(json).as("json").isNotNull();

    assertAll(
        () -> assertThat(json).as("correlationId").containsSequence("""
            "correlationId":"correlationId"
            """.stripIndent().trim()),
        () -> assertThat(json).as("brukerident").containsSequence("""
            "brukerident":"jactor-rises"
            """.stripIndent().trim()),
        () -> assertThat(json).as("saksbehandlersNavn").containsSequence("""
            "saksbehandlersNavn":"james bond"
            """.stripIndent().trim()),
        () -> assertThat(json).as("journalpostId").containsSequence("""
            "journalpostId":"BID-10101"
            """.stripIndent().trim()),
        () -> assertThat(json).as("aktorId").containsSequence("""
            "aktorId":"aktorId"
            """.stripIndent().trim()),
        () -> assertThat(json).as("fagomrade").containsSequence("""
            "fagomrade":"fagomrade"
            """.stripIndent().trim()),
        () -> assertThat(json).as("enhet").containsSequence("""
            "enhet":"enhet"
            """.stripIndent().trim()),
        () -> assertThat(json).as("journalstatus").containsSequence("""
            "journalstatus":"journalstatus"
            """.stripIndent().trim()),
        () -> assertThat(json).as("enhetsnummer").containsSequence("""
            "enhetsnummer":"enhetsnummer"
            """.stripIndent().trim())
    );
  }
}
