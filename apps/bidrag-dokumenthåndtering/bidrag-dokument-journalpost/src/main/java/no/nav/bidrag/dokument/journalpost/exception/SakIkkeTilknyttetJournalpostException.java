package no.nav.bidrag.dokument.journalpost.exception;

public class SakIkkeTilknyttetJournalpostException extends RuntimeException {

  public SakIkkeTilknyttetJournalpostException(String saksnr) {
    super("Sak med saksnummer " + saksnr + " mangler kobling til oppgitt journalpost");
  }

}
