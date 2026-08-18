package no.nav.bidrag.dokument.journalpost.entity;

import static java.util.function.Predicate.not;
import static no.nav.bidrag.commons.util.KildesystemIdenfikator.PREFIX_BIDRAG_COMPLETE;
import static no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.ENHETSNUMMER;
import static no.nav.bidrag.dokument.journalpost.model.Avvikstype.BESTILL_ORIGINAL;
import static no.nav.bidrag.dokument.journalpost.model.Avvikstype.BESTILL_RESKANNING;
import static no.nav.bidrag.dokument.journalpost.model.Avvikstype.BESTILL_SPLITTING;
import static no.nav.bidrag.dokument.journalpost.model.Avvikstype.ENDRE_FAGOMRADE;
import static no.nav.bidrag.dokument.journalpost.model.Avvikstype.FARSKAP_UTELUKKET;
import static no.nav.bidrag.dokument.journalpost.model.Avvikstype.FEILFORE_SAK;
import static no.nav.bidrag.dokument.journalpost.model.Avvikstype.INNG_TIL_UTG_DOKUMENT;
import static no.nav.bidrag.dokument.journalpost.model.Avvikstype.OVERFOR_TIL_ANNEN_ENHET;
import static no.nav.bidrag.dokument.journalpost.model.Avvikstype.REGISTRER_RETUR;
import static no.nav.bidrag.dokument.journalpost.model.Avvikstype.SEND_TIL_FAGOMRADE;
import static no.nav.bidrag.dokument.journalpost.model.Avvikstype.SLETT_JOURNALPOST;
import static no.nav.bidrag.dokument.journalpost.model.Avvikstype.TREKK_JOURNALPOST;
import static no.nav.bidrag.dokument.journalpost.model.BehandleKt.initGyldigAvviksbehandling;
import static no.nav.bidrag.dokument.journalpost.model.ConstantsKt.BATCH_NAVN_JOARK;
import static no.nav.bidrag.dokument.journalpost.model.ConstantsKt.JP_ARKIVDEL;
import static no.nav.bidrag.dokument.journalpost.model.ConstantsKt.JP_SYSTEMID_BISYS;

import com.fasterxml.jackson.core.type.TypeReference;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import no.nav.bidrag.commons.CorrelationId;
import no.nav.bidrag.transport.dokument.ConstantsKt;
import no.nav.bidrag.transport.dokument.HendelseType;
import no.nav.bidrag.transport.dokument.JournalpostHendelse;
import no.nav.bidrag.transport.dokument.JournalpostStatus;
import no.nav.bidrag.transport.dokument.Sporingsdata;
import no.nav.bidrag.dokument.journalpost.dto.AktorIntern;
import no.nav.bidrag.dokument.journalpost.dto.AvsenderMottaker;
import no.nav.bidrag.dokument.journalpost.dto.AvvikshendelseIntern;
import no.nav.bidrag.dokument.journalpost.dto.BestillOriginalOppgave;
import no.nav.bidrag.dokument.journalpost.dto.BestillReskanningOppgave;
import no.nav.bidrag.dokument.journalpost.dto.BestillSplittingOppgave;
import no.nav.bidrag.dokument.journalpost.dto.DokumentIntern;
import no.nav.bidrag.dokument.journalpost.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.journalpost.dto.JournalpostIntern;
import no.nav.bidrag.dokument.journalpost.dto.JournalpostResponseIntern;
import no.nav.bidrag.dokument.journalpost.dto.KodeIntern;
import no.nav.bidrag.dokument.journalpost.dto.OpprettUtgaaendeJournalpostIntern;
import no.nav.bidrag.dokument.journalpost.exception.SakIkkeTilknyttetJournalpostException;
import no.nav.bidrag.dokument.journalpost.exception.SaksnummerManglerException;
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer;
import no.nav.bidrag.dokument.journalpost.model.Avviksbehandling;
import no.nav.bidrag.dokument.journalpost.model.Avvikstype;
import no.nav.bidrag.dokument.journalpost.model.Dokstatus;
import no.nav.bidrag.dokument.journalpost.model.DokumentType;
import no.nav.bidrag.dokument.journalpost.model.Fagomrade;
import no.nav.bidrag.dokument.journalpost.model.FagomradeKt;
import no.nav.bidrag.dokument.journalpost.model.HendelseData;
import no.nav.bidrag.dokument.journalpost.model.JoarkArkiveringStatus;
import no.nav.bidrag.dokument.journalpost.model.Journalstatus;
import no.nav.bidrag.dokument.journalpost.model.UgyldigAvviksbehandling;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "T_JP")
@SuppressWarnings("JpaDataSourceORMInspection")
public class Journalpost {

  private static final Logger LOGGER = LoggerFactory.getLogger(Journalpost.class);
  private static final String BLANK_STRENG = "";
  private static final String SAKSNUMMER_UKJENT_SAMT_REELLE_SAKSNUMMER =
      "Saksnummer (%s) er ukjent. relaterer følgende saker: %s";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "JP_ID")
  Integer journalpostId;

  @OneToMany(mappedBy = "journalpost", fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
  @SuppressWarnings("FieldMayBeFinal")
  private List<Journalsak> journalsaker = new ArrayList<>();

  @Column(name = "ARKIVTIDSPUNKT")
  private LocalDateTime arkiveringstidspunkt;

  @Column(name = "BRUKERID")
  String brukerid;

  @Column(name = "MOTTAKER_ID")
  String mottakerId;

  @Column(name = "ADM_ENHET")
  String journalforendeEnhet;

  @Column(name = "ENH_NAVN")
  String journalforendeEnhetNavn;

  @Column(name = "DOK_TYPE")
  String dokumentType;

  @Column(name = "SYSTEM_ID")
  String systemId;

  @Column(name = "tjeneste")
  String tjeneste;

  @Column(name = "kravtype")
  String kravtype;

  @Column(name = "ARKIVDEL")
  String arkivdel;

  @Column(name = "DOK_REF")
  String dokumentreferanse;

  @Column(name = "DOK_DATO")
  LocalDate dokumentdato;

  @Column(name = "EKST_NAVN")
  private String avsender;

  @Column(name = "EKST_FORNAVN")
  private String avsenderFornavn;

  @Column(name = "FAGOMR")
  String fagomrade;

  @Column(name = "GJELDER")
  String gjelder;

  @Column(name = "INNHOLD")
  String beskrivelse;

  @Column(name = "JOURNAL_DATO")
  LocalDate journaldato;

  @Column(name = "SAKS_BEH_NAVN")
  String journalfortAv;

  @Column(name = "SKANNET_DATO")
  LocalDate skannetDato;

  @Column(name = "BATCH_NAVN")
  String batchNavn;

  @Column(name = "JSTATUS")
  String journalstatus;

  @Column(name = "DOK_STATUS")
  String dokstatus;

  @Column(name = "ORIGINAL_DOK_REF")
  String dokumentreferanseVedJournalforing;

  @Column(name = "ORIG_BESTILT")
  boolean originalBestilt;

  @Column(name = "FIL_NAVN")
  String filnavn;

  @Column(name = "KODE_BREV")
  String brevkode;

  @Column(name = "ant_retur")
  Integer antallRetur;

  @Column(name = "avs_retur_dato")
  LocalDate returDato;

  @Lob
  @Column(name = "retur_detaljer_logg", columnDefinition = "CLOB")
  private String returDetaljerLoggJson;

  @Column(name = "ER_PAPIR")
  @SuppressWarnings("FieldMayBeFinal")
  private boolean erPapir = false; // not null og ikke i bruk...

  @Column(name = "ORIG_RETUR")
  @SuppressWarnings("FieldMayBeFinal")
  private boolean originalReturnert = false; // not null og ikke i bruk...

  @Column(name = "renset_dok_ref", length = 60, nullable = true)
  private String rensetDokRef;

  @Column(name = "dok_status_sjekket", nullable = true)
  private LocalDateTime dokStatusSjekket;

  @Transient private HendelseData hendelseData;

  public void leggTilDokumentreferanse() {
    String timestamp = StringUtils.right(LocalDateTime.now().toString(), 2);
    this.dokumentreferanse = journalpostId.toString() + timestamp;
  }

  public void oppdaterReturDato(LocalDate nyReturDato) {
    if (!Objects.isNull(returDato) && returDato.isEqual(nyReturDato)) {
      return;
    }

    if (Objects.isNull(antallRetur)) {
      antallRetur = 0;
    }

    returDato = nyReturDato;
    antallRetur = antallRetur + 1;
  }

  public List<ReturDetaljerLogg> getReturDetaljerLogg() {
    if (Strings.isEmpty(returDetaljerLoggJson)) {
      return new ArrayList<>();
    }

    return JsonMapper.fromJsonString(returDetaljerLoggJson, new TypeReference<>() {});
  }

  public void leggTilReturDetaljerLogg(ReturDetaljerLogg returDetaljerLogg) {
    var returDetaljerLogList = this.getReturDetaljerLogg();
    returDetaljerLogList.add(returDetaljerLogg);
    this.oppdaterReturDetaljerLoggJson(returDetaljerLogList);
  }

  public boolean harReturDetaljerLoggMedDato(String date) {
    return harReturDetaljerLoggMedDato(LocalDate.parse(date));
  }

  public boolean harReturDetaljerLoggMedDato(LocalDate date) {
    return this.getReturDetaljerLogg().stream().anyMatch(it -> it.dato.equals(date));
  }

  private void oppdaterReturDetaljerLogg(LocalDate dato, ReturDetaljerLogg returDetaljerLogg) {
    var returDetaljerLogList =
        this.getReturDetaljerLogg().stream()
            .map(it -> it.dato.equals(dato) ? returDetaljerLogg : it)
            .collect(Collectors.toList());
    this.oppdaterReturDetaljerLoggJson(returDetaljerLogList);
  }

  private void oppdaterReturDetaljerLoggJson(List<ReturDetaljerLogg> returDetaljerLoggList) {
    returDetaljerLoggJson = JsonMapper.toJsonString(returDetaljerLoggList);
  }

  public Journalpost() {
    // empty constructor to be used by entity manager....
  }

  void lagAvsenderNavn(String avsender) {
    if (avsender != null) {
      AvsenderMottaker avs = new AvsenderMottaker(avsender, null);
      this.avsender = avs.hentEtternavn();
      this.avsenderFornavn = avs.hentFornavn();
    }
  }

  public Journalpost opprett(OpprettUtgaaendeJournalpostIntern opprett) {
    brukerid = EntityUtils.truncateBrukerId(opprett.getOpprettetAvId(), 20);
    beskrivelse = opprett.getDokumenttittel();
    gjelder = opprett.getGjelderId();
    fagomrade = opprett.getFagomrade();
    dokumentreferanse = opprett.getDokumentreferanse();
    filnavn = opprett.getReferanseId();
    journalfortAv = opprett.getOpprettetAvNavn();
    journalstatus = opprett.getJournalstatus();
    brevkode = opprett.getBrevkode();
    dokumentType = opprett.getDokumentType();
    journalforendeEnhet = opprett.getJournalforendeEnhet();
    journalforendeEnhetNavn = opprett.getJournalforendeEnhetNavn();
    kravtype = opprett.getKravtype();
    if (opprett.harMottaker() && !opprett.erNotat()) {
      mottakerId = opprett.hentMottakerId();
      avsender = opprett.hentEtternavn();
      avsenderFornavn = opprett.hentFornavn();
    }

    arkiveringstidspunkt = LocalDateTime.now();
    dokumentdato = LocalDate.now();
    journaldato = LocalDate.now();

    dokstatus = Dokstatus.DOKBESKRIVELSE_STATUS;
    systemId = JP_SYSTEMID_BISYS;
    arkivdel = JP_ARKIVDEL;
    tjeneste = "AN";

    return this;
  }

  public Journalpost endre(EndreJournalpostCommandIntern endreJournalpostCommandIntern) {
    endreJournalpostCommandIntern.sjekkGyldigEndring(this);

    if (endreJournalpostCommandIntern.harAvsender()) {
      avsender = endreJournalpostCommandIntern.hentEtternavn();
      avsenderFornavn = endreJournalpostCommandIntern.hentFornavn();
    }

    beskrivelse = nullNarBlank(endreJournalpostCommandIntern.hentBeskrivelse(), beskrivelse);
    brevkode = nullNarBlank(endreJournalpostCommandIntern.getBrevkode(), brevkode);
    dokumentdato = whenNotNull(endreJournalpostCommandIntern.getDokumentDato(), dokumentdato);
    gjelder = nullNarBlank(endreJournalpostCommandIntern.getGjelder(), gjelder);
    journaldato = whenNotNull(endreJournalpostCommandIntern.getJournaldato(), journaldato);
    journalforendeEnhet = endreJournalpostCommandIntern.getJournalforendeEnhet();

    if (endreJournalpostCommandIntern.getSkalJournalfores()) {
      journalstatus = Journalstatus.JOURNALFORT;
      brukerid = EntityUtils.truncateBrukerId(endreJournalpostCommandIntern.getBrukerId(), 20);
      journalfortAv = endreJournalpostCommandIntern.getJournalfortAv();
    }
    if (endreJournalpostCommandIntern.hasEndreReturDetaljer()) {
      endreJournalpostCommandIntern
          .getEndreReturDetaljer()
          .forEach(
              it ->
                  oppdaterReturDetaljerLogg(
                      it.getOriginalDato(),
                      new ReturDetaljerLogg(it.getGjeldendeDato(), it.getBeskrivelse())));
    }

    return this;
  }

  private String nullNarBlank(String string) {
    return BLANK_STRENG.equals(string) ? null : string;
  }

  private String nullNarBlank(String streng, String defaultVerdi) {
    String muligVerdi = nullNarBlank(streng);

    if (muligVerdi != null) {
      return muligVerdi;
    }

    return defaultVerdi;
  }

  private <T> T whenNotNull(T value, T defaultValue) {
    if (value == null) {
      return defaultValue;
    }

    return value;
  }

  public void leggTil(Journalsak journalsak) {
    journalsak.setJournalpost(this);
    journalsaker.add(journalsak);
  }

  JournalpostIntern tilJournalpostIntern(boolean feilfort) {
    if (journalsaker.isEmpty()) {
      throw new IllegalStateException(
          String.format("Journalpost med id %d har ingen saker koblet til seg", journalpostId));
    }

    var journalpost = tilJournalpostIntern();
    journalpost.setFeilfort(feilfort);

    return journalpost;
  }

  public JournalpostIntern tilJournalpostIntern() {
    JournalpostIntern journalpostIntern = new JournalpostIntern();
    journalpostIntern.setAvsenderMottaker(
        avsender != null || avsenderFornavn != null
            ? new AvsenderMottaker(avsender, avsenderFornavn, mottakerId)
            : null);
    journalpostIntern.setBatchNavn(batchNavn);
    journalpostIntern.setBrukerid(brukerid);
    journalpostIntern.setMottakerId(mottakerId);
    journalpostIntern.setBrevkode(brevkode != null ? new KodeIntern(brevkode) : null);
    journalpostIntern.setDokumentDato(dokumentdato);
    journalpostIntern.setDokumenter(initDokumenterForDokumentreferanser());
    journalpostIntern.setDokumentType(dokumentType);
    journalpostIntern.setFagomrade(FagomradeKt.fraDatabase(fagomrade));
    journalpostIntern.setGjelderAktor(gjelder != null ? new AktorIntern(gjelder) : null);
    journalpostIntern.setInnhold(blankNarNull(beskrivelse));
    journalpostIntern.setJournalforendeEnhet(blankNarNull(journalforendeEnhet));
    journalpostIntern.setJournalfortAv(blankNarNull(journalfortAv));
    journalpostIntern.setJournalfortDato(journaldato);
    journalpostIntern.setJournalpostId(PREFIX_BIDRAG_COMPLETE + journalpostId);
    journalpostIntern.setJournalstatus(journalstatus);
    journalpostIntern.setMottattDato(journaldato != null ? journaldato : skannetDato);
    journalpostIntern.setReturDato(returDato);
    journalpostIntern.setAntallRetur(antallRetur);
    journalpostIntern.setReturDetaljerLog(this.getReturDetaljerLogg());
    journalpostIntern.setJoarkJournalpostId(getJoarkJournalpostid());

    journalsaker.forEach(
        js -> {
          if (js.getSaksnummer() != null) {
            journalpostIntern.leggTilSaksnummer(js.getSaksnummer());
          }
        });

    return journalpostIntern;
  }

  public String getJoarkJournalpostid() {
    if (journalsaker == null || journalsaker.isEmpty()) {
      return null;
    }
    var joarkJpId = journalsaker.get(0).joarkJpId;
    return joarkJpId == null ? null : joarkJpId.toString();
  }

  public JournalpostResponseIntern tilJournalpostResponseIntern() {
    return new JournalpostResponseIntern(
        tilJournalpostIntern(),
        journalsaker.stream().map(Journalsak::getSaksnummer).collect(Collectors.toList()));
  }

  private String blankNarNull(String streng) {
    return Optional.ofNullable(streng).orElse(BLANK_STRENG);
  }

  private List<DokumentIntern> initDokumenterForDokumentreferanser() {
    if (dokumentreferanseVedJournalforing == null
        || dokumentreferanseVedJournalforing.equals(dokumentreferanse)) {
      return Collections.singletonList(initDokument(dokumentreferanse, beskrivelse));
    }

    return List.of(
        initDokument(dokumentreferanse, beskrivelse),
        initDokument(dokumentreferanseVedJournalforing, "Dokumentreferanse ved journalføring"));
  }

  private DokumentIntern initDokument(String dokumentreferanse) {
    return initDokument(dokumentreferanse, BLANK_STRENG);
  }

  private DokumentIntern initDokument(String dokumentreferanse, String tittel) {
    DokumentIntern dokumentIntern = new DokumentIntern();
    dokumentIntern.setDokumentreferanse(dokumentreferanse);
    dokumentIntern.setDokumentType(dokumentType);
    dokumentIntern.setTittel(tittel);

    return dokumentIntern;
  }

  boolean erFor(String fagomrade) {
    return FagomradeKt.erFor(fagomrade, this.fagomrade);
  }

  public List<Avvikstype> finnAvvikForSaksnummer(@NotNull String saksnummer) {
    if (erIkkeForSaksnummer(saksnummer)) {
      var reelleSaksnummer =
          journalsaker.stream().map(Journalsak::getSaksnummer).collect(Collectors.joining(", "));
      LOGGER.warn(
          String.format(SAKSNUMMER_UKJENT_SAMT_REELLE_SAKSNUMMER, saksnummer, reelleSaksnummer));

      return Collections.emptyList();
    }

    return finnAvvik(saksnummer);
  }

  private boolean erIkkeForSaksnummer(String saksnummer) {
    return journalsaker.stream().noneMatch(journalsak -> journalsak.erForSaksnummer(saksnummer));
  }

  public List<Avvikstype> finnAvvik() {
    return finnAvvik(null);
  }

  public List<Avvikstype> finnAvvik(String saksnummer) {
    if (erSlettet()) {
      LOGGER.warn(
          "Journalpost med id {} er slettet! Ingen avvikstyper vil bli returnert!!!",
          journalpostId);

      return Collections.emptyList();
    }

    List<Avvikstype> listeMedAvvik = new ArrayList<>();

    if (erInngaendeSkannetDokumentSomIkkeErElektroniskInnsendtOgIkkeHarOriginalBestilt()) {
      listeMedAvvik.add(BESTILL_ORIGINAL);
    }

    if (erInngaendeSkannetDokumentSomIkkeErElektronisk()) {
      listeMedAvvik.add(BESTILL_RESKANNING);
    }

    if (erInngaendeSkannetDokumentMedFilnavnIkkeOpprettetAvJoarkBatch()) {
      listeMedAvvik.add(BESTILL_SPLITTING);
    }

    if (erInngaendeDokument()) {
      listeMedAvvik.add(INNG_TIL_UTG_DOKUMENT);
    }

    if (erIkkeJournalstatusForMottattOgHarSakSomIkkeErFeilfort(saksnummer)) {
      listeMedAvvik.add(FEILFORE_SAK);
    }

    if (!erFarskapUtelukket()){
      listeMedAvvik.add(ENDRE_FAGOMRADE);
    }

    if (erJournalstatusForUnderProduksjon()) {
      listeMedAvvik.add(SLETT_JOURNALPOST);
    }

    if (erJournalstatusMottaksregistrert()) {
      listeMedAvvik.add(TREKK_JOURNALPOST);
    }

    if (erJournalstatusMottaksregistrert() && erInngaendeDokument()) {
      listeMedAvvik.add(OVERFOR_TIL_ANNEN_ENHET);
    }

    if (erDokumentTypeUtgaaende() && erGyldigJournalstatusForRegistrerRetur()) {
      listeMedAvvik.add(REGISTRER_RETUR);
    }

    if (erInngaendeDokument() && erJournalstatusJournalfort()) {
      listeMedAvvik.add(SEND_TIL_FAGOMRADE);
    }

    if (!erFarskapUtelukket() && erEnhetFarskap() && !(erJournalstatusMottaksregistrert() || erJournalstatusForUnderProduksjon())) {
      listeMedAvvik.add(FARSKAP_UTELUKKET);
    }

    return listeMedAvvik.stream().filter(this::erJournalstatusGyldigForAvvikstype).toList();
  }

  private boolean erJournalstatusGyldigForAvvikstype(Avvikstype avvikstype) {
    boolean gyldigJournalstatus = avvikstype.skalBehandleAvvikstype(journalstatus);

    if (!gyldigJournalstatus) {
      LOGGER.warn("Journalstatus {} er ugyldig for avvistype {}.", journalstatus, avvikstype);
    }

    return gyldigJournalstatus;
  }

  public Avviksbehandling startAvviksbehandling(AvvikshendelseIntern avvikshendelseIntern) {

    if (avvikshendelseIntern.erUgyldigForJournalstatus(journalstatus)) {
      var message =
          String.format(
              "Avvikstype er ugyldig: avvikstype[%s]/journalstatus[%s]",
              avvikshendelseIntern.getAvvikstype(), journalstatus);
      LOGGER.warn(message);

      return new UgyldigAvviksbehandling(avvikshendelseIntern.getAvvikstype(), message);
    }

    leggTilHendelseData(avvikshendelseIntern);

    return switch (avvikshendelseIntern.getAvvikstype()) {
      case ARKIVERE_JOURNALPOST -> startBehandlingAvArkivereJournalpostOppdatereStatus(
          avvikshendelseIntern);
      case BESTILL_ORIGINAL -> startBehandlingAvBestillOriginal(avvikshendelseIntern);
      case BESTILL_RESKANNING -> startBehandlingAvBestillReskanning(avvikshendelseIntern);
      case BESTILL_SPLITTING -> startBehandlingAvBestillSplitting(avvikshendelseIntern);
      case ENDRE_FAGOMRADE -> startBehandlingAvEndreFagomrade(avvikshendelseIntern);
      case FEILFORE_SAK -> startBehandlingAvFeilfortSak(avvikshendelseIntern);
      case INNG_TIL_UTG_DOKUMENT -> startBehandlingAvInnTilUtgDokument(avvikshendelseIntern);
      case OVERFOR_TIL_ANNEN_ENHET -> startBehandlingAvOverforingTilAnnenEnhet(
          avvikshendelseIntern);
      case SLETT_JOURNALPOST -> startBehandlingAvSlettJournalpost(avvikshendelseIntern);
      case TREKK_JOURNALPOST -> startBehandlingAvTrekkJournalpost(avvikshendelseIntern);
      case REGISTRER_RETUR -> startBehandlingAvRegistrerRetur(avvikshendelseIntern);
      case SEND_TIL_FAGOMRADE -> startBehandlingAvSendTilFagomrade(avvikshendelseIntern);
      case FARSKAP_UTELUKKET -> startBehandlingAvFarskapUtelukket(avvikshendelseIntern);
      default -> ugyldigAvviksbehandling(
          avvikshendelseIntern,
          Map.of("avvikstype", avvikshendelseIntern.getAvvikstype().toString()));
    };
  }

  private Avviksbehandling startBehandlingAvArkivereJournalpostOppdatereStatus(
      AvvikshendelseIntern avvikshendelseIntern) {
    return erJournalpostKlarForArkivering(avvikshendelseIntern.getJoarkArkiveringStatus())
        ? startGyldigBehandlingAvArkivereJournalpostOppdatereStatus(avvikshendelseIntern)
        : ugyldigAvviksbehandling(
            avvikshendelseIntern,
            Map.of(
                "journalstatus",
                notNull(journalstatus),
                "saksnummer",
                notNull(avvikshendelseIntern.getSaksnummer()),
                "joarkArkiveringStatus",
                avvikshendelseIntern.getJoarkArkiveringStatus().toString()));
  }

  private Avviksbehandling startBehandlingAvBestillOriginal(
      AvvikshendelseIntern avvikshendelseIntern) {
    return erInngaendeSkannetDokumentSomIkkeErElektroniskInnsendtOgIkkeHarOriginalBestilt()
        ? startGyldigBehandlingAvBestillOriginal(avvikshendelseIntern)
        : ugyldigAvviksbehandling(
            avvikshendelseIntern,
            Map.of(
                "dokumentType",
                notNull(dokumentType),
                "skannetDato",
                notNull(skannetDato),
                "orginalBestilt",
                notNull(originalBestilt)));
  }

  private Avviksbehandling startBehandlingAvBestillReskanning(
      AvvikshendelseIntern avvikshendelseIntern) {
    return erInngaendeSkannetDokumentSomIkkeErElektronisk()
        ? startGyldigBehandlingAvBestillReskanning(avvikshendelseIntern)
        : ugyldigAvviksbehandling(
            avvikshendelseIntern,
            Map.of("dokumentType", notNull(dokumentType), "skannetDato", notNull(skannetDato)));
  }

  private Avviksbehandling startBehandlingAvBestillSplitting(
      AvvikshendelseIntern avvikshendelseIntern) {
    return erInngaendeSkannetDokumentMedFilnavnIkkeOpprettetAvJoarkBatch()
        ? startGyldigBehandlingAvBestillSplitting(avvikshendelseIntern)
        : ugyldigAvviksbehandling(
            avvikshendelseIntern,
            Map.of(
                "dokumentType",
                notNull(dokumentType),
                "skannetDato",
                notNull(skannetDato),
                "batchNavn",
                notNull(batchNavn)));
  }

  private Avviksbehandling startBehandlingAvEndreFagomrade(
      AvvikshendelseIntern avvikshendelseIntern) {
    return erGyldigEndringAvFagomradeEllerKanEndreTilFagomradeUtenforBrevlagerMedBekreftelseForSendingTilScanning(
            avvikshendelseIntern)
        ? startGyldigBehandlingAvEndreFagomrade(avvikshendelseIntern)
        : ugyldigAvviksbehandling(avvikshendelseIntern, Map.of("fagomrade", notNull(fagomrade)));
  }

  private Avviksbehandling startBehandlingAvInnTilUtgDokument(
      AvvikshendelseIntern avvikshendelseIntern) {
    return erInngaendeDokument()
        ? startGyldigBehandlingAvInnTilUtgDokument(avvikshendelseIntern)
        : ugyldigAvviksbehandling(
            avvikshendelseIntern, Map.of("dokumentType", notNull(dokumentType)));
  }

  private Avviksbehandling startBehandlingAvOverforingTilAnnenEnhet(
      AvvikshendelseIntern avvikshendelseIntern) {
    return erJournalstatusMottaksregistrert()
        ? startGyldigBehandlingAvOverforingTilAnnenEnhet(avvikshendelseIntern)
        : ugyldigAvviksbehandling(
            avvikshendelseIntern,
            Map.of(
                "journalstatus", notNull(journalstatus),
                "journalforendeEnhet", notNull(journalforendeEnhet)));
  }

  private Avviksbehandling startBehandlingAvFeilfortSak(AvvikshendelseIntern avvikshendelseIntern) {
    return erIkkeJournalstatusForMottattOgHarSakSomIkkeErFeilfort(
            avvikshendelseIntern.getSaksnummer())
        ? startGyldigBehandlingAvFeilfortSak(avvikshendelseIntern)
        : ugyldigAvviksbehandling(
            avvikshendelseIntern,
            Map.of(
                "journalstatus", notNull(journalstatus), "feilført", String.valueOf(erFeilfort())));
  }

  private Avviksbehandling startBehandlingAvSlettJournalpost(
      AvvikshendelseIntern avvikshendelseIntern) {
    return erJournalstatusForUnderProduksjon()
        ? startGyldigBehandlingAvSlettJournalpost(avvikshendelseIntern)
        : ugyldigAvviksbehandling(
            avvikshendelseIntern, Map.of("journalstatus", notNull(journalstatus)));
  }

  private Avviksbehandling startBehandlingAvTrekkJournalpost(
      AvvikshendelseIntern avvikshendelseIntern) {
    return erJournalstatusMottaksregistrert()
        ? startGyldigBehandlingAvTrekkJournalpost(avvikshendelseIntern)
        : ugyldigAvviksbehandling(
            avvikshendelseIntern, Map.of("journalstatus", notNull(journalstatus)));
  }

  private Avviksbehandling startBehandlingAvRegistrerRetur(
      AvvikshendelseIntern avvikshendelseIntern) {
    return erDokumentTypeUtgaaende()
            && erGyldigJournalstatusForRegistrerRetur()
            && !harReturDetaljerLoggMedDato(avvikshendelseIntern.getReturDato())
        ? startGyldigBehandlingAvRegistrerRetur(avvikshendelseIntern)
        : ugyldigAvviksbehandling(
            avvikshendelseIntern, Map.of("journalstatus", notNull(journalstatus)));
  }

  private Avviksbehandling startBehandlingAvFarskapUtelukket(
      AvvikshendelseIntern avvikshendelseIntern) {
    return finnAvvik().contains(FARSKAP_UTELUKKET)
        ? startGyldigBehandlingAvFarskapUtelukket(avvikshendelseIntern)
        : ugyldigAvviksbehandling(
            avvikshendelseIntern, Map.of("enhet", notNull(journalforendeEnhet), "beskrivelse", notNull(beskrivelse), "status", notNull(journalstatus)));
  }

  private Avviksbehandling startBehandlingAvSendTilFagomrade(
      AvvikshendelseIntern avvikshendelseIntern) {
    return erInngaendeDokument() && erJournalstatusJournalfort()
        ? initGyldigAvviksbehandling(avvikshendelseIntern)
        : ugyldigAvviksbehandling(
            avvikshendelseIntern, Map.of("journalstatus", notNull(journalstatus)));
  }

  private Avviksbehandling ugyldigAvviksbehandling(
      AvvikshendelseIntern avvikshendelseIntern, Map<String, String> datatilstand) {
    var message =
        String.format(
            "Ugyldig avvik: %s - database: %s",
            avvikshendelseIntern.getAvvikstype(),
            datatilstand.entrySet().stream()
                .map(entry -> entry.getKey() + '=' + entry.getValue())
                .collect(Collectors.joining(", ")));

    LOGGER.warn(message);

    return new UgyldigAvviksbehandling(avvikshendelseIntern.getAvvikstype(), message);
  }

  private String notNull(Object obj) {
    return obj != null ? String.valueOf(obj) : "?";
  }

  private boolean erJournalpostKlarForArkivering(JoarkArkiveringStatus joarkArkiveringStatus) {
    return erGyldigJournalstatusForArkivering()
        && !JoarkArkiveringStatus.IKKE_STARTET.equals(joarkArkiveringStatus);
  }

  private boolean erInngaendeSkannetDokumentSomIkkeErElektroniskInnsendtOgIkkeHarOriginalBestilt() {
    return erInngaendeSkannetDokument() && batchNavnStarterIkkeMedBjoark() && !originalBestilt;
  }

  private boolean erInngaendeSkannetDokumentMedFilnavnIkkeOpprettetAvJoarkBatch() {
    return erInngaendeSkannetDokument()
        && medFilnavnOgBatchNavn()
        && batchNavnStarterIkkeMedBjoark();
  }

  private boolean
      erGyldigEndringAvFagomradeEllerKanEndreTilFagomradeUtenforBrevlagerMedBekreftelseForSendingTilScanning(
          AvvikshendelseIntern detaljer) {
    return kanEndreDatabase(detaljer)
        && (erGyldigEndringAvFagomradeForBrevlager(detaljer.getNyttFagomrade())
            || (FagomradeKt.erForAnnetEnnBrevlager(detaljer.getNyttFagomrade())
                && detaljer.erBreftetSendtScanning()));
  }

  private boolean kanEndreDatabase(AvvikshendelseIntern detaljer) {
    return detaljer.harNyttFagomrade()
        && fagomradeFraDbHarUlikVerdiSomSkrivesTilDb(detaljer.getNyttFagomrade());
  }

  private boolean fagomradeFraDbHarUlikVerdiSomSkrivesTilDb(String fagomrade) {
    return FagomradeKt.erUlike(this.fagomrade, fagomrade);
  }

  private boolean erGyldigEndringAvFagomradeForBrevlager(String fagomrade) {
    return erIkkeBlank(fagomrade) && FagomradeKt.erForBrevlager(fagomrade);
  }

  private boolean erIkkeBlank(String fagomrade) {
    return !(fagomrade == null || "".equals(fagomrade));
  }

  private boolean medFilnavnOgBatchNavn() {
    return filnavn != null && batchNavn != null;
  }

  private boolean erInngaendeSkannetDokument() {
    return erInngaendeDokument() && skannetDato != null;
  }

  private boolean erInngaendeSkannetDokumentSomIkkeErElektronisk() {
    return erInngaendeSkannetDokument() && batchNavnStarterIkkeMedBjoark();
  }

  private boolean batchNavnStarterIkkeMedBjoark() {
    return batchNavn == null || !batchNavn.startsWith(BATCH_NAVN_JOARK);
  }
  public boolean erEnhetFarskap() {
    return "4860".equals(journalforendeEnhet);
  }
  public boolean erFarskapUtelukket() {
    return "FAR".equals(fagomrade) && beskrivelse != null && beskrivelse.startsWith(ConstantsKt.FARSKAP_UTELUKKET_PREFIKS);
  }

  public boolean erInngaendeDokument() {
    return DokumentType.INNGAENDE_DOKUMENT.equalsIgnoreCase(dokumentType);
  }

  private boolean erIkkeJournalstatusForMottattOgHarSakSomIkkeErFeilfort(String saksnummer) {
    if (saksnummer == null) {
      return erIkkeMottattDokument() && erIkkeFeilfort();
    }

    return erIkkeMottattDokument()
        && journalsaker.stream()
            .filter(js -> js.getSaksnummer().equals(saksnummer))
            .anyMatch(js -> !js.erFeilfort());
  }

  private boolean erJournalstatusForUnderProduksjon() {
    return Journalstatus.UNDER_PRODUKSJON.equalsIgnoreCase(journalstatus);
  }

  private boolean erIkkeFeilfort() {
    return !erFeilfort();
  }

  boolean erFeilfort() {
    return journalsaker.stream().anyMatch(Journalsak::erFeilfort);
  }

  private boolean erIkkeMottattDokument() {
    return !Journalstatus.MOTTAKSREGISTRERT.equalsIgnoreCase(journalstatus);
  }

  private Avviksbehandling startGyldigBehandlingAvArkivereJournalpostOppdatereStatus(
      AvvikshendelseIntern avvikshendelseIntern) {

    if (avvikshendelseIntern.harIkkeSaksnummer()) {
      throw new SaksnummerManglerException();
    }

    var saksnummer = avvikshendelseIntern.getSaksnummer();
    var joarkJpId = avvikshendelseIntern.getJoarkJournalpostId();

    switch (avvikshendelseIntern.getJoarkArkiveringStatus()) {
      case STARTET -> journalsaker.stream()
          .filter(js -> js.getSaksnummer().equals(saksnummer))
          .findFirst()
          .map(Journalsak::startArkivering)
          .orElseThrow(() -> new SakIkkeTilknyttetJournalpostException(saksnummer));
      case FEILET -> journalsaker.stream()
          .filter(js -> js.getSaksnummer().equals(saksnummer))
          .findFirst()
          .map(Journalsak::feilforArkivering)
          .orElseThrow(() -> new SakIkkeTilknyttetJournalpostException(saksnummer));
      case FULLFORT -> {
        journalsaker.stream()
            .filter(js -> js.getSaksnummer().equals(saksnummer))
            .findFirst()
            .map(js -> js.fullforArkivering(joarkJpId))
            .orElseThrow(() -> new SakIkkeTilknyttetJournalpostException(saksnummer));
        journalstatus = Journalstatus.EKSPEDERT_JOARK;
      }
    }

    return initGyldigAvviksbehandling(avvikshendelseIntern);
  }

  private Avviksbehandling startGyldigBehandlingAvBestillOriginal(
      AvvikshendelseIntern avvikshendelseIntern) {
    originalBestilt = true;

    return initGyldigAvviksbehandling(
        avvikshendelseIntern,
        new BestillOriginalOppgave(
            journalpostId,
            avvikshendelseIntern.getSaksnummer(),
            skannetDato,
            batchNavn,
            gjelder,
            avvikshendelseIntern.getSaksbehandlersEnhet()));
  }

  Avviksbehandling startGyldigBehandlingAvBestillReskanning(
      AvvikshendelseIntern avvikshendelseIntern) {
    journalstatus = Journalstatus.AVVIK_BESTILL_RESKANNING;
    journalsaker.stream()
        .filter(js -> js.getSaksnummer().equals(avvikshendelseIntern.getSaksnummer()))
        .findFirst()
        .map(Journalsak::setFeilfort);

    return initGyldigAvviksbehandling(
        avvikshendelseIntern,
        new BestillReskanningOppgave(
            journalpostId,
            avvikshendelseIntern.getSaksnummer(),
            skannetDato,
            batchNavn,
            avvikshendelseIntern.getBeskrivelse(),
            gjelder,
            avvikshendelseIntern.getSaksbehandlersEnhet()));
  }

  private Avviksbehandling startGyldigBehandlingAvBestillSplitting(
      AvvikshendelseIntern avvikshendelseIntern) {
    journalsaker.forEach(Journalsak::setFeilfort);
    journalstatus = Journalstatus.AVVIK_BESTILL_SPLITTING;

    return initGyldigAvviksbehandling(
        avvikshendelseIntern,
        new BestillSplittingOppgave(
            journalpostId,
            avvikshendelseIntern.getSaksnummer(),
            skannetDato,
            batchNavn,
            filnavn,
            avvikshendelseIntern.hentBeskrivelse(),
            gjelder,
            avvikshendelseIntern.getSaksbehandlersEnhet()));
  }

  Avviksbehandling startGyldigBehandlingAvEndreFagomrade(
      AvvikshendelseIntern avvikshendelseIntern) {
    String fagomrade = avvikshendelseIntern.getNyttFagomrade();

    this.fagomrade = FagomradeKt.tilDatabase(fagomrade);
    boolean erFagomradeErAnnetEnnBidragOgFarskap = FagomradeKt.erForAnnetEnnBrevlager(fagomrade);

    if (erFagomradeErAnnetEnnBidragOgFarskap) {
      journalstatus = Journalstatus.AVVIK_ENDRE_FAGOMRADE;

      journalsaker.stream()
          .filter(js -> js.getSaksnummer().equals(avvikshendelseIntern.getSaksnummer()))
          .findFirst()
          .map(Journalsak::setFeilfort);
    }

    return initGyldigAvviksbehandling(avvikshendelseIntern);
  }

  private Avviksbehandling startGyldigBehandlingAvInnTilUtgDokument(
      AvvikshendelseIntern avvikshendelseIntern) {
    dokumentType = "U";
    setJournalforendeEnhet(avvikshendelseIntern.hentSaksbehandlersEnhetsnummer());
    return initGyldigAvviksbehandling(avvikshendelseIntern);
  }

  private Avviksbehandling startGyldigBehandlingAvOverforingTilAnnenEnhet(
      AvvikshendelseIntern avvikshendelseIntern) {
    journalforendeEnhet = avvikshendelseIntern.getEnhetsnummerNytt();

    return initGyldigAvviksbehandling(avvikshendelseIntern);
  }

  private Avviksbehandling startGyldigBehandlingAvFeilfortSak(
      AvvikshendelseIntern avvikshendelseIntern) {
    journalsaker.stream()
        .filter(
            js -> js.erForSaksnummer(Objects.requireNonNull(avvikshendelseIntern.getSaksnummer())))
        .findFirst()
        .map(Journalsak::setFeilfort);

    if (journalsaker.stream().allMatch(Journalsak::erFeilfort)) {
      journalstatus = Journalstatus.FEILFORT;
    }

    return initGyldigAvviksbehandling(avvikshendelseIntern);
  }

  private Avviksbehandling startGyldigBehandlingAvSlettJournalpost(
      AvvikshendelseIntern avvikshendelseIntern) {
    journalstatus = Journalstatus.SLETTET;

    return initGyldigAvviksbehandling(avvikshendelseIntern);
  }

  private Avviksbehandling startGyldigBehandlingAvTrekkJournalpost(
      AvvikshendelseIntern avvikshendelseIntern) {
    journalstatus = Journalstatus.UTGAR;

    return initGyldigAvviksbehandling(avvikshendelseIntern);
  }

  private Avviksbehandling startGyldigBehandlingAvRegistrerRetur(
      AvvikshendelseIntern avvikshendelseIntern) {
    var returDato = LocalDate.parse(avvikshendelseIntern.getReturDato());
    var isCurrentReturDatoInReturDetaljerLogg =
        this.getReturDetaljerLogg().stream().anyMatch(it -> it.dato.equals(this.returDato));
    if (this.returDato != null && !isCurrentReturDatoInReturDetaljerLogg) {
      leggTilReturDetaljerLogg(new ReturDetaljerLogg(this.returDato, ""));
    }
    oppdaterReturDato(returDato);
    leggTilReturDetaljerLogg(
        new ReturDetaljerLogg(returDato, avvikshendelseIntern.getBeskrivelse()));
    return initGyldigAvviksbehandling(avvikshendelseIntern);
  }

  private Avviksbehandling startGyldigBehandlingAvFarskapUtelukket(
      AvvikshendelseIntern avvikshendelseIntern) {
    fagomrade = "FAR";
    beskrivelse = String.format("%s: %s", ConstantsKt.FARSKAP_UTELUKKET_PREFIKS, beskrivelse);
    return initGyldigAvviksbehandling(avvikshendelseIntern);

  }


  public boolean tilhorerSak(String saksnummer) {
    return journalsaker.stream()
        .anyMatch(journalsak -> journalsak.getSaksnummer().equals(saksnummer));
  }

  public boolean tilhorerIkkeSak(String saksnummer) {
    return !tilhorerSak(saksnummer);
  }

  boolean erIkkeSlettet() {
    return !erSlettet();
  }

  private boolean erSlettet() {
    return Journalstatus.SLETTET.equalsIgnoreCase(journalstatus);
  }

  public Journalsak hentJournalsak(String saksnummer) {
    return journalsaker.stream()
        .filter(js -> saksnummer.equals(js.getSaksnummer()))
        .findFirst()
        .orElse(null);
  }

  private boolean erJournalstatusMottaksregistrert() {
    return Journalstatus.MOTTAKSREGISTRERT.equalsIgnoreCase(journalstatus);
  }

  private boolean erJournalstatusJournalfort() {
    return Journalstatus.JOURNALFORT.equalsIgnoreCase(journalstatus);
  }

  private boolean erDokumentTypeUtgaaende() {
    return DokumentType.UTGAAENDE_DOKUMENT.equalsIgnoreCase(dokumentType);
  }
  private boolean erGyldigJournalstatusForRegistrerRetur() {
    return Journalstatus.EKSPEDERT.equalsIgnoreCase(journalstatus)
        || Journalstatus.RESERVERT.equalsIgnoreCase(journalstatus);
  }

  public boolean erJournalstatusIkkeMottaksregistrert() {
    return !erJournalstatusMottaksregistrert();
  }



  public boolean erGyldigJournalstatusForArkivering() {
    return Journalstatus.KLAR_TIL_PRINT.equalsIgnoreCase(journalstatus)
        || Journalstatus.EKSPEDERT_JOARK.equalsIgnoreCase(journalstatus);
  }

  public boolean harIngenJournalsaker() {
    return journalsaker.isEmpty();
  }

  public boolean kanIkkeEndre(int journalpostId) {
    return this.journalpostId == null || journalpostId != this.journalpostId;
  }

  public boolean manglerGjelder(String gjelder) {
    return gjelder == null && this.gjelder == null;
  }

  public HendelseData leggTilHendelseData(AvvikshendelseIntern avvikshendelseIntern) {

    if (hendelseData == null) {
      hendelseData =
          new HendelseData(journalpostId, fagomrade, initDetaljerFor(avvikshendelseIntern));
    }

    if (harAlleDataForHendelse(avvikshendelseIntern.getAvvikstype())) {
      avvikshendelseIntern.leggTilHendelseData(hendelseData);
    }

    return hendelseData;
  }

  private boolean harAlleDataForHendelse(Avvikstype avvikstype) {
    return switch (avvikstype) {
      case ENDRE_FAGOMRADE, OVERFOR_TIL_ANNEN_ENHET -> journalforendeEnhet != null;
      default -> true;
    };
  }

  private Map<String, String> initDetaljerFor(AvvikshendelseIntern avvikshendelseIntern) {
    return switch (avvikshendelseIntern.getAvvikstype()) {
      case ENDRE_FAGOMRADE -> Map.of(
          ENHETSNUMMER,
          journalforendeEnhet != null
              ? journalforendeEnhet
              : avvikshendelseIntern.getEnhetsnummer());
      case OVERFOR_TIL_ANNEN_ENHET -> Map.of(
          AvvikDetaljer.ENHETSNUMMER_GAMMELT, avvikshendelseIntern.getEnhetsnummerGammelt());
      default -> Collections.emptyMap();
    };
  }

  public Set<String> hentTilknyttedeSaksnummer() {
    return journalsaker.stream().map(Journalsak::getSaksnummer).collect(Collectors.toSet());
  }

  public String hentTilknyttetSaksnummer() {
    return journalsaker.stream()
        .filter(not(Journalsak::erFeilfort))
        .map(Journalsak::getSaksnummer)
        .findFirst()
        .orElse(null);
  }

  public boolean erTilknyttetSak() {
    return journalsaker.stream().filter(not(Journalsak::erFeilfort)).iterator().hasNext();
  }

  public boolean harSattGjelder() {
    return gjelder != null;
  }

  public JournalpostHendelse initJournalpostHendelse(String saksbehandlersEnhet) {
    return new JournalpostHendelse(
        "%s-%d".formatted(Fagomrade.BIDRAG, journalpostId),
        null,
        gjelder,
        null,
        beskrivelse,
        FagomradeKt.fraDatabase(fagomrade),
        FagomradeKt.fraDatabase(fagomrade),
        batchNavn,
        dokumentType,
        HendelseType.ENDRING,
        journalforendeEnhet,
        journalstatus,
        JournalpostStatus.Companion.fraKode(journalstatus),
        new Sporingsdata(
            CorrelationId.Companion.fetchCorrelationIdForThread(), null, null, saksbehandlersEnhet),
        hentTilknyttedeSaksnummer().stream().toList(),
        dokumentdato,
        journaldato, null, null);
  }

  public String hentFagomrade() {
    return FagomradeKt.fraDatabase(fagomrade);
  }

  @Override
  public String toString() {
    return String.format(
        "Journalpost{journalpostId=%s, journalstatus=%s, saksjournaler=%s}",
        journalpostId, journalstatus, journalsaker);
  }

  public String getAvsender() {
    return avsender;
  }

  public String getAvsenderFornavn() {
    return avsenderFornavn;
  }

  public String getBeskrivelse() {
    return beskrivelse;
  }

  public LocalDate getDokumentdato() {
    return dokumentdato;
  }

  public String getDokumentreferanse() {
    return dokumentreferanse;
  }

  public String getDokumentType() {
    return dokumentType;
  }

  public String getFagomrade() {
    return fagomrade;
  }

  public String getGjelder() {
    return gjelder;
  }

  public HendelseData getHendelseData() {
    return hendelseData;
  }

  public LocalDate getJournaldato() {
    return journaldato;
  }

  public String getJournalforendeEnhet() {
    return journalforendeEnhet;
  }

  public String getJournalfortAv() {
    return journalfortAv;
  }

  public Integer getJournalpostId() {
    return journalpostId;
  }

  public List<Journalsak> getJournalsaker() {
    return journalsaker;
  }

  public String getJournalstatus() {
    return journalstatus;
  }

  public void settStatusEkspedert() {
    journalstatus = Journalstatus.EKSPEDERT;
  }

  public void settStatusMottatt() {
    journalstatus = Journalstatus.MOTTAKSREGISTRERT;
  }

  public void settStatusKlarTilPrint() {
    journalstatus = Journalstatus.KLAR_TIL_PRINT;
  }

  public void setJournalstatus(String journalstatus) {
    this.journalstatus = journalstatus;
  }

  private void setJournalforendeEnhet(String journalforendeEnhet) {
    this.journalforendeEnhet = journalforendeEnhet;
  }

  public void setSkannetDato(LocalDate skannetDato) {
    this.skannetDato = skannetDato;
  }

  public boolean hasStatusKlarTilPrint() {
    return Journalstatus.KLAR_TIL_PRINT.equals(journalstatus);
  }

  public boolean erUtgaaende() {
    return DokumentType.UTGAAENDE_DOKUMENT.equalsIgnoreCase(dokumentType);
  }

  public boolean erNotat() {
    return DokumentType.NOTAT.equalsIgnoreCase(dokumentType);
  }

  public boolean harTemaBID() {
    return Fagomrade.BIDRAG.equals(fagomrade) || Fagomrade.BIDRAG_DATABASE.equals(fagomrade);
  }

  public boolean gjelderErSamhandler() {
    return gjelder != null && (gjelder.startsWith("8") || gjelder.startsWith("9"));
  }

  public boolean hasStatusEkspedert() {
    return Journalstatus.EKSPEDERT.equals(journalstatus);
  }

  public String getMottakerId() {
    return mottakerId;
  }

  public boolean erMottakerOgGjelderSamme() {
    return Strings.isBlank(mottakerId) || mottakerId.equals(gjelder);
  }

  public String getBrevkode() {
    return brevkode;
  }

  public String getFilnavn() {
    return filnavn;
  }

  public void setBrevkode(String brevkode) {
    this.brevkode = brevkode;
  }

  public String getSystemId() {
    return systemId;
  }

  public String getArkivdel() {
    return arkivdel;
  }

  public String getDokstatus() {
    return dokstatus;
  }

  public LocalDateTime getArkiveringstidspunkt() {
    return arkiveringstidspunkt;
  }

  public String getTjeneste() {
    return tjeneste;
  }

  public String getKravtype() {
    return kravtype;
  }

  public String getBrukerid() {
    return brukerid;
  }

  public String getJournalforendeEnhetNavn() {
    return journalforendeEnhetNavn;
  }

  public String getBatchNavn() {
    return batchNavn;
  }

  public LocalDateTime getDokStatusSjekket() {
    return dokStatusSjekket;
  }

  public void setDokStatusSjekket(LocalDateTime dokStatusSjekket) {
    this.dokStatusSjekket = dokStatusSjekket;
  }
}
