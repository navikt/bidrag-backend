package no.nav.bidrag.dokument.journalpost.service;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.TEST;
import static no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger.enJournalfortJournalpost;
import static no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger.enJournalpost;
import static no.nav.bidrag.dokument.journalpost.entity.JournalsakBygger.enJournalsak;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import jakarta.transaction.Transactional;
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest;
import no.nav.bidrag.dokument.journalpost.TestDataManager;
import no.nav.bidrag.dokument.journalpost.model.Journalstatus;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@ActiveProfiles(TEST)
@DisplayName("KodeService")
@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BidragDokumentJournalpostLocalTest.class)
@EnableWireMock(value = @ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
class KodeServiceTest {

  @Autowired
  private KodeService kodeService;
  @Autowired
  private TestDataManager testDataManager;

  @Test
  @DisplayName("skal lese KodeJournalstatus.skalVises fra database")
  void skalLeseFlaggFraDatabase() {
    testDataManager.opprettKodeForJournalstatusSomIkkeSkalVises(
        Journalstatus.AVVIK_BESTILL_RESKANNING, Journalstatus.AVVIK_BESTILL_SPLITTING, Journalstatus.AVVIK_ENDRE_FAGOMRADE, Journalstatus.SLETTET
    );

    var sakMedReskanning = enJournalsak().med(enJournalfortJournalpost().medJournalstatus(Journalstatus.AVVIK_BESTILL_RESKANNING)).bygg();
    var sakMedSplitting = enJournalsak().med(enJournalfortJournalpost().medJournalstatus(Journalstatus.AVVIK_BESTILL_SPLITTING)).bygg();
    var sakMedFagomrade = enJournalsak().med(enJournalfortJournalpost().medJournalstatus(Journalstatus.AVVIK_ENDRE_FAGOMRADE)).bygg();
    var sakMedSlettet = enJournalsak().med(enJournalfortJournalpost().medJournalstatus(Journalstatus.SLETTET)).bygg();

    var journalstatusKoder = List.of(
        kodeService.skalViseJournalpost(sakMedFagomrade),
        kodeService.skalViseJournalpost(sakMedReskanning),
        kodeService.skalViseJournalpost(sakMedSlettet),
        kodeService.skalViseJournalpost(sakMedSplitting)
    );

    assertThat(journalstatusKoder).as("antall koder")
        .hasSize(4).
        isEqualTo(List.of(false, false, false, false));
  }

  @Test
  @DisplayName("skal ikke vise journalsak der journalpostens journalstatus mangler flagg for visning")
  void skalIkkeViseJournalsakMedJournalpostDerJournalstatusManglerFlaggForVisning() {
    testDataManager.opprettKodeForJournalstatusSomIkkeSkalVises(Journalstatus.AVVIK_BESTILL_RESKANNING);
    var journalsak = enJournalsak().med(enJournalpost().medJournalstatus(Journalstatus.AVVIK_BESTILL_RESKANNING)).bygg();

    assertThat(kodeService.skalViseJournalpost(journalsak)).isFalse();
  }

  @Test
  @DisplayName("skal vise journalsak der journalpostens journalstatus har flagg for visning")
  void skalViseJournalsakMedJournalpostDerJournalstatusHarFlaggForVisning() {
    testDataManager.opprettKodeForJournalstatusSomSkalVises(Journalstatus.JOURNALFORT);
    var journalsak = enJournalsak().med(enJournalfortJournalpost()).bygg();

    assertThat(kodeService.skalViseJournalpost(journalsak)).isTrue();
  }

  @Test
  @DisplayName("skal ikke vise journalsak med journalpost som mangler journalstatus")
  void skalIkkeViseJournalsakMedJournalpostSomManglerJournalstatus() {
    var journalsak = enJournalsak().med(enJournalpost().medJournalstatus(null)).bygg();

    assertThat(kodeService.skalViseJournalpost(journalsak)).isFalse();
  }
}
