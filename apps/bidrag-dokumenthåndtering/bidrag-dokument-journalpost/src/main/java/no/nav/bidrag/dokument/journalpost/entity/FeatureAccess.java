package no.nav.bidrag.dokument.journalpost.entity;

import java.time.LocalDate;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.apache.logging.log4j.util.Strings;

@Entity
@Table(name = "t_feature_access")
public class FeatureAccess {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  private int id;

  @Column(name = "feature")
  private String featureName;

  @Column(name = "periode_fra", nullable = true)
  private LocalDate periodeFra;

  @Column(name = "periode_til", nullable = true)
  private LocalDate periodeTil;

  @Column(name = "enhetsnr")
  private String enhetsnr;

  @Column(name = "bruker")
  private String brukerNavn;

  protected FeatureAccess() {
    // Hibernate Constructor
  }

  public FeatureAccess(String featureName, LocalDate periodeFra, LocalDate periodeTil, String enhetsnr, String brukerNavn) {
    this.featureName = featureName;
    this.periodeTil = periodeTil;
    this.periodeFra = periodeFra;
    this.enhetsnr = enhetsnr;
    this.brukerNavn = brukerNavn;
  }

  public FeatureAccess(String featureName, String enhetsnr, String brukerNavn) {
    this.featureName = featureName;
    this.enhetsnr = enhetsnr;
    this.brukerNavn = brukerNavn;
  }

  public int getId() {
    return id;
  }

  public Boolean isEnabled(String saksbehandlerId, String enhetsnr){
    return checkAccess(brukerNavn, saksbehandlerId) && checkAccess(this.enhetsnr, enhetsnr);
  }

  public Boolean featureEnabled(){
    var today = LocalDate.now();
    var isAfterPeriodeFra = periodeFra == null || today.isAfter(periodeFra);
    var isBeforePeriodeTil = periodeTil == null || today.isBefore(periodeTil);
    return isAfterPeriodeFra && isBeforePeriodeTil;
  }

  private boolean checkAccess(String accessValue, String inputValue){
    var inputValueLowerCase = Strings.isNotEmpty(inputValue) ? inputValue.toLowerCase() : inputValue;
     return accessValue == null || accessValue.isEmpty() || Objects.equals(accessValue.toLowerCase(), inputValueLowerCase);
  }

}
