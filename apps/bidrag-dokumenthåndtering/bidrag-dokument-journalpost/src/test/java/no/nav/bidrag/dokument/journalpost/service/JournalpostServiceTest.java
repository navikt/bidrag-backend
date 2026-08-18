package no.nav.bidrag.dokument.journalpost.service;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.TEST;
import static no.nav.bidrag.dokument.journalpost.dto.CommandBuilder.enCommandBuilder;
import static no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger.enJournalpost;
import static no.nav.bidrag.dokument.journalpost.entity.JournalsakBygger.enJournalsak;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest;
import no.nav.bidrag.dokument.journalpost.dto.AktorIntern;
import no.nav.bidrag.dokument.journalpost.dto.JournalpostIntern;
import no.nav.bidrag.dokument.journalpost.dto.KodeIntern;
import no.nav.bidrag.dokument.journalpost.dto.Sakjournal;
import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostKafkaEventProducer;
import no.nav.bidrag.dokument.journalpost.model.Fagomrade;
import no.nav.bidrag.dokument.journalpost.model.Journalstatus;
import no.nav.bidrag.dokument.journalpost.repository.JournalpostRepository;
import no.nav.bidrag.dokument.journalpost.repository.JournalsakReposistory;
import no.nav.bidrag.dokument.journalpost.utils.TestUtilsKt;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@ActiveProfiles(TEST)
@DisplayName("JournalpostService")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BidragDokumentJournalpostLocalTest.class)
@EnableWireMock(value = @ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
class JournalpostServiceTest {

  @Autowired
  private JournalpostService journalpostService;
  @MockitoBean
  private TilgangskontrollService tilgangskontrollServiceMock;
  @MockitoBean
  private KodeService kodeServiceMock;
  @MockitoBean
  private JournalpostKafkaEventProducer journalpostKafkaEventProducerMock;
  @MockitoBean
  private JournalpostRepository journalpostRepositoryMock;
  @MockitoBean
  private JournalsakReposistory journalsakReposistoryMock;
  @MockitoBean
  private TokenInformationService tokenInformationServiceMock;

  @BeforeEach
  void timestampCorrelationId() {
    TestUtilsKt.timestampCorrelationIdForThread("journalpostServiceTest");
    when(tilgangskontrollServiceMock.harTilgangTilTema(anyString())).thenReturn(true);

  }


  @Test
  @DisplayName("skal sette dekode på intern dto for journalpost")
  void skalSetteDekodePaDto() {
    when(journalpostRepositoryMock.findById(101)).thenReturn(
        Optional.of(enJournalpost().leggTilSaksnummer("1001001").medBrevkode("innt").hent())
    );

    when(kodeServiceMock.hentBrevKode("innt")).thenReturn(Optional.of(new KodeIntern("innt", "inntekter")));

    var journalpostResponseIntern = journalpostService.hentJournalpost("1001001", 101);

    assertThat(journalpostResponseIntern.getJournalpost())
        .extracting(JournalpostIntern::getBrevkode)
        .isEqualTo(new KodeIntern("innt", "inntekter"));
  }

  @Test
  @DisplayName("skal returnere en journalpost per journalpostId når journal hentes for en sak")
  void skalReturnereEnJournalpostPerJournalpostIdNarJournalForSakHentes() {
    when(kodeServiceMock.skalViseJournalpost(any())).thenReturn(true);

    when(journalsakReposistoryMock.findBySaksnummer("101")).thenReturn(List.of(
        enJournalsak()
            .medSaksnummer("101")
            .med(enJournalpost().medJournalpostId(1).medFagomrade("BNR"))
            .bygg(),
        enJournalsak()
            .medSaksnummer("101")
            .med(enJournalpost().medJournalpostId(1).medFagomrade("BNR"))
            .bygg(),
        enJournalsak()
            .medSaksnummer("101")
            .med(enJournalpost().medJournalpostId(1).medFagomrade("BNR"))
            .bygg(),
        enJournalsak()
            .medSaksnummer("101")
            .med(enJournalpost().medJournalpostId(2).medFagomrade("BNR"))
            .bygg()
    ));

    List<JournalpostIntern> saksjournal = journalpostService.hentJournal(new Sakjournal("101", Fagomrade.BIDRAG));

    assertThat(saksjournal).hasSize(2);
  }

  @Test
  @DisplayName("skal journalføre journalposter")
  void skalJournalforeJournalposter() {
    int id101 = 101;
    var drAcula = "Dr. A. Cula";
    var meg = "meg";
    var ytreIndre = "ytre indre";
    var z123 = "z123";

    when(journalpostRepositoryMock.findById(id101)).thenReturn(
        Optional.of(enJournalpost().medJournalpostId(id101).medJournalstatus(Journalstatus.MOTTAKSREGISTRERT).leggTilSaksnummer("1001").hent())
    );

    when(tokenInformationServiceMock.hentSaksbehandlersBrukerid()).thenReturn(z123);
    when(tokenInformationServiceMock.hentSaksbehandlersNavn()).thenReturn(drAcula);

    Optional<JournalpostIntern> muligEndretJournalpost = journalpostService.endre(
        enCommandBuilder()
            .medJournalpostId(id101)
            .medEnhet(ytreIndre)
            .medGjelder(meg)
            .medSkalJournalfores()
            .tilEndreJournalpostCommandIntern()
    );

    assertThat(muligEndretJournalpost).hasValueSatisfying(journalpostIntern -> {
      assertThat(journalpostIntern).as("bruker id").extracting(JournalpostIntern::getBrukerid).isEqualTo(z123);
      assertThat(journalpostIntern).as("gjelder").extracting(JournalpostIntern::getGjelderAktor).isEqualTo(new AktorIntern(meg));
      assertThat(journalpostIntern).as("journalførende enhet").extracting(JournalpostIntern::getJournalforendeEnhet).isEqualTo(ytreIndre);
      assertThat(journalpostIntern).as("journalført av").extracting(JournalpostIntern::getJournalfortAv).isEqualTo(drAcula);
      assertThat(journalpostIntern).as("journalstatus").extracting(JournalpostIntern::getJournalstatus).isEqualTo(Journalstatus.JOURNALFORT);
    });
  }
}
