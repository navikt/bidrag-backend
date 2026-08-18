package no.nav.bidrag.dokument.journalpost.controller;

import static no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger.enUtgaendeJournalpostKlarTilPrint;
import static no.nav.bidrag.dokument.journalpost.service.FeatureService.FEATURE_DISTRIBUTE_DOCUMENT_BUTTON;
import static no.nav.bidrag.dokument.journalpost.utils.TestUtilsKt.initHttpEntity;
import static no.nav.bidrag.dokument.journalpost.utils.TestUtilsKt.prefixId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import no.nav.bidrag.commons.web.EnhetFilter;
import no.nav.bidrag.transport.dokument.DistribuerJournalpostResponse;
import no.nav.bidrag.dokument.journalpost.entity.FeatureAccess;
import no.nav.bidrag.dokument.journalpost.model.Fagomrade;
import no.nav.bidrag.dokument.journalpost.model.Journalstatus;
import no.nav.bidrag.dokument.journalpost.utils.CustomHeader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

@DisplayName("JournalpostController distribuer test")
public class JournalpostControllerDistribuerTest extends AbstractControllerTest {


  @Test
  @DisplayName("skal endre journalpost status til EKSPEDERT")
  public void skalEndreJournalpostStatusTil_EKSPEDERT(){
    var journalpost = testDataManager.opprett(enUtgaendeJournalpostKlarTilPrint()
        .medAvsender("Cula, Dr. A.")
        .medBeskrivelse("Dette er et testnotat")
        .medDokumentdato(LocalDate.now().minusDays(2))
        .medDokumentreferanse("1001")
        .medFagomrade(Fagomrade.BIDRAG)
        .medGjelder("Guess!!!")
        .medJournalstatus(Journalstatus.KLAR_TIL_PRINT)
        .medJournaldato(LocalDate.now())
        .medJournalfortAv("S. Vindel")
        .medJournalforendeEnhet("Trygdekontoret")
        .leggTilSaksnummer("12345"));

    var response = httpHeaderTestRestTemplate.exchange(
        String.format(JOURNAL_DISTRIBUER, prefixId(journalpost)),
        HttpMethod.POST,
        initHttpEntity(null, new CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
        DistribuerJournalpostResponse.class
    );

    var storedJournalpost = journalpostRepository.findById(journalpost.getJournalpostId()).orElseThrow();
    assertAll(
        () -> assertThat(response.getStatusCode()).as("Status skal være 200").isEqualTo(HttpStatus.OK),
        () -> assertThat(storedJournalpost.getJournalstatus()).as("Journalpost skal ha status EKSPEDERT").isEqualTo(Journalstatus.EKSPEDERT),
        () -> assertThat(response.getBody().getJournalpostId()).as("Journalpostid").isEqualTo(prefixId(journalpost))
    );
  }

  @Test
  @DisplayName("skal tillate distribuering av journalpost")
  public void skalTillateDistribueringAvJournalpost(){
    var SAKSBEHANDLER ="Z23432141";
    when(saksbehandlerOidcTokenManagerMock.hentSaksbehandler()).thenReturn(SAKSBEHANDLER);
    var journalpost = testDataManager.opprett(enUtgaendeJournalpostKlarTilPrint()
        .medAvsender("Cula, Dr. A.")
        .medBeskrivelse("Dette er et testnotat")
        .medDokumentdato(LocalDate.now().minusDays(2))
        .medDokumentreferanse("1001")
        .medFagomrade(Fagomrade.BIDRAG)
        .medGjelder("Guess!!!")
        .medJournalstatus(Journalstatus.KLAR_TIL_PRINT)
        .medJournaldato(LocalDate.now())
        .medJournalfortAv("S. Vindel")
        .medJournalforendeEnhet("Trygdekontoret")
        .leggTilSaksnummer("123453"));
    testDataManager.opprettFeatureAccess(new FeatureAccess(FEATURE_DISTRIBUTE_DOCUMENT_BUTTON, "1001", SAKSBEHANDLER));

    var response = httpHeaderTestRestTemplate.exchange(
        String.format(JOURNAL_DISTRIBUER_ENABLED, prefixId(journalpost)),
        HttpMethod.GET,
        initHttpEntity(null, new CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
        Void.class
    );

    assertAll(
        () -> assertThat(response.getStatusCode()).as("Status skal være 200").isEqualTo(HttpStatus.OK)
    );
  }

  @Test
  @DisplayName("skal tillate distribuering av journalpost hvis servicebruker")
  public void skalTillateDistribueringAvJournalpostHvisServiceBruker(){
    var SAKSBEHANDLER ="Z23432141";
    when(saksbehandlerOidcTokenManagerMock.hentSaksbehandler()).thenReturn("srvbisys");
    var journalpost = testDataManager.opprett(enUtgaendeJournalpostKlarTilPrint()
        .medAvsender("Cula, Dr. A.")
        .medBeskrivelse("Dette er et testnotat")
        .medDokumentdato(LocalDate.now().minusDays(2))
        .medDokumentreferanse("1001")
        .medFagomrade(Fagomrade.BIDRAG)
        .medGjelder("Guess!!!")
        .medJournalstatus(Journalstatus.KLAR_TIL_PRINT)
        .medJournaldato(LocalDate.now())
        .medJournalfortAv("S. Vindel")
        .medJournalforendeEnhet("Trygdekontoret")
        .leggTilSaksnummer("123453"));
    testDataManager.opprettFeatureAccess(new FeatureAccess(FEATURE_DISTRIBUTE_DOCUMENT_BUTTON, "1001", SAKSBEHANDLER));

    var response = httpHeaderTestRestTemplate.exchange(
        String.format(JOURNAL_DISTRIBUER_ENABLED, prefixId(journalpost)),
        HttpMethod.GET,
        initHttpEntity(null, new CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
        Void.class
    );

    assertAll(
        () -> assertThat(response.getStatusCode()).as("Status skal være 200").isEqualTo(HttpStatus.OK)
    );
  }

  @Test
  @DisplayName("skal ikke tillate distribuering av journalpost")
  public void skalIkkeTillateDistribueringAvJournalpostHvisIngenFeatureTilgang(){
    var SAKSBEHANDLER ="Z23432141";
    when(saksbehandlerOidcTokenManagerMock.hentSaksbehandler()).thenReturn(SAKSBEHANDLER);
    var journalpost = testDataManager.opprett(enUtgaendeJournalpostKlarTilPrint()
        .medAvsender("Cula, Dr. A.")
        .medBeskrivelse("Dette er et testnotat")
        .medDokumentdato(LocalDate.now().minusDays(2))
        .medDokumentreferanse("1001")
        .medFagomrade(Fagomrade.BIDRAG)
        .medGjelder("Guess!!!")
        .medJournalstatus(Journalstatus.KLAR_TIL_PRINT)
        .medJournaldato(LocalDate.now())
        .medJournalfortAv("S. Vindel")
        .medJournalforendeEnhet("Trygdekontoret")
        .leggTilSaksnummer("12345"));
    testDataManager.opprettFeatureAccess(new FeatureAccess(FEATURE_DISTRIBUTE_DOCUMENT_BUTTON, "1001", "Z123"));

    var response = httpHeaderTestRestTemplate.exchange(
        String.format(JOURNAL_DISTRIBUER_ENABLED, prefixId(journalpost)),
        HttpMethod.GET,
        initHttpEntity(null, new CustomHeader(EnhetFilter.X_ENHET_HEADER, "1001")),
        Void.class
    );

    assertAll(
        () -> assertThat(response.getStatusCode()).as("Status skal være 406").isEqualTo(HttpStatus.NOT_ACCEPTABLE),
        () -> assertThat(response.getHeaders().get("Warning").get(0)).as("Warning message").isEqualTo("Saksbehandler eller enhet må ha tilgang til å distribuere journalpost")
    );
  }

}
