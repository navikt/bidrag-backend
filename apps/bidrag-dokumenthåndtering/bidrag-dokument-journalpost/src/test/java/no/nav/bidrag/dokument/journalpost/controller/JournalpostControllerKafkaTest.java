package no.nav.bidrag.dokument.journalpost.controller;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.SECURED_TEST;
import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.TEST;
import static no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger.enMottaksregistrertJournalpost;
import static no.nav.bidrag.dokument.journalpost.utils.TestUtilsKt.initHttpEntity;
import static no.nav.bidrag.dokument.journalpost.utils.TestUtilsKt.prefixId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import no.nav.bidrag.commons.web.EnhetFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest;
import no.nav.bidrag.dokument.journalpost.TestDataManager;
import no.nav.bidrag.dokument.journalpost.configuration.BidragDokumentJournalpostConfig.SaksbehandlerOidcTokenManager;
import no.nav.bidrag.dokument.journalpost.dto.Saksbehandler;
import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostKafkaEventProducer;
import no.nav.bidrag.dokument.journalpost.model.DokumentType;
import no.nav.bidrag.dokument.journalpost.model.Fagomrade;
import no.nav.bidrag.dokument.journalpost.service.TilgangskontrollService;
import no.nav.bidrag.dokument.journalpost.service.TokenInformationService;
import no.nav.bidrag.dokument.journalpost.utils.CustomHeader;
import no.nav.bidrag.transport.dokument.BehandleAvvikshendelseResponse;
import no.nav.bidrag.transport.dokument.JournalpostHendelse;
import no.nav.bidrag.transport.dokument.Sporingsdata;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@ActiveProfiles({TEST, SECURED_TEST})
@DisplayName("JournalpostControllerTest for JournalpostKafkaEventProducer")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BidragDokumentJournalpostLocalTest.class)
@EnableWireMock(value = @ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
public class JournalpostControllerKafkaTest {

  @Autowired
  private TestRestTemplate httpHeaderTestRestTemplate;
  @Autowired
  private TestDataManager testDataManager;

  @MockitoBean
  private TilgangskontrollService tilgangskontrollServiceMock;
  @MockitoBean
  private HttpHeaderRestTemplate httpHeaderRestTemplateMock;
  @MockitoBean
  private JournalpostKafkaEventProducer journalpostKafkaEventProducerMock;
  @MockitoBean
  private SaksbehandlerOidcTokenManager saksbehandlerOidcTokenManagerMock;
  @MockitoBean
  private TokenInformationService tokenInformationServiceMock;

  @BeforeEach
  void resetMocks() {
    reset(
        tilgangskontrollServiceMock,
        httpHeaderRestTemplateMock,
        journalpostKafkaEventProducerMock,
        saksbehandlerOidcTokenManagerMock,
        tokenInformationServiceMock
    );
  }

  @Test
  @DisplayName("skal legge ved saksbehandlers brukerident og navn i sporingsdata når JournalpostHendelse opprettes")
  void skalLeggeVedBrukeridentOgNavnNarHendelseOpprettes() {
    var gammeltEnhetsnummer = "007";
    var enMottaksregistrertJournalpost = testDataManager.opprett(
        enMottaksregistrertJournalpost()
            .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
            .medFagomrade(Fagomrade.BIDRAG)
            .medJournalforendeEnhet(gammeltEnhetsnummer)
    );

    var nyttEnhetsnummer = "1001";
    var avvikshendelse = String.format("""
            {
              "avvikType":"OVERFOR_TIL_ANNEN_ENHET",
              "detaljer": {
                "gammeltEnhetsnummer":"%s",
                "nyttEnhetsnummer"   :"%s"
              }
            }
            """.stripIndent(),
        gammeltEnhetsnummer, nyttEnhetsnummer
    );

    when(tokenInformationServiceMock.hentSaksbehandler()).thenReturn(Optional.of(new Saksbehandler("jactor-rises", "Blund, Jon")));

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        String.format("/journal/%s/avvik", prefixId(enMottaksregistrertJournalpost)),
        HttpMethod.POST,
        initHttpEntity(avvikshendelse, new CustomHeader(EnhetFilter.X_ENHET_HEADER, gammeltEnhetsnummer)),
        BehandleAvvikshendelseResponse.class
    );

    assertThat(responseEntity.getStatusCode()).as("status").isEqualTo(HttpStatus.OK);
    var hendelseCaptor = ArgumentCaptor.forClass(JournalpostHendelse.class);
    verify(journalpostKafkaEventProducerMock).publish(hendelseCaptor.capture());
    var sporing = hendelseCaptor.getValue().getSporing();

    assertAll(
        () -> assertThat(sporing).extracting(Sporingsdata::getBrukerident).isEqualTo("jactor-rises"),
        () -> assertThat(sporing).extracting(Sporingsdata::getSaksbehandlersNavn).isEqualTo("Blund, Jon")
    );
  }

  @Test
  @DisplayName("skal legge ved brukerident fra token når data om saksbehandler ikke finnes")
  void skalLeggeVedBrukeridentFraTokenNarSaksbehandlerIkkeFinnes() {
    var gammeltEnhetsnummer = "007";
    var enMottaksregistrertJournalpost = testDataManager.opprett(
        enMottaksregistrertJournalpost()
            .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
            .medFagomrade(Fagomrade.BIDRAG)
            .medJournalforendeEnhet(gammeltEnhetsnummer)
    );

    var nyttEnhetsnummer = "1001";
    var avvikshendelse = String.format("""
            {
              "avvikType":"OVERFOR_TIL_ANNEN_ENHET",
              "detaljer": {
                "gammeltEnhetsnummer":"%s",
                "nyttEnhetsnummer"   :"%s"
              }
            }
            """.stripIndent(),
        gammeltEnhetsnummer, nyttEnhetsnummer
    );

    when(tokenInformationServiceMock.hentSaksbehandler()).thenReturn(Optional.empty());
    when(tokenInformationServiceMock.hentSaksbehandlersBrukerid()).thenReturn("jactor-rises");

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        String.format("/journal/%s/avvik", prefixId(enMottaksregistrertJournalpost)),
        HttpMethod.POST,
        initHttpEntity(avvikshendelse, new CustomHeader(EnhetFilter.X_ENHET_HEADER, gammeltEnhetsnummer)),
        BehandleAvvikshendelseResponse.class
    );

    assertThat(responseEntity.getStatusCode()).as("status").isEqualTo(HttpStatus.OK);
    var hendelseCaptor = ArgumentCaptor.forClass(JournalpostHendelse.class);
    verify(journalpostKafkaEventProducerMock).publish(hendelseCaptor.capture());
    var sporing = hendelseCaptor.getValue().getSporing();

    assertAll(
        () -> assertThat(sporing).extracting(Sporingsdata::getBrukerident).isEqualTo("jactor-rises"),
        () -> assertThat(sporing).extracting(Sporingsdata::getSaksbehandlersNavn).isNull()
    );
  }
}
