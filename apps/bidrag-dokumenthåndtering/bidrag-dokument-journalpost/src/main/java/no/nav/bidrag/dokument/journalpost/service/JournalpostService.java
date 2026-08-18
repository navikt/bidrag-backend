package no.nav.bidrag.dokument.journalpost.service;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import jakarta.transaction.Transactional;
import no.nav.bidrag.dokument.journalpost.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.journalpost.dto.JournalpostIntern;
import no.nav.bidrag.dokument.journalpost.dto.JournalpostResponseIntern;
import no.nav.bidrag.dokument.journalpost.dto.Sakjournal;
import no.nav.bidrag.dokument.journalpost.entity.Journalpost;
import no.nav.bidrag.dokument.journalpost.entity.Journalsak;
import no.nav.bidrag.dokument.journalpost.exception.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.journalpost.repository.JournalpostRepository;
import no.nav.bidrag.dokument.journalpost.service.manager.EndreJournalpostManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class JournalpostService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JournalpostService.class);

  private final ApplicationContext applicationContext;
  private final JournalpostRepository journalpostRepository;
  private final KodeService kodeService;
  private final TilgangskontrollService tilgangskontrollService;
  private final SakService sakService;

  public JournalpostService(
      ApplicationContext applicationContext,
      JournalpostRepository journalpostRepository,
      KodeService kodeService,
      TilgangskontrollService tilgangskontrollService, SakService sakService) {
    this.applicationContext = applicationContext;
    this.kodeService = kodeService;
    this.journalpostRepository = journalpostRepository;
    this.tilgangskontrollService = tilgangskontrollService;
    this.sakService = sakService;
  }

  public void lagreJournalpost(Journalpost journalpost){
    journalpostRepository.save(journalpost);
  }
  public Optional<Journalpost> hentJournalpostEntitet(Integer journalpostId) {
    return journalpostRepository.findById(journalpostId);
  }

  public Stream<Journalpost> streamJournalpostFraDatabase(Integer journalpostId) {
    return hentJournalpostEntitet(journalpostId)
        .map(Stream::of)
        .orElseThrow(() -> new JournalpostIkkeFunnetException(String.format("Kunne ikke finne journalpost (%d)", journalpostId)));
  }

  public Journalpost hentJournalpostEntitetForId(Integer journalpostId) {
    return journalpostRepository.findById(journalpostId).orElse(null);
  }

  public Journalpost hentJournalpostForDokumentReferanse(String dokref) {
    return journalpostRepository.findByDokumentreferanse(dokref).orElse(null);
  }

  public JournalpostResponseIntern hentJournalpost(Integer journalpostId) {
    var muligJournalpost = hentJournalpostEntitet(journalpostId);

    return muligJournalpost
        .map(Journalpost::tilJournalpostResponseIntern)
        .map(this::berikBrevkode)
        .orElseGet(JournalpostResponseIntern::new);
  }

  public JournalpostResponseIntern hentJournalpost(String saksnummer, Integer journalpostId) {
    var muligJournalpost = journalpostRepository.findById(journalpostId);

    return muligJournalpost
        .map(jp -> jp.hentJournalsak(saksnummer))
        .map(Journalsak::tilJournalpostResponseIntern)
        .map(this::berikBrevkode)
        .orElseGet(JournalpostResponseIntern::new);
  }

  private JournalpostResponseIntern berikBrevkode(JournalpostResponseIntern journalpostResponseIntern) {
    var brevkode = journalpostResponseIntern.hentBrevkode();

    if (brevkode != null) {
      var muligBrevkode = kodeService.hentBrevKode(brevkode.getKode());

      muligBrevkode.ifPresentOrElse(journalpostResponseIntern::oppdaterMedBrevkode, () -> LOGGER.warn("Brevkoden ({}) er ukjent", brevkode));
    }

    return journalpostResponseIntern;
  }

  @Transactional
  public Optional<JournalpostIntern> endre(EndreJournalpostCommandIntern endreJournalpostCommandIntern) {
    var endreJournalpostManager = applicationContext.getBean(EndreJournalpostManager.class);
    return streamJournalpostFraDatabase(endreJournalpostCommandIntern.getJournalpostId())
        .map(endreJournalpostManager::behandle)
        .peek(manager -> manager.leggTil(endreJournalpostCommandIntern))
        .peek(EndreJournalpostManager::oppdaterBrukerinfoForJournalforing)
        .peek(EndreJournalpostManager::endre)
        .peek(EndreJournalpostManager::lagreEndringMedOpprettedeSaksrelasjoner)
        .peek(EndreJournalpostManager::publishJournalpostHendelse)
        .map(EndreJournalpostManager::mapEndretJournalpostTilInternDto)
        .findFirst();
  }

  public List<JournalpostIntern> hentJournal(Sakjournal sakjournal) {
    var bySaksnummer = sakService.finn(sakjournal.getSaksnummer());
    var journalpostIds = new HashSet<Integer>();

    return bySaksnummer.stream()
        .filter(not(journalsak -> journalpostIds.contains(journalsak.getJournalpost().getJournalpostId())))
        .peek(journalsak -> journalpostIds.add(journalsak.getJournalpost().getJournalpostId()))
        .filter(js -> sakjournal.getFagomrade().stream().anyMatch(js::erFor))
        .filter(js -> tilgangskontrollService.harTilgangTilTema(js.getJournalpost().hentFagomrade()))
        .filter(kodeService::skalViseJournalpost)
        .filter(jsak->!jsak.erArkivertIJoark())
        .map(Journalsak::tilJournalpostIntern)
        .collect(toList());
  }
}
