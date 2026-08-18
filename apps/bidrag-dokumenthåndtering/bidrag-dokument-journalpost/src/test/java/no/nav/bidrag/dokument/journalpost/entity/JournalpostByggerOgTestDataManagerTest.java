package no.nav.bidrag.dokument.journalpost.entity;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.TEST;
import static no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger.enJournalpost;
import static no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger.enJournalpostSomErFeilfort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import jakarta.transaction.Transactional;
import java.util.List;
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest;
import no.nav.bidrag.dokument.journalpost.TestDataManager;
import no.nav.bidrag.dokument.journalpost.model.Fagomrade;
import no.nav.bidrag.dokument.journalpost.repository.JournalpostRepository;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@ActiveProfiles(TEST)
@DisplayName("En JournalpostBygger og TestDataManager")
@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BidragDokumentJournalpostLocalTest.class)
@EnableWireMock(value = @ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
public class JournalpostByggerOgTestDataManagerTest {

  @Autowired
  private TestDataManager testDataManager;

  @Autowired
  private JournalpostRepository journalpostRepository;

  @Test
  @DisplayName("skal opprette en feilført journalpost")
  void skalByggeJournalpostUtenSak() {
    var byggEnFeilfortJournalpost = enJournalpostSomErFeilfort("1001")
        .medAvsender("Bert, Sky").medFagomrade(Fagomrade.BIDRAG_DATABASE);

    var opprettetId = testDataManager.opprett(byggEnFeilfortJournalpost).getJournalpostId();
    var muligJournalpost = journalpostRepository.findById(opprettetId);

    assertThat(muligJournalpost).hasValueSatisfying(journalpost -> assertAll(
        () -> assertThat(journalpost.getAvsender()).as("avsender").isEqualTo("Bert"),
        () -> assertThat(journalpost.getAvsenderFornavn()).as("avsenderForhavn").isEqualTo("Sky"),
        () -> assertThat(journalpost.hentTilknyttetSaksnummer()).as("tilknyttetSaksnummer").isNull(),
        () -> assertThat(journalpost.getJournalsaker()).as("saksnummer").hasSize(1).first().extracting(Journalsak::getSaksnummer).isEqualTo("1001"),
        () -> assertThat(journalpost.getFagomrade()).as("fagområde (database)").isEqualTo(Fagomrade.BIDRAG_DATABASE),
        () -> assertThat(journalpost.erFeilfort()).as("feilført").isTrue()
    ));
  }

  @Test
  @DisplayName("skal initialisere en journalpost")
  void skalInitialisereEnJournalpost() {
    var journalpost = enJournalpost().medJournalpostId(101).medJournalstatus("X").leggTilSaksnummer("1001").hent();

    assertAll(
        () -> assertThat(journalpost.getJournalpostId()).as("journalpostId").isEqualTo(101),
        () -> assertThat(journalpost.getJournalstatus()).as("journalstatus").isEqualTo("X"),
        () -> assertThat(journalpost.getJournalsaker()).as("journalsaker").hasSize(1)
            .as("saksnummer").first().extracting(Journalsak::getSaksnummer).isEqualTo("1001")
    );
  }

  @Test
  @DisplayName("skal initialisere en journalpost med 2 saker")
  void skalInitialisereEnJournalpostMedToSaker() {
    var journalpost = enJournalpost()
        .medJournalpostId(101).medJournalstatus("X").leggTilSaksnummer("1001").leggTilSaksnummer("1002")
        .hent();

    assertAll(
        () -> assertThat(journalpost.getJournalpostId()).as("journalpostId").isEqualTo(101),
        () -> assertThat(journalpost.getJournalstatus()).as("journalstatus").isEqualTo("X"),
        () -> assertThat(journalpost.getJournalsaker()).as("journalsaker").hasSize(2)
            .as("saksnummer").extracting(Journalsak::getSaksnummer).isEqualTo(List.of("1001", "1002"))
    );
  }
}
