package no.nav.bidrag.dokument.journalpost;

import static no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger.enJournalpost;

import no.nav.bidrag.dokument.journalpost.model.Fagomrade;
import no.nav.bidrag.dokument.journalpost.model.Journalstatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile(BidragDokumentJournalpostProfiles.LOCAL)
public class DatabaseTestDataLoader implements ApplicationRunner {

  private final TestDataManager testDataManager;

  @Autowired
  public DatabaseTestDataLoader(@Autowired TestDataManager testDataManager) {
    this.testDataManager = testDataManager;
  }

  public void run(ApplicationArguments args) {
    testDataManager.opprett(
        enJournalpost()
            .leggTilSaksnummer("1900000")
            .medFagomrade(Fagomrade.BIDRAG_DATABASE)
            .medAvsender("Blund, Jon Bovi")
            .medJournalstatus(Journalstatus.RESERVERT)
            .medJournalforendeEnhet("4802")
    );

    testDataManager.opprett(
        enJournalpost()
            .leggTilSaksnummer("1900001")
            .medFagomrade(Fagomrade.BIDRAG_DATABASE)
            .medAvsender("Blund, Jon Bovi")
            .medJournalstatus(Journalstatus.MOTTAKSREGISTRERT)
            .medJournalforendeEnhet("4802")
    );
  }
}
