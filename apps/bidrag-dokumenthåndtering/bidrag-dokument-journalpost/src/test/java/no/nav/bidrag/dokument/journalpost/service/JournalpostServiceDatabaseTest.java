package no.nav.bidrag.dokument.journalpost.service;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.TEST;
import static no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger.enJournalpost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import java.util.Optional;
import jakarta.transaction.Transactional;
import no.nav.bidrag.commons.CorrelationId;
import no.nav.bidrag.dokument.journalpost.configuration.BidragDokumentJournalpostConfig.SaksbehandlerOidcTokenManager;
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest;
import no.nav.bidrag.dokument.journalpost.TestDataManager;
import no.nav.bidrag.dokument.journalpost.consumer.OppgaveConsumer;
import no.nav.bidrag.dokument.journalpost.consumer.SaksbehandlerConsumer;
import no.nav.bidrag.dokument.journalpost.dto.CommandBuilder;
import no.nav.bidrag.dokument.journalpost.dto.EndreJournalpostCommandIntern;
import no.nav.bidrag.dokument.journalpost.dto.JournalpostIntern;
import no.nav.bidrag.dokument.journalpost.dto.JournalpostResponseIntern;
import no.nav.bidrag.dokument.journalpost.dto.Saksbehandler;
import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostKafkaEventProducer;
import no.nav.bidrag.dokument.journalpost.model.Journalstatus;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@ActiveProfiles(TEST)
@DisplayName("JournalpostService, test av database")
@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BidragDokumentJournalpostLocalTest.class)
@EnableWireMock(value = @ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
class JournalpostServiceDatabaseTest {

  private static final String SAK_1234567 = "1234567";

  @Autowired
  private JournalpostService journalpostService;
  @Autowired
  private TestDataManager testDataManager;
  @MockitoBean
  private JournalpostKafkaEventProducer journalpostKafkaEventProducerMock;
  @MockitoBean
  private OppgaveConsumer oppgaveConsumerMock;
  @MockitoBean
  private SaksbehandlerOidcTokenManager saksbehandlerOidcTokenManagerMock;
  @MockitoBean
  private SaksbehandlerConsumer saksbehandlerConsumerMock;

  @BeforeEach
  void mockCorrelationId() {
    CorrelationId.Companion.generateTimestamped("JournalpostServiceDatabaseTest");
  }

  @Nested
  @DisplayName("endringer")
  class Endringer {

    @Test
    @DisplayName("skal hente journalpost for id og sak")
    void skalHenteJournalpostForIdOgSak() {
      var journalpostUtenSak = testDataManager.opprett(enJournalpost().utenSak());

      var response = journalpostService.hentJournalpost(journalpostUtenSak.getJournalpostId());
      var responseForSak = journalpostService.hentJournalpost(SAK_1234567, journalpostUtenSak.getJournalpostId());

      assertAll(
          () -> assertThat(response).extracting(JournalpostResponseIntern::getJournalpost).as("journalpost for id").isNotNull(),
          () -> assertThat(responseForSak).extracting(JournalpostResponseIntern::getJournalpost).as("journalpost for id og sak").isNull()
      );
    }

    @Test
    @DisplayName("skal knytte sak til journalpost når den endres")
    void skalKnytteSakTilJournalposVedEndring() {
      var saksbehandlerBrukerid = "s123456";
      var saksbehandlerNavn = "Tom Jones";

      var journalpostUtenSak = testDataManager.opprett(enJournalpost().utenSak().medGjelder("dr. a. cula"));
      testDataManager.opprettJournalsakForSaksnummer(SAK_1234567);
      var saksbehandlersPaaloggedeEnhet = "4806";

      var endreJournalpostCommand = new CommandBuilder()
          .medTilknyttSaker(SAK_1234567)
          .tilEndreJournalpostCommand();

      when(saksbehandlerOidcTokenManagerMock.hentSaksbehandler()).thenReturn(saksbehandlerBrukerid);
      when(saksbehandlerConsumerMock.hentSaksbehandler(saksbehandlerBrukerid))
          .thenReturn(Optional.of(new Saksbehandler(saksbehandlerBrukerid, saksbehandlerNavn)));

      journalpostService.endre(
          new EndreJournalpostCommandIntern(journalpostUtenSak.getJournalpostId(), saksbehandlersPaaloggedeEnhet, endreJournalpostCommand)
      );

      var responseForSak = journalpostService.hentJournalpost(SAK_1234567, journalpostUtenSak.getJournalpostId());

      assertThat(responseForSak).extracting(JournalpostResponseIntern::getJournalpost).as("journalpost for id og sak").isNotNull();
    }

    @Test
    @DisplayName("skal knytte sak til journalpost når den journalføres")
    void skalKnytteSakTilJournalpostVedJournalforing() {
      var journalpostUtenSak = testDataManager.opprett(enJournalpost().utenSak().medGjelder("dr. a. cula").medJournalstatus("M"));
      var saksbehandlersPaaloggedeEnhet = "4806";
      var saksbehandlerBrukerid = "s123456";
      var saksbehandlerNavn = "Tom Jones";

      var endreJournalpostCommand = new CommandBuilder()
          .medTilknyttSaker(SAK_1234567)
          .medSkalJournalfores()
          .tilEndreJournalpostCommand();

      when(saksbehandlerOidcTokenManagerMock.hentSaksbehandler()).thenReturn(saksbehandlerBrukerid);
      when(saksbehandlerConsumerMock.hentSaksbehandler(saksbehandlerBrukerid))
          .thenReturn(Optional.of(new Saksbehandler(saksbehandlerBrukerid, saksbehandlerNavn)));

      journalpostService.endre(
          new EndreJournalpostCommandIntern(journalpostUtenSak.getJournalpostId(), saksbehandlersPaaloggedeEnhet, endreJournalpostCommand)
      );

      var responseForSak = journalpostService.hentJournalpost(SAK_1234567, journalpostUtenSak.getJournalpostId());

      assertAll(
          () -> assertThat(responseForSak.getJournalpost()).as("journalpost for id og sak").isNotNull(),
          () -> assertThat(responseForSak.getJournalpost()).extracting(JournalpostIntern::getJournalstatus).as("journalpost.jorunalstatus")
              .isEqualTo("J")
      );
    }

    @Test
    @DisplayName("Skal sette 'journalforende enhet', 'brukerid', og 'journalført av' ved journalføring")
    void skalSetteJournalforendeEnhetBrukeridOgJournalfortAvVedJournalforing() {
      var journalpostUtenSak = testDataManager.opprett(enJournalpost().utenSak().medGjelder("dr. a. cula").medJournalstatus("M"));
      var saksbehandlersPaaloggedeEnhet = "4806";
      var saksbehandlerBrukerid = "s123456";
      var saksbehandlerNavn = "Tom Jones";

      var endreJournalpostCommand = new CommandBuilder()
          .medTilknyttSaker(SAK_1234567)
          .medSkalJournalfores()
          .tilEndreJournalpostCommand();

      when(saksbehandlerOidcTokenManagerMock.hentSaksbehandler()).thenReturn(saksbehandlerBrukerid);
      when(saksbehandlerConsumerMock.hentSaksbehandler(saksbehandlerBrukerid))
          .thenReturn(Optional.of(new Saksbehandler(saksbehandlerBrukerid, saksbehandlerNavn)));

      journalpostService.endre(
          new EndreJournalpostCommandIntern(journalpostUtenSak.getJournalpostId(), saksbehandlersPaaloggedeEnhet, endreJournalpostCommand)
      );

      var lagretJournalpost = journalpostService.hentJournalpost(journalpostUtenSak.getJournalpostId());

      assertAll(
          () -> assertThat(lagretJournalpost.getJournalpost()).as("journalpost for id").isNotNull(),
          () -> assertThat(lagretJournalpost.getJournalpost()).extracting(JournalpostIntern::getJournalstatus).as("journalpost.jorunalstatus")
              .isEqualTo(Journalstatus.JOURNALFORT),
          () -> assertThat(lagretJournalpost.getJournalpost()).extracting(JournalpostIntern::getJournalforendeEnhet)
              .as("journalpost.journalforendeEnhet")
              .isEqualTo(saksbehandlersPaaloggedeEnhet),
          () -> assertThat(lagretJournalpost.getJournalpost()).extracting(JournalpostIntern::getBrukerid).as("journalpost.brukerid")
              .isEqualTo(saksbehandlerBrukerid),
          () -> assertThat(lagretJournalpost.getJournalpost()).extracting(JournalpostIntern::getJournalfortAv).as("journalpost.journalfortAv")
              .isEqualTo(saksbehandlerNavn)
      );
    }

    @Test
    @DisplayName("skal opprette max 1 saksrelasjon til samme saksnummer")
    void skalOppretteMaxEnSaksrelasjonTilSammeSaksnummer() {
      var journalpostUtenSak = testDataManager.opprett(
          enJournalpost().utenSak().medGjelder("dr. a. cula").medJournalstatus("M")
      );

      var endreJournalpostCommand = new CommandBuilder()
          .medTilknyttSaker("101", "101", "101")
          .medJournalpostId(journalpostUtenSak.getJournalpostId())
          .tilEndreJournalpostCommand();

      var muligEndring = journalpostService.endre(
          new EndreJournalpostCommandIntern(journalpostUtenSak.getJournalpostId(), "1234", endreJournalpostCommand)
      );

      assertThat(muligEndring).isPresent();

      var endretJournalpost = journalpostService.hentJournalpostEntitet(journalpostUtenSak.getJournalpostId());

      assertThat(endretJournalpost).hasValueSatisfying(
          journalpost -> assertThat(journalpost.getJournalsaker()).hasSize(1)
      );
    }
  }
}
