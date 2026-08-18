package no.nav.bidrag.dokument.journalpost.entity;

public class JournalsakBygger {

  private Journalpost journalpost;
  private String saksnummer;

  public static JournalsakBygger enJournalsak() {
    return new JournalsakBygger();
  }

  public JournalsakBygger medSaksnummer(String saksnummer) {
    this.saksnummer = saksnummer;
    return this;
  }

  public JournalsakBygger med(JournalpostBygger journalpostBygger) {
    journalpost = journalpostBygger.hent();
    return this;
  }

  public JournalsakBygger med(Journalpost journalpost) {
    this.journalpost = journalpost;
    return this;
  }

  public Journalsak bygg() {
    var journalsak = new Journalsak();
    journalsak.saksnummer = saksnummer;

    journalpost.leggTil(journalsak);

    return journalsak;
  }
}
