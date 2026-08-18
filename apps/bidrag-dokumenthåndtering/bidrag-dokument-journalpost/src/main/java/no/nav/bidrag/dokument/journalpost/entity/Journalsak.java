package no.nav.bidrag.dokument.journalpost.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import no.nav.bidrag.dokument.journalpost.dto.JournalpostIntern;
import no.nav.bidrag.dokument.journalpost.dto.JournalpostResponseIntern;

@Entity
@Table(name = "T_JSAK")
@SuppressWarnings("JpaDataSourceORMInspection")
public class Journalsak {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "JSAKID", nullable = false)
  private Integer journalsakId;

  @Column(name = "SAKSNR", nullable = false)
  String saksnummer;
  @Column(name = "FEILFORT", nullable = false)
  private boolean feilfort = false;
  @Column(name = "JOARK_JP_ID")
  Integer joarkJpId;
  @Column(name = "ARKIVERING_STARTET")
  LocalDateTime arkiveringStartet;
  @Column(name = "ARKIVERING_FULLFORT")
  LocalDateTime arkiveringFullfort;
  @Column(name = "ARKIVERING_FEILET")
  LocalDateTime arkiveringFeilet;

  @ManyToOne(targetEntity = Journalpost.class, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinColumn(name = "JP_ID", nullable = false)
  private Journalpost journalpost;

  @SuppressWarnings("unused")
  public Journalsak() {
    // used by entity manager
  }

  public Journalsak(String saksnummer) {
    this.saksnummer = saksnummer;
  }

  public Journalsak(Journalpost journalpost, String saksnummer) {
    this.journalpost = journalpost;
    this.saksnummer = saksnummer;
    journalpost.leggTil(this);
  }

  public boolean erFor(String fagomrade) {
    return journalpost != null && journalpost.erFor(fagomrade);
  }

  boolean erFeilfort() {
    return feilfort;
  }

  public boolean erForSaksnummer(@NotNull String saksnummer) {
    return saksnummer.equals(this.saksnummer);
  }

  public boolean erArkivertIJoark(){
    return Objects.nonNull(joarkJpId);
  }

  public boolean erIkkeSlettetJournalpost() {
    return journalpost != null && journalpost.erIkkeSlettet();
  }

  @Override
  public String toString() {
    return String.format("Journalsak{saksnummer='%s'%s}", saksnummer, Optional.ofNullable(journalpost)
        .map(jp -> ", journalpostId=" + jp.getJournalpostId() + ", fagomrade=" + jp.getFagomrade())
        .orElse("")
    );
  }

  Journalsak setFeilfort() {
    feilfort = true;
    return this;
  }

  Journalsak startArkivering() {
    arkiveringStartet = LocalDateTime.now();
    return this;
  }

  Journalsak fullforArkivering(int joarkJournalpostId) {
    if (!Objects.equals(joarkJournalpostId, joarkJpId)){
      arkiveringFullfort = LocalDateTime.now();
      joarkJpId = joarkJournalpostId;
    }
    return this;
  }

  Journalsak feilforArkivering() {
    arkiveringFeilet = LocalDateTime.now();
    return this;
  }

  public Journalpost getJournalpost() {
    return journalpost;
  }

  public JournalpostIntern tilJournalpostIntern() {
    return journalpost.tilJournalpostIntern(feilfort);
  }

  public JournalpostResponseIntern tilJournalpostResponseIntern() {
    return new JournalpostResponseIntern(tilJournalpostIntern(), new ArrayList<>(journalpost.hentTilknyttedeSaksnummer()));
   }

  void setJournalpost(Journalpost journalpost) {
    this.journalpost = journalpost;
  }

  public String hentJournalpostensJournalstatus() {
    return journalpost.getJournalstatus();
  }

  public String getSaksnummer() {
    return saksnummer;
  }

  public Integer getJoarkJpId(){
    return joarkJpId;
  }
}
