package no.nav.bidrag.dokument.journalpost.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public class ReturDetaljerLogg {
  String beskrivelse;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
  LocalDate dato;

  public ReturDetaljerLogg() {
    // Required for jackson mapper
  }

  public ReturDetaljerLogg(LocalDate dato, String beskrivelse) {
    this.beskrivelse = beskrivelse;
    this.dato = dato;
  }

  public String getBeskrivelse() {
    return beskrivelse;
  }

  public void setBeskrivelse(String beskrivelse) {
    this.beskrivelse = beskrivelse;
  }

  public LocalDate getDato() {
    return dato;
  }

  public void setDato(LocalDate dato) {
    this.dato = dato;
  }
}
