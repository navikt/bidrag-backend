package no.nav.bidrag.dokument.journalpost.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import no.nav.bidrag.dokument.journalpost.dto.KodeIntern;
import no.nav.bidrag.dokument.journalpost.entity.Journalsak;
import no.nav.bidrag.dokument.journalpost.entity.KodeBrev;
import no.nav.bidrag.dokument.journalpost.entity.KodeJournalstatus;
import no.nav.bidrag.dokument.journalpost.model.Cacheable;
import no.nav.bidrag.dokument.journalpost.repository.KodeBrevReposistory;
import no.nav.bidrag.dokument.journalpost.repository.KodeJournalstatusRepository;
import org.springframework.stereotype.Service;

@Service
public class KodeService {

  private final KodeBrevReposistory kodeBrevReposistory;
  private final KodeJournalstatusRepository kodeJournalstatusRepository;
  private final Map<String, Cacheable<Optional<KodeBrev>>> brevkoder = new HashMap<>();
  private final Map<String, Cacheable<Optional<KodeJournalstatus>>> cachedKodeJournalstatus = new HashMap<>();

  public KodeService(KodeBrevReposistory kodeBrevReposistory, KodeJournalstatusRepository kodeJournalstatusRepository) {
    this.kodeBrevReposistory = kodeBrevReposistory;
    this.kodeJournalstatusRepository = kodeJournalstatusRepository;
  }

  public Optional<KodeIntern> hentBrevKode(String kode) {
    if (!brevkoder.containsKey(kode)) {
      brevkoder.put(kode, new Cacheable<>(Duration.ofHours(12)));
    }

    Cacheable<Optional<KodeBrev>> cached = brevkoder.get(kode);
    return cached.fetchOrRenew(() -> kodeBrevReposistory.findByKode(kode))
        .map(KodeBrev::tilKodeIntern);
  }

  public boolean skalViseJournalpost(Journalsak journalsak) {
    String journalstatus = journalsak.hentJournalpostensJournalstatus();

    return skalVise(journalstatus);
  }

  public boolean skalVise(String journalstatus) {
    if (journalstatus == null) {
      return false;
    }

    if (!cachedKodeJournalstatus.containsKey(journalstatus)) {
      cachedKodeJournalstatus.put(journalstatus, new Cacheable<>(Duration.ofHours(12)));
    }

    var cached = cachedKodeJournalstatus.get(journalstatus);
    var muligKode = cached.fetchOrRenew(() -> kodeJournalstatusRepository.findById(journalstatus));
    return muligKode.map(KodeJournalstatus::skalVises).orElse(false);
  }
}
