package no.nav.bidrag.dokument.journalpost;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import no.nav.bidrag.dokument.journalpost.entity.FeatureAccess;
import no.nav.bidrag.dokument.journalpost.entity.JournalHendelse;
import no.nav.bidrag.dokument.journalpost.entity.Journalpost;
import no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger;
import no.nav.bidrag.dokument.journalpost.entity.Journalsak;
import no.nav.bidrag.dokument.journalpost.entity.KodeBrevBygger;
import no.nav.bidrag.dokument.journalpost.entity.KodeJournalstatus;
import no.nav.bidrag.dokument.journalpost.repository.FeatureAccessRepository;
import no.nav.bidrag.dokument.journalpost.repository.JournalHendelseRepository;
import no.nav.bidrag.dokument.journalpost.repository.JournalpostRepository;
import no.nav.bidrag.dokument.journalpost.repository.JournalsakReposistory;
import no.nav.bidrag.dokument.journalpost.repository.KodeBrevReposistory;
import no.nav.bidrag.dokument.journalpost.repository.KodeJournalstatusRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile(value = {BidragDokumentJournalpostProfiles.TEST, BidragDokumentJournalpostProfiles.LOCAL})
public class TestDataManager {

  private final EntityManager entityManager;
  private final KodeJournalstatusRepository kodeJournalstatusRepository;
  private final JournalHendelseRepository journalHendelseRepository;
  private final JournalpostRepository journalpostRepository;
  private final JournalsakReposistory journalsakReposistory;
  private final KodeBrevReposistory kodeBrevReposistory;
  private final FeatureAccessRepository featureAccessRepository;

  public TestDataManager(
      EntityManager entityManager,
      KodeJournalstatusRepository kodeJournalstatusRepository,
      JournalHendelseRepository journalHendelseRepository,
      JournalpostRepository journalpostRepository,
      JournalsakReposistory journalsakReposistory,
      KodeBrevReposistory kodeBrevReposistory, FeatureAccessRepository featureAccessRepository) {
    this.entityManager = entityManager;
    this.kodeJournalstatusRepository = kodeJournalstatusRepository;
    this.journalHendelseRepository = journalHendelseRepository;
    this.journalpostRepository = journalpostRepository;
    this.journalsakReposistory = journalsakReposistory;
    this.kodeBrevReposistory = kodeBrevReposistory;
    this.featureAccessRepository = featureAccessRepository;
  }

  @Transactional
  public FeatureAccess opprettFeatureAccess(FeatureAccess featureAccess) {
    return featureAccessRepository.save(featureAccess);
  }

  @Transactional
  public Journalpost opprett(JournalpostBygger journalpostBygger) {
    journalpostBygger.settManglendeSaksnummer();
    return opprett(journalpostBygger.hent());
  }

  @Transactional
  public void opprett(JournalpostBygger journalpostBygger, LocalDate skannetDato) {
    Journalpost journalpost = journalpostBygger.hent();
    journalpost.setSkannetDato(skannetDato);
    opprett(journalpost);
  }

  @Transactional
  public Journalpost opprett(Journalpost journalpost) {
    if (!journalpost.getJournalsaker().isEmpty()) {
      for (Journalpost journalpostFraDb : journalpostRepository.findAll()) {
        var saksnummerSomJournalpostOpprettesPa = journalpost.getJournalsaker().iterator().next().getSaksnummer();
        var eksisterendeJournalpost = journalpost.getAvsender() != null && journalpost.getAvsender().equals(journalpostFraDb.getAvsender()) &&
            journalpostFraDb.getJournalsaker().stream()
                .map(Journalsak::getSaksnummer)
                .anyMatch(saksnummer -> saksnummer.equals(saksnummerSomJournalpostOpprettesPa));

        if (eksisterendeJournalpost) {
          return journalpostFraDb;
        }
      }
    }

    entityManager.persist(journalpost);
    enforceReadFromDatabase();

    return journalpost;
  }

  @Transactional
  public void opprettJournalsakForSaksnummer(String saksnummer) {
    List<Journalsak> journalsaker = journalsakReposistory.findBySaksnummer(saksnummer);

    if (journalsaker.isEmpty()) {
      opprettJournalsak(saksnummer);
    }
  }

  private void opprettJournalsak(String saksnummer) {
    opprett(JournalpostBygger.enJournalpost().leggTilSaksnummer(saksnummer));
  }

  @Transactional
  public void opprett(KodeBrevBygger kodeBrevBygger) {
    kodeBrevReposistory.findByKode(kodeBrevBygger.hent().tilKodeIntern().getKode())
        .orElseGet(()->{
      entityManager.persist(kodeBrevBygger.hent());
      enforceReadFromDatabase();
      return null;
    });

  }

  public void enforceReadFromDatabase() {
    entityManager.flush();
    entityManager.clear();
  }

  public List<JournalHendelse> lesJournalHendelser(Integer journalpostId) {
    return journalHendelseRepository.findByJournalpostId(journalpostId);
  }

  public void slettJournalhendelser() {
    journalHendelseRepository.deleteAll();
  }

  public void opprettKodeForJournalstatusSomIkkeSkalVises(String... journalstatuser) {
    if (journalstatuser != null) {
      for (String journalstatus : journalstatuser) {
        kodeJournalstatusRepository.save(new KodeJournalstatus(journalstatus, false));
      }
    }
  }

  public void opprettKodeForJournalstatusSomSkalVises(String journalstatus) {
    kodeJournalstatusRepository.save(new KodeJournalstatus(journalstatus, true));
  }

  @Transactional
  @SuppressWarnings("unchecked")
  public <T> Optional<T> hent(Integer journalpostId, @SuppressWarnings("unused") Class<T> clazz) {
    enforceReadFromDatabase();
    return (Optional<T>) journalpostRepository.findById(journalpostId);
  }

  @Transactional
  public void slettAlt(){
    enforceReadFromDatabase();
    journalsakReposistory.deleteAll();
    journalpostRepository.deleteAll();
    enforceReadFromDatabase();

  }

  public no.nav.bidrag.transport.dokument.ReturDetaljerLog findReturDetaljerLogByDate(LocalDate date, List<no.nav.bidrag.transport.dokument.ReturDetaljerLog> returDetaljerLogs){
    return returDetaljerLogs.stream().filter(it->it.getDato().equals(date)).findFirst().orElse(null);
  }
}
