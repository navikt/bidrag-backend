package no.nav.bidrag.dokument.journalpost.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class FeatureAccessTest {

  private String SAKSBEHANDLER = "Z2312";
  private String ENHET = "1233";

  @Test
  public void shouldGiveAccessValidSaksbehandlerAndEnhet(){
    var featureAccess = new FeatureAccess("", ENHET, SAKSBEHANDLER);
    assertThat(featureAccess.isEnabled(SAKSBEHANDLER, ENHET)).isTrue();
    assertThat(featureAccess.isEnabled(SAKSBEHANDLER.toLowerCase(), ENHET.toLowerCase())).isTrue();
    assertThat(featureAccess.isEnabled(SAKSBEHANDLER, "333")).isFalse();
    assertThat(featureAccess.isEnabled("Zasdasd", ENHET)).isFalse();
  }

  @Test
  public void shouldGiveAccessValidSaksbehandler(){
    var featureAccess = new FeatureAccess("", null, SAKSBEHANDLER);
    assertThat(featureAccess.isEnabled(SAKSBEHANDLER, ENHET)).isTrue();
    assertThat(featureAccess.isEnabled(SAKSBEHANDLER, ENHET)).isTrue();
    assertThat(featureAccess.isEnabled(null, null)).isFalse();
    assertThat(featureAccess.isEnabled("asdasd", null)).isFalse();
  }

  @Test
  public void shouldGiveAccessValidEnhet(){
    var featureAccess = new FeatureAccess("", ENHET, null);
    assertThat(featureAccess.isEnabled(SAKSBEHANDLER, ENHET)).isTrue();
    assertThat(featureAccess.isEnabled(null, ENHET)).isTrue();
    assertThat(featureAccess.isEnabled(null, null)).isFalse();
    assertThat(featureAccess.isEnabled(null, "asdasd")).isFalse();
  }

  @Test
  public void shouldGiveAccessForAllCases(){
    var featureAccess = new FeatureAccess("", null, null);
    assertThat(featureAccess.isEnabled(SAKSBEHANDLER, ENHET)).isTrue();
    assertThat(featureAccess.isEnabled(SAKSBEHANDLER, null)).isTrue();
    assertThat(featureAccess.isEnabled(null, ENHET)).isTrue();
    assertThat(featureAccess.isEnabled(null, null)).isTrue();
  }

  @Test
  public void shouldGiveAccessForValidPeriod(){
    var dateNowMinusOneDay = LocalDate.now().minusDays(1);
    var dateNowPlusTwoDays = LocalDate.now().plusDays(2);
    var featureAccess = new FeatureAccess("", dateNowMinusOneDay, dateNowPlusTwoDays, null, null);
    assertThat(featureAccess.featureEnabled()).isTrue();
  }

  @Test
  public void shouldGiveAccessForValidToDate(){
    var dateNowPlusTwoDays = LocalDate.now().plusDays(2);
    var featureAccess = new FeatureAccess("", null, dateNowPlusTwoDays, null, null);
    assertThat(featureAccess.featureEnabled()).isTrue();
  }

  @Test
  public void shouldGiveAccessForValidFromDate(){
    var dateNowMinusOneDay = LocalDate.now().minusDays(1);
    var featureAccess = new FeatureAccess("", dateNowMinusOneDay, null, null, null);
    assertThat(featureAccess.featureEnabled()).isTrue();
  }

  @Test
  public void shouldNotGiveAccessForInvalidFromDate(){
    var fromDate = LocalDate.now().plusDays(1);
    var featureAccess = new FeatureAccess("", fromDate, null, null, null);
    assertThat(featureAccess.featureEnabled()).isFalse();
  }

  @Test
  public void shouldNotGiveAccessForInvalidToDate(){
    var toDate = LocalDate.now().minusDays(1);
    var featureAccess = new FeatureAccess("", null, toDate, null, null);
    assertThat(featureAccess.featureEnabled()).isFalse();
  }

  @Test
  public void shouldNotGiveAccessForInvalidPeriod(){
    var fromDate = LocalDate.now().minusDays(1);
    var toDate = LocalDate.now().minusDays(1);
    var featureAccess = new FeatureAccess("", fromDate, toDate, null, null);
    assertThat(featureAccess.featureEnabled()).isFalse();
  }
}
