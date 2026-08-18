package no.nav.bidrag.dokument.journalpost.service;

import java.util.stream.Stream;
import jakarta.transaction.Transactional;
import no.nav.bidrag.dokument.journalpost.entity.Journalpost;
import no.nav.bidrag.dokument.journalpost.exception.JournalpostIkkeFunnetException;
import no.nav.bidrag.dokument.journalpost.exception.UgyldigJournalpostStatus;
import no.nav.bidrag.dokument.journalpost.model.Journalstatus;
import no.nav.bidrag.dokument.journalpost.repository.JournalpostRepository;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DistribuerService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DistribuerService.class);
  private final FeatureService featureService;
  private final JournalpostRepository journalpostRepository;

  public DistribuerService(FeatureService featureService, JournalpostRepository journalpostRepository) {
    this.featureService = featureService;
    this.journalpostRepository = journalpostRepository;
  }

  public Stream<Journalpost> hentJournalpost(Integer journalpostId) {
    return journalpostRepository.findById(journalpostId)
        .map(Stream::of)
        .orElseThrow(() -> new JournalpostIkkeFunnetException(String.format("Kunne ikke finne journalpost (%d)", journalpostId)));
  }

  @Transactional
  public void settStatusEkspedert(Integer journalpostId) {
    var journalpost = hentJournalpost(journalpostId).findFirst().get();
    if (!(journalpost.hasStatusKlarTilPrint() || journalpost.hasStatusEkspedert())){
      throw new UgyldigJournalpostStatus(String.format("Journalpost %s må ha status %s (KLAR_TIL_PRINT) for å kunne sette til %s men hadde status %s", journalpostId, Journalstatus.KLAR_TIL_PRINT, Journalstatus.EKSPEDERT, journalpost.getJournalstatus()));
    }
    journalpost.settStatusEkspedert();
    LOGGER.info("Journalpost {} status satt til {} (EKSPEDERT)", journalpostId, Journalstatus.EKSPEDERT);
  }

  public void kanDistribuereJournalpost(Integer journalpostId, String enhet) {
    var journalpost = hentJournalpost(journalpostId).findFirst().get();
    var antallSaker = journalpost.getJournalsaker().size();
    Validate.isTrue(featureService.kanDistribuereJournalpost(enhet), "Saksbehandler eller enhet må ha tilgang til å distribuere journalpost");
    Validate.isTrue(journalpost.hasStatusKlarTilPrint(), "Journalpost må ha status KP (KLAR_TIL_PRINT)");
    Validate.isTrue(journalpost.erUtgaaende(), "Journalpost må være utgående");
    Validate.isTrue(antallSaker == 1, "Journalpost må totalt ha 1 tilknyttet sak");
    Validate.isTrue(journalpost.harTemaBID(), "Journalpost må ha tema BID");
    Validate.isTrue(journalpost.harSattGjelder(), "Journalpost må ha satt gjelder");
    Validate.isTrue(!journalpost.gjelderErSamhandler(), "Journalpost gjelder kan ikke være samhandler ident");
    Validate.isTrue(journalpost.erMottakerOgGjelderSamme(), "Mottaker og gjelder må være samme");
  }

}
