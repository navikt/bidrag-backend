package no.nav.bidrag.dokument.journalpost.dto;

import no.nav.bidrag.dokument.journalpost.AvvikshendelseBuilder;
import no.nav.bidrag.dokument.journalpost.model.Avvikstype;
import no.nav.bidrag.dokument.journalpost.model.BehandleAvvikRequest;

public class BehandleAvvikRequestBuilder {

  private final AvvikshendelseBuilder avvikshendelseBuilder = new AvvikshendelseBuilder();

  private BehandleAvvikRequestBuilder(Avvikstype avvikstype) {
    avvikshendelseBuilder.med(avvikstype);
  }

  public BehandleAvvikRequestBuilder medJournalpostId(int journalpostId) {
    avvikshendelseBuilder.medJournalpostId(journalpostId);
    return this;
  }

  public BehandleAvvikRequestBuilder medBeskrivelse(String beskrivelse) {
    avvikshendelseBuilder.medBeskrivelse(beskrivelse);
    return this;
  }

  public BehandleAvvikRequestBuilder medNyttFagomrade(String fagomrade) {
    avvikshendelseBuilder.medNyttFagomrade(fagomrade);
    return this;
  }

  public BehandleAvvikRequestBuilder medSaksnummer(String saksnummer) {
    avvikshendelseBuilder.medSaksnummer(saksnummer);
    return this;
  }

  public BehandleAvvikRequestBuilder somErSendtScanning() {
    avvikshendelseBuilder.somErSendtScanning();
    return this;
  }

  public BehandleAvvikRequestBuilder medGammeltEnhetsnummer(String gammeltEnhetsnummer) {
    avvikshendelseBuilder.medGammeltEnhetsnummer(gammeltEnhetsnummer);
    return this;
  }

  public BehandleAvvikRequestBuilder medNyttEnhetsnummer(String nyttEnhetsnummer) {
    avvikshendelseBuilder.medNyttEnhetsnummer(nyttEnhetsnummer);
    return this;
  }

  public BehandleAvvikRequest bygg() {
    return new BehandleAvvikRequest(avvikshendelseBuilder.byggAvvikshendelseIntern());
  }

  public static BehandleAvvikRequestBuilder enBehandleAvvikRequest(Avvikstype avvikstype) {
    return new BehandleAvvikRequestBuilder(avvikstype);
  }
}
