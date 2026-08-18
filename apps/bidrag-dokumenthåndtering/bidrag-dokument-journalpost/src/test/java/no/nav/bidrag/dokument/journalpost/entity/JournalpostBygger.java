package no.nav.bidrag.dokument.journalpost.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import no.nav.bidrag.dokument.journalpost.model.Journalstatus;

public class JournalpostBygger {

  private boolean utenSak = false;
  private final Journalpost journalpost = new Journalpost();
  private final List<Journalsak> journalsaker = new ArrayList<>();

  private JournalpostBygger() {
  }

  private JournalpostBygger(String journalstatus) {
    this(null, journalstatus);
  }

  private JournalpostBygger(Journalsak journalsak, String journalstatus) {
    if (journalsak != null && journalsak.saksnummer != null) {
      journalsaker.add(journalsak);
    }

    journalpost.journalstatus = journalstatus;
  }

  public JournalpostBygger medJournaldato(LocalDate journaldato) {
    journalpost.journaldato = journaldato;
    return this;
  }

  public Optional<Journalpost> hentMuligJournalpost() {
    return Optional.of(hent());
  }

  public JournalpostBygger utenOrginalBestilt() {
    journalpost.originalBestilt = false;
    return this;
  }

  public JournalpostBygger medOrginalBestilt() {
    journalpost.originalBestilt = true;
    return this;
  }

  public JournalpostBygger medAvsender(String avsender) {
    journalpost.lagAvsenderNavn(avsender);
    return this;
  }

  public JournalpostBygger leggTilSaksnummerArkivertIJoark(String saksnummer, Integer joarkJournalpostId) {
    var sak = new Journalsak(saksnummer);
    sak.arkiveringFullfort = LocalDateTime.parse("2021-12-08T11:42:07.503433");
    sak.joarkJpId = joarkJournalpostId;
    journalsaker.add(sak);
    return this;
  }

  public JournalpostBygger leggTilSaksnummer(String saksnummer) {
    journalsaker.add(new Journalsak(saksnummer));
    return this;
  }

  public JournalpostBygger medFagomrade(String fagomrade) {
    journalpost.fagomrade = fagomrade;
    return this;
  }

  public JournalpostBygger medBeskrivelse(String beskrivelse) {
    journalpost.beskrivelse = beskrivelse;
    return this;
  }

  public JournalpostBygger medDokumentdato(LocalDate dokumentdato) {
    journalpost.dokumentdato = dokumentdato;
    return this;
  }

  public JournalpostBygger medDokumentreferanse(String dokumentreferanse) {
    journalpost.dokumentreferanse = dokumentreferanse;
    return this;
  }

  public JournalpostBygger medDokumentType(String dokumenttype) {
    journalpost.dokumentType = dokumenttype;
    return this;
  }

  public JournalpostBygger medGjelder(String gjelder) {
    journalpost.gjelder = gjelder;
    return this;
  }

  public JournalpostBygger medMottaker(String mottaker) {
    journalpost.mottakerId = mottaker;
    return this;
  }


  public JournalpostBygger medJournalfortAv(String journalfoertAv) {
    journalpost.journalfortAv = journalfoertAv;
    return this;
  }

  public JournalpostBygger medJournalforendeEnhet(String journalforendeEnhet) {
    journalpost.journalforendeEnhet = journalforendeEnhet;
    return this;
  }

  public JournalpostBygger medSkannetDato(LocalDate skannetDato) {
    journalpost.skannetDato = skannetDato;
    return this;
  }

  public JournalpostBygger medBatchNavn(String batchNavn) {
    journalpost.batchNavn = batchNavn;
    return this;
  }

  public JournalpostBygger medJournalpostId(int journalpostId) {
    journalpost.journalpostId = journalpostId;
    return this;
  }

  public JournalpostBygger medFilnavn(String filnavn) {
    journalpost.filnavn = filnavn;
    return this;
  }

  public JournalpostBygger medBrevkode(String brevkode) {
    journalpost.brevkode = brevkode;
    return this;
  }

  public JournalpostBygger medReturDato(LocalDate returDato){
    journalpost.oppdaterReturDato(returDato);
    return this;
  }

  public JournalpostBygger medRetur(LocalDate returDato, ReturDetaljerLogg returDetaljerLoggList){
    medReturDato(returDato);
    journalpost.leggTilReturDetaljerLogg(returDetaljerLoggList);
    return this;
  }

  public JournalpostBygger medJournalstatus(String journalstatus) {
    journalpost.journalstatus = journalstatus;
    return this;
  }

  public JournalpostBygger utenSak() {
    utenSak = true;
    return this;
  }

  public void settManglendeSaksnummer() {
    if (journalsaker.isEmpty()) {
      journalsaker.add(new Journalsak("1900001"));
    }
  }

  public Journalpost hent() {
    if (!utenSak) {
      journalsaker.forEach(journalpost::leggTil);
    }

    return journalpost;
  }

  public static JournalpostBygger enJournalpost() {
    return new JournalpostBygger();
  }

  public static JournalpostBygger enUtgaendeJournalpostKlarTilPrint() {
    return new JournalpostBygger(Journalstatus.KLAR_TIL_PRINT).medDokumentType("U");
  }

  public static JournalpostBygger enJournalfortJournalpost() {
    return new JournalpostBygger(Journalstatus.JOURNALFORT);
  }

  public static JournalpostBygger enMottaksregistrertJournalpost() {
    return new JournalpostBygger(Journalstatus.MOTTAKSREGISTRERT);
  }

  public static JournalpostBygger enJournalpostSomErFeilfort(String saksnummer) {
    Journalsak journalsak = new Journalsak();
    journalsak.setFeilfort();
    journalsak.saksnummer = saksnummer;

    return new JournalpostBygger(journalsak, "J");
  }
}
