package no.nav.bidrag.dokument.journalpost.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import no.nav.bidrag.transport.dokument.EndreDokument;
import no.nav.bidrag.transport.dokument.EndreJournalpostCommand;
import no.nav.bidrag.transport.dokument.EndreReturDetaljer;

public class CommandBuilder {

  private boolean skalJournalfores;
  private final List<EndreDokument> endreDokumenter = new ArrayList<>();
  private final List<EndreReturDetaljer> endreReturDetaljer = new ArrayList<>();
  private final List<String> tilknyttSaker = new ArrayList<>();
  private AvsenderMottaker avsenderMottaker;
  private String avsenderNavn;
  private String beskrivelse;
  private String enhet;
  private String gjelder;
  private String journalpostId;
  private LocalDate journaldato;
  private LocalDate dokumentDato;

  public CommandBuilder medDokumenter(EndreDokument... endreDokument) {
    endreDokumenter.addAll(Arrays.asList(endreDokument));

    return this;
  }

  public CommandBuilder medEndreReturDetaljer(EndreReturDetaljer... addEndreReturDetaljer){
    endreReturDetaljer.addAll(Arrays.asList(addEndreReturDetaljer));
    return this;
  }

  public CommandBuilder medGjelder(String gjelder) {
    this.gjelder = gjelder;
    return this;
  }

  public CommandBuilder medSkalJournalfores() {
    skalJournalfores = true;
    return this;
  }

  public CommandBuilder medSkalJournalforesForGjelder() {
    gjelder = "noe";
    return medSkalJournalfores();
  }

  public CommandBuilder medTilknyttSaker(String... saksnummer) {
    tilknyttSaker.addAll(Arrays.asList(saksnummer));
    return this;
  }

  public CommandBuilder medJournalpostId(Integer journalpostId) {
    this.journalpostId = String.valueOf(journalpostId);
    return this;
  }

  public CommandBuilder medAvsenderNavn(String avsenderNavn) {
    this.avsenderNavn = avsenderNavn;
    return this;
  }

  public CommandBuilder medBeskrivelse(String beskrivelse) {
    this.beskrivelse = beskrivelse;
    return this;
  }

  public CommandBuilder medJournaldato(LocalDate journaldato) {
    this.journaldato = journaldato;
    return this;
  }

  public CommandBuilder medEnhet(String enhet) {
    this.enhet = enhet;
    return this;
  }

  public CommandBuilder medDokumentDato(LocalDate dokumentDato) {
    this.dokumentDato = dokumentDato;
    return this;
  }

  public CommandBuilder medAvsender(String etternavn, String fornavn) {
    avsenderMottaker = new AvsenderMottaker(etternavn, fornavn, null);
    return this;
  }

  public EndreJournalpostCommand tilEndreJournalpostCommand() {
    return new EndreJournalpostCommand(
        journalpostId,
        avsenderNavn,
        null,
        null,
        beskrivelse,
        dokumentDato,
        gjelder,
        journaldato,
        tilknyttSaker,
        endreDokumenter, null, null, null, null,
        skalJournalfores, endreReturDetaljer
    );
  }

  public EndreJournalpostCommandIntern tilEndreJournalpostCommandIntern() {
    var command = new EndreJournalpostCommandIntern(
        Integer.parseInt(journalpostId),
        enhet != null ? enhet : "enEnhet",
        tilEndreJournalpostCommand()
    );

    command.setAvsenderMottaker(avsenderMottaker);
    command.setJournalforendeEnhet(enhet);

    return command;
  }

  public static CommandBuilder enCommandBuilder() {
    return new CommandBuilder();
  }
}
