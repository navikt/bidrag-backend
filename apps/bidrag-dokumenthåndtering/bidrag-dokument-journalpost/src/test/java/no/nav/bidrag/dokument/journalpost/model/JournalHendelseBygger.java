package no.nav.bidrag.dokument.journalpost.model;

import no.nav.bidrag.dokument.journalpost.AvvikshendelseBuilder;
import no.nav.bidrag.dokument.journalpost.entity.Journalpost;

public class JournalHendelseBygger {

  private AvvikshendelseBuilder avvikshendelseBuilder;
  private Journalpost journalpost;
  private String brukerident;

  public JournalHendelseBygger med(Journalpost journalpost) {
    this.journalpost = journalpost;
    return this;
  }

  public JournalHendelseBygger med(AvvikshendelseBuilder avvikshendelseBuilder) {
    this.avvikshendelseBuilder = avvikshendelseBuilder;
    return this;
  }

  public JournalHendelseBygger medBrukerident(String brukerident) {
    this.brukerident = brukerident;
    return this;
  }

  public JournalHendelseForAvvik bygg() {
    var avvikshendelseIntern = avvikshendelseBuilder.byggAvvikshendelseIntern();

    journalpost.leggTilHendelseData(avvikshendelseIntern);

    return new JournalHendelseForAvvik(
        avvikshendelseIntern,
        journalpost.getHendelseData(),
        brukerident != null ? brukerident : "enhetstester",
        null
    );
  }

  public static JournalHendelseBygger enJournalHendelse() {
    return new JournalHendelseBygger();
  }
}
