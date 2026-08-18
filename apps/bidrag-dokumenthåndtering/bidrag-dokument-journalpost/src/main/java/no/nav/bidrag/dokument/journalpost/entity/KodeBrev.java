package no.nav.bidrag.dokument.journalpost.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import no.nav.bidrag.dokument.journalpost.dto.KodeIntern;

@Entity
@Table(name = "T_KODE_BREV")
@SuppressWarnings("JpaDataSourceORMInspection")
public class KodeBrev {

  @Id
  @Column(name = "KODE")
  String kode;
  @Column(name = "DEKODE")
  String dekode;
  @Column(name = "ER_GYLDIG")
  boolean erGyldig;

  @Column(name = "kravtype")
  String kravType;

  @Transient
  private KodeIntern kodeDto;

  public KodeIntern tilKodeIntern() {
    if (kodeDto == null) {
      kodeDto = new KodeIntern(kode, dekode, kravType, erGyldig);
    }

    return kodeDto;
  }

  @Override
  public String toString() {
    return kode + " - " + dekode;
  }
}
