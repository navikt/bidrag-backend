package no.nav.bidrag.dokument.journalpost.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "T_KODE_JOURN_STAT")
@SuppressWarnings("JpaDataSourceORMInspection")
public class KodeJournalstatus {

  @Id
  @Column(name = "KODE")
  private String kode;
  @Column(name = "VIS_I_JOURNAL")
  private boolean skalVises;

  public KodeJournalstatus() {
    // brukes av EntityManager
  }

  public KodeJournalstatus(String journalstatus, boolean skalVises) {
    kode = journalstatus;
    this.skalVises = skalVises;
  }

  public boolean skalVises() {
    return skalVises;
  }
}
