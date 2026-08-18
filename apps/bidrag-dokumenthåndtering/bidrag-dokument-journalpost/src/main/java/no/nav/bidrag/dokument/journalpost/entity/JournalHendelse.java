package no.nav.bidrag.dokument.journalpost.entity;

import static no.nav.bidrag.dokument.journalpost.model.ConstantsKt.MAX_LENGDE_JORNALHENDELSE_BESKRIVELSE;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.bidrag.dokument.journalpost.model.JournalHendelseForAvvik;

@Entity
@Table(name = "T_JP_LOGG")
@SuppressWarnings("JpaDataSourceORMInspection")
public class JournalHendelse {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  private Integer id;

  @Column(name = "JP_ID")
  private Integer journalpostId;
  @Column(name = "OPPRETTET")
  private LocalDateTime opprettet;
  @Column(name = "BRUKERID")
  private String brukerIdent;
  @Column(name = "ENHET")
  private String enhet;
  @Column(name = "HENDELSE")
  private String hendelse;
  @Column(name = "BESKRIVELSE", length = MAX_LENGDE_JORNALHENDELSE_BESKRIVELSE)
  private String beskrivelse;
  @Column(name = "OPPGAVEID")
  private Long oppgaveId;


  public JournalHendelse() {
    // used by entity manager
  }

  public JournalHendelse(Integer journalpostId) {
    this.journalpostId = journalpostId;
  }

  public void leggTil(JournalHendelseForAvvik journalHendelseForAvvik) {
    opprettet = LocalDateTime.now();
    brukerIdent = EntityUtils.truncateBrukerId(journalHendelseForAvvik.getBrukerident(), 15);
    enhet = journalHendelseForAvvik.hentOpprettetAvEnhet();
    hendelse = journalHendelseForAvvik.lagHendelse();
    beskrivelse = journalHendelseForAvvik.lagBeskrivelse();
  }

  @Override
  public String toString() {
    return "JournalHendelse{" +
        "id=" + id +
        ", journalpostId=" + journalpostId +
        ", opprettet=" + opprettet +
        ", brukerIdent='" + brukerIdent + '\'' +
        ", enhet='" + enhet + '\'' +
        ", hendelse='" + hendelse + '\'' +
        ", beskrivelse='" + beskrivelse + '\'' +
        ", oppgaveId=" + oppgaveId +
        '}';
  }

  public String getBeskrivelse() {
    return beskrivelse;
  }

  public String getBrukerIdent() {
    return brukerIdent;
  }

  public String getEnhet() {
    return enhet;
  }

  public String getHendelse() {
    return hendelse;
  }

  public Integer getId() {
    return id;
  }

  public void setOppgaveId(Long oppgaveId) {
    this.oppgaveId = oppgaveId;
  }

  public LocalDateTime getOpprettet() {
    return opprettet;
  }
}
