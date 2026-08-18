package no.nav.bidrag.dokument.journalpost.controller;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.SECURED_TEST;
import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.TEST;
import static org.mockito.Mockito.reset;

import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest;
import no.nav.bidrag.dokument.journalpost.TestDataManager;
import no.nav.bidrag.dokument.journalpost.configuration.BidragDokumentJournalpostConfig.SaksbehandlerOidcTokenManager;
import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostHendelseListener;
import no.nav.bidrag.dokument.journalpost.repository.JournalpostRepository;
import no.nav.bidrag.dokument.journalpost.repository.JournalsakReposistory;
import no.nav.bidrag.dokument.journalpost.service.TilgangskontrollService;
import no.nav.bidrag.dokument.journalpost.service.TokenInformationService;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@ActiveProfiles({TEST, SECURED_TEST})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BidragDokumentJournalpostLocalTest.class, properties = "STS_URL=junit")
@EnableWireMock(@ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
public abstract class AbstractControllerTest {
  protected static final String JOURNAL_MED_SAK = "/journal/%s?saksnummer=%s";
  protected static final String JOURNAL_UTEN_SAK = "/journal/%s";
  protected static final String JOURNAL_DISTRIBUER = "/journal/distribuer/%s";
  protected static final String JOURNAL_DISTRIBUER_ENABLED = "/journal/distribuer/%s/enabled";

  @LocalServerPort
  protected int port;

  @Autowired
  protected JournalsakReposistory journalsakReposistory;
  @Autowired
  protected JournalpostRepository journalpostRepository;
  @Autowired
  protected TestDataManager testDataManager;
  @Autowired
  protected TestRestTemplate httpHeaderTestRestTemplate;

  @MockitoBean
  protected TilgangskontrollService tilgangskontrollServiceMock;
  @MockitoBean
  protected HttpHeaderRestTemplate httpHeaderRestTemplateMock;
  @MockitoBean
  protected JournalpostHendelseListener journalpostHendelseListenerMock;
  @MockitoBean
  protected SaksbehandlerOidcTokenManager saksbehandlerOidcTokenManagerMock;
  @MockitoBean
  protected TokenInformationService tokenInformationServiceMock;

  @BeforeEach
  void resetMocks() {
    reset(
        tilgangskontrollServiceMock,
        httpHeaderRestTemplateMock,
        journalpostHendelseListenerMock,
        tokenInformationServiceMock,
        saksbehandlerOidcTokenManagerMock
    );
  }

  protected String baseUrl(){
      return "http://localhost:" + port + "/bidrag-dokument-journalpost/";
  }
}
