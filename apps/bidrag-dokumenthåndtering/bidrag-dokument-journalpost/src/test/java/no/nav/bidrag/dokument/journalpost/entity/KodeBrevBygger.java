package no.nav.bidrag.dokument.journalpost.entity;

public class KodeBrevBygger {

  private final KodeBrev kodeBrev;

  private KodeBrevBygger(boolean gyldig) {
    kodeBrev = new KodeBrev();
    kodeBrev.erGyldig = gyldig;
  }

  public KodeBrevBygger medKode(String kode) {
    kodeBrev.kode = kode;
    return this;
  }

  public KodeBrevBygger medKravtype(String kravtype) {
    kodeBrev.kravType = kravtype;
    return this;
  }

  public KodeBrevBygger medDekode(String dekode) {
    kodeBrev.dekode = dekode;
    return this;
  }

  public KodeBrev hent() {
    return kodeBrev;
  }

  public static KodeBrevBygger enGyldigBrevkode() {
    return new KodeBrevBygger(true);
  }
}
