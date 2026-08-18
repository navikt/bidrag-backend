package no.nav.bidrag.dokument.journalpost.service;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpost.SECURE_LOGGER;

import java.util.Optional;
import no.nav.bidrag.dokument.journalpost.entity.JournalHendelse;
import no.nav.bidrag.dokument.journalpost.model.JournalHendelseForAvvik;
import no.nav.bidrag.dokument.journalpost.repository.JournalHendelseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HendelseService {

  private static final Logger LOGGER = LoggerFactory.getLogger(HendelseService.class);

  private final JournalHendelseRepository journalHendelseRepository;

  public HendelseService(JournalHendelseRepository journalHendelseRepository) {
    this.journalHendelseRepository = journalHendelseRepository;
  }

  public int lagHendelseFor(JournalHendelseForAvvik journalHendelseForAvvik) {
    var journalHendelse = new JournalHendelse(journalHendelseForAvvik.hentJournalpostId());
    journalHendelse.leggTil(journalHendelseForAvvik);

    SECURE_LOGGER.info("Oppretter ny journalhendelse {}", journalHendelse);
    LOGGER.info("Oppretter ny journalhendelse {}", journalHendelse.getHendelse());
    journalHendelseRepository.save(journalHendelse);
    return journalHendelse.getId();
  }

  public Optional<JournalHendelse> hent(int journalHendelseId) {
    return journalHendelseRepository.findById(journalHendelseId);
  }
}

