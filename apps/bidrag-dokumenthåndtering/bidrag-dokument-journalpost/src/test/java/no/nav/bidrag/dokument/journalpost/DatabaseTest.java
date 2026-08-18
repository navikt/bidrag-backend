package no.nav.bidrag.dokument.journalpost;

import static no.nav.bidrag.transport.dokument.AvvikType.BESTILL_ORIGINAL;
import static no.nav.bidrag.transport.dokument.AvvikType.BESTILL_RESKANNING;
import static no.nav.bidrag.transport.dokument.AvvikType.BESTILL_SPLITTING;
import static no.nav.bidrag.transport.dokument.AvvikType.ENDRE_FAGOMRADE;
import static no.nav.bidrag.transport.dokument.AvvikType.FEILFORE_SAK;
import static no.nav.bidrag.transport.dokument.AvvikType.INNG_TIL_UTG_DOKUMENT;
import static no.nav.bidrag.dokument.journalpost.AvvikshendelseBuilder.enAvvikshendelse;
import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.TEST;
import static no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger.enJournalfortJournalpost;
import static no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger.enJournalpost;
import static no.nav.bidrag.dokument.journalpost.entity.KodeBrevBygger.enGyldigBrevkode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import jakarta.transaction.Transactional;
import no.nav.bidrag.dokument.journalpost.configuration.BidragDokumentJournalpostConfig.SaksbehandlerOidcTokenManager;
import no.nav.bidrag.dokument.journalpost.consumer.OppgaveConsumer;
import no.nav.bidrag.dokument.journalpost.consumer.SaksbehandlerConsumer;
import no.nav.bidrag.dokument.journalpost.dto.CommandBuilder;
import no.nav.bidrag.dokument.journalpost.dto.JournalpostIntern;
import no.nav.bidrag.dokument.journalpost.dto.KodeIntern;
import no.nav.bidrag.dokument.journalpost.dto.Sakjournal;
import no.nav.bidrag.dokument.journalpost.dto.Saksbehandler;
import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostKafkaEventProducer;
import no.nav.bidrag.dokument.journalpost.model.Avvikstype;
import no.nav.bidrag.dokument.journalpost.model.BehandleAvvikRequest;
import no.nav.bidrag.dokument.journalpost.model.Fagomrade;
import no.nav.bidrag.dokument.journalpost.model.Journalstatus;
import no.nav.bidrag.dokument.journalpost.repository.JournalpostRepository;
import no.nav.bidrag.dokument.journalpost.repository.JournalsakReposistory;
import no.nav.bidrag.dokument.journalpost.service.AvvikService;
import no.nav.bidrag.dokument.journalpost.service.HendelseService;
import no.nav.bidrag.dokument.journalpost.service.JournalpostService;
import no.nav.bidrag.dokument.journalpost.service.TilgangskontrollService;
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
@DisplayName("Test av database")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BidragDokumentJournalpostLocalTest.class)
@Transactional
@EnableWireMock(value = @ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
class DatabaseTest {

  private static final String JOURNALPOST_FOR_BIDRAG = "BID";

  @Autowired
  private AvvikService avvikService;
  @Autowired
  private TestDataManager testDataManager;
  @Autowired
  private JournalpostRepository journalpostRepository;
  @Autowired
  private JournalsakReposistory journalsakReposistory;
  @Autowired
  private JournalpostService journalpostService;

  @MockitoBean
  private HendelseService hendelseServiceMock;
  @MockitoBean
  private JournalpostKafkaEventProducer journalpostKafkaEventProducerMock;
  @MockitoBean
  private OppgaveConsumer oppgaveConsumerMock;

  @MockitoBean
  private TilgangskontrollService tilgangskontrollServiceMock;
  @MockitoBean
  private SaksbehandlerOidcTokenManager saksbehandlerOidcTokenManagerMock;
  @MockitoBean
  private SaksbehandlerConsumer saksbehandlerConsumerMock;

  @BeforeEach
  void initMocks(){
    when(tilgangskontrollServiceMock.harTilgangTilTema(anyString())).thenReturn(true);
  }

  @DisplayName("skal finne journalposter i en bidragssak")
  @Test
  void skalFinneJournalposterFraBidragSak() {
    testDataManager.opprettKodeForJournalstatusSomSkalVises(Journalstatus.JOURNALFORT);
    testDataManager.opprett(enJournalfortJournalpost().leggTilSaksnummer("007").medFagomrade(Fagomrade.BIDRAG_DATABASE).medAvsender("Blund, Jon"));
    var listeMedJournalposter = journalpostService.hentJournal(new Sakjournal("007", JOURNALPOST_FOR_BIDRAG));
    assertThat(listeMedJournalposter).as("journalposter").isNotEmpty();
  }

  @DisplayName("skal hente journalpost fra t_jp og t_jsak")
  @Test
  void skalHenteJournalpost() {
    var journalpost = testDataManager.opprett(enJournalpost().leggTilSaksnummer("007").medBeskrivelse("OMREGNING BIDRAG / INFORMASJONSBREV"));
    var journalpostResponseIntern = journalpostService.hentJournalpost("007", journalpost.getJournalpostId());

    assertThat(journalpostResponseIntern.getJournalpost())
        .extracting(JournalpostIntern::getInnhold)
        .isEqualTo("OMREGNING BIDRAG / INFORMASJONSBREV");
  }

  @DisplayName("skal endre journalpost")
  @Test
  void skalEndreJournalpost() {
    var saksnummer = "0000003";
    var etAnnetSaksnummer = "0000004";
    var saksbehandlerBrukerid = "s123456";
    var saksbehandlerNavn = "Tom Jones";

    testDataManager.opprett(
        enJournalpost()
            .medAvsender("Cula, Dr. A.")
            .medBeskrivelse("Lagres til db2")
            .medDokumentdato(LocalDate.now().minusDays(2))
            .medDokumentreferanse("1001")
            .medDokumentType("N")
            .medFagomrade(Fagomrade.FARSKAP)
            .medGjelder("Guess who!")
            .medJournalfortAv(saksbehandlerNavn)
            .medJournalforendeEnhet("Trygdekontoret")
            .medJournalstatus(Journalstatus.MOTTAKSREGISTRERT)
            .leggTilSaksnummer(saksnummer)
    );

    testDataManager.opprettJournalsakForSaksnummer(etAnnetSaksnummer);

    var journalsaker = journalsakReposistory.findBySaksnummer(saksnummer);
    assertThat(journalsaker).as("journalsaker").isNotEmpty();

    var journalpostId = journalsaker.get(journalsaker.size() - 1).getJournalpost().getJournalpostId();

    var endreJournalpostCommandIntern = new CommandBuilder()
        .medJournalpostId(journalpostId)
        .medAvsender("Mamma", "Mia")
        .medBeskrivelse("Here we go again...")
        .medDokumentDato(LocalDate.now().minusMonths(1))
        .medEnhet("4806")
        .medGjelder("My... My...")
        .medJournaldato(LocalDate.now().plusDays(1))
        .medTilknyttSaker(etAnnetSaksnummer)
        .tilEndreJournalpostCommandIntern();

    when(saksbehandlerOidcTokenManagerMock.hentSaksbehandler()).thenReturn(saksbehandlerBrukerid);
    when(saksbehandlerConsumerMock.hentSaksbehandler(saksbehandlerBrukerid))
        .thenReturn(Optional.of(new Saksbehandler(saksbehandlerBrukerid, saksbehandlerNavn)));

    var endretJournalpost = journalpostService.endre(endreJournalpostCommandIntern);
    assertThat(endretJournalpost).isPresent();

    testDataManager.enforceReadFromDatabase();

    var journalpost = journalpostRepository.findById(journalpostId)
        .orElseThrow(() -> new IllegalStateException("Fant ikke journalpost med id: " + journalpostId));

    assertAll(
        () -> assertThat(journalpost.getAvsender()).as("avsender").isEqualTo("Mamma"),
        () -> assertThat(journalpost.getAvsenderFornavn()).as("avsender fornavn").isEqualTo("Mia"),
        () -> assertThat(journalpost.getBeskrivelse()).as("beskrivelse").isEqualTo("Here we go again..."),
        () -> assertThat(journalpost.getDokumentdato()).as("dokumentdato").isEqualTo(LocalDate.now().minusMonths(1)),
        () -> assertThat(journalpost.getDokumentreferanse()).as("dokumentreferanse").isEqualTo("1001"),
        () -> assertThat(journalpost.getDokumentType()).as("dokumentType").isEqualTo("N"),
        () -> assertThat(journalpost.getFagomrade()).as("fagomrade").isEqualTo(Fagomrade.FARSKAP),
        () -> assertThat(journalpost.getGjelder()).as("gjelder").startsWith("My... My..."),
        () -> assertThat(journalpost.getJournalfortAv()).as("journalfortAv").isEqualTo(saksbehandlerNavn),
        () -> assertThat(journalpost.getJournalpostId()).as("journalpostId").isNotNull(),
        () -> assertThat(journalpost.getJournalsaker()).as("journalsaker").hasSize(2)
    );
  }

  @Test
  @DisplayName("skal lese dokumentType og journalstatus fra database")
  void skalLeseDokumentTypeOgJournalstatusFraDatabase() {
    testDataManager.opprettKodeForJournalstatusSomSkalVises(Journalstatus.JOURNALFORT);
    testDataManager.opprett(
        enJournalfortJournalpost().leggTilSaksnummer("007").medFagomrade(Fagomrade.BIDRAG_DATABASE).medAvsender("Blund, Jon").medDokumentType("DOKTYPE")
    );

    var listeMedJournalposter = journalpostService.hentJournal(new Sakjournal("007", JOURNALPOST_FOR_BIDRAG));
    assertThat(listeMedJournalposter).as("journalposter").isNotEmpty();

    listeMedJournalposter.forEach(journalpostDto -> assertThat(journalpostDto.getDokumentType()).as("dokumentType").isNotNull());
  }

  @Test
  @DisplayName("skal endre dokumentdato til en journalpost")
  void skalEndreDokumentdatoTilJournalpost() {
    var saksnummer = "0000003";
    var saksbehandlerBrukerid = "s123456";
    var saksbehandlerNavn = "Tom Jones";

    testDataManager.opprett(
        enJournalpost()
            .medAvsender("Cula, Dr. A.")
            .medBeskrivelse("Lagres til db2")
            .medDokumentdato(LocalDate.now())
            .medDokumentreferanse("1001")
            .medDokumentType("N")
            .medFagomrade(Fagomrade.FARSKAP)
            .medGjelder("Guess who!")
            .leggTilSaksnummer(saksnummer)
    );

    testDataManager.enforceReadFromDatabase();

    var journalsaker = journalsakReposistory.findBySaksnummer(saksnummer);
    assertThat(journalsaker).as("journalsaker").isNotEmpty();

    var journalpostId = journalsaker.get(journalsaker.size() - 1).getJournalpost().getJournalpostId();
    var endreJournalpostCommandIntern = new CommandBuilder()
        .medJournalpostId(journalpostId)
        .medEnhet("4806")
        .medDokumentDato(LocalDate.now().minusMonths(1))
        .tilEndreJournalpostCommandIntern();

    when(saksbehandlerOidcTokenManagerMock.hentSaksbehandler()).thenReturn(saksbehandlerBrukerid);
    when(saksbehandlerConsumerMock.hentSaksbehandler(saksbehandlerBrukerid))
        .thenReturn(Optional.of(new Saksbehandler(saksbehandlerBrukerid, saksbehandlerNavn)));

    var endretJournalpost = journalpostService.endre(endreJournalpostCommandIntern);
    assertThat(endretJournalpost).isPresent();

    testDataManager.enforceReadFromDatabase();

    var muligJournalpost = journalpostRepository.findById(journalpostId);

    assertThat(muligJournalpost).hasValueSatisfying(
        journalpost -> assertThat(journalpost.getDokumentdato()).isEqualTo(LocalDate.now().minusMonths(1))
    );
  }

  @Test
  @DisplayName("skal hente brevkode fra database når journalpost hentes med sakstilknytning")
  void skalHenteBrevkodeFraDbNarJournalpostHentesMedSakstilknytning() {
    String langt = "LONG";

    testDataManager.opprett(enGyldigBrevkode().medKode(langt).medDekode("Langt brev"));
    var journalpost = testDataManager.opprett(
        enJournalpost().leggTilSaksnummer("007").medFagomrade(Fagomrade.BIDRAG_DATABASE).medAvsender("Blund, Jon").medBrevkode(langt)
    );

    var response = journalpostService.hentJournalpost("007", journalpost.getJournalpostId());

    assertAll(
        () -> assertThat(response.getJournalpost()).as("journalpost").isNotNull(),
        () -> assertAll(
            () -> assertThat(response.getJournalpost()).extracting(JournalpostIntern::getBrevkode).as("brevkode").isNotNull(),
            () -> assertThat(response.getJournalpost())
                .extracting(JournalpostIntern::getBrevkode).as("brevkode")
                .extracting(KodeIntern::getDekode).as("dekode").isEqualTo("Langt brev")
        )
    );
  }

  @Test
  @DisplayName("skal hente brevkode fra database når journalpost hentes uten sakstilknytning")
  void skalHenteBrevkodeFraDbNarJournalpostHentesUtenSakstilknytning() {
    String langt = "LONG";

    testDataManager.opprett(enGyldigBrevkode().medKode(langt).medDekode("Langt brev"));
    var journalpost = testDataManager.opprett(
        enJournalpost().leggTilSaksnummer("007").medFagomrade(Fagomrade.BIDRAG_DATABASE).medAvsender("Blund, Jon").medBrevkode(langt)
    );

    var response = journalpostService.hentJournalpost(journalpost.getJournalpostId());

    assertAll(
        () -> assertThat(response.getJournalpost()).as("journalpost").isNotNull(),
        () -> assertAll(
            () -> assertThat(response.getJournalpost()).extracting(JournalpostIntern::getBrevkode).as("brevkode").isNotNull(),
            () -> assertThat(response.getJournalpost())
                .extracting(JournalpostIntern::getBrevkode).as("brevkode")
                .extracting(KodeIntern::getDekode).as("dekode").isEqualTo("Langt brev")
        )
    );
  }

  @Test
  @DisplayName("skal oppdatere database når avvik ENDRE_FAGOMRADE utføres")
  void skalOppdatereDatabaseNarEndreAvvikUtfores() {
    var saksnummer = String.valueOf(((LocalDate.now().getYear() % 100) * 100000) - 1);
    var journalpost = testDataManager.opprett(
        enJournalpost()
            .medFagomrade(JOURNALPOST_FOR_BIDRAG)
            .medJournalforendeEnhet("4806")
            .leggTilSaksnummer(saksnummer)
    );

    testDataManager.enforceReadFromDatabase();

    var behandleAvvikRequest = new BehandleAvvikRequest(
        enAvvikshendelse()
            .med(Avvikstype.ENDRE_FAGOMRADE)
            .medNyttFagomrade("FAR")
            .medJournalpostId(journalpost.getJournalpostId())
            .medSaksnummer(saksnummer)
            .byggAvvikshendelseIntern()
    );

    var behandleAvvikResponse = avvikService.behandleAvvik(behandleAvvikRequest);

    assertThat(behandleAvvikResponse.erUgyldig()).as("fikk ugyldig avviksbehandling").isEqualTo(false);

    testDataManager.enforceReadFromDatabase();

    var journalpostResponseIntern = journalpostService.hentJournalpost(saksnummer, journalpost.getJournalpostId());

    assertThat(journalpostResponseIntern.getJournalpost()).as("journalpost fra database")
        .extracting(JournalpostIntern::getFagomrade)
        .isEqualTo(Fagomrade.FARSKAP);
  }

  @Test
  @DisplayName("skal hente avvik for journalpost")
  void skalHenteAvvikForJournalpost() {
    testDataManager.opprettKodeForJournalstatusSomSkalVises(Journalstatus.JOURNALFORT);
    var journalpost = testDataManager.opprett(
        enJournalfortJournalpost()
            .medBatchNavn("batchen")
            .medDokumentType("I")
            .medFagomrade(Fagomrade.BIDRAG)
            .medFilnavn("doc.pdf")
            .leggTilSaksnummer("007")
            .medSkannetDato(LocalDate.of(2019, 8, 21))
            .utenOrginalBestilt()
    );

    testDataManager.enforceReadFromDatabase();

    var finnAvvik = avvikService.finnAvvik(journalpost.getJournalpostId(), "007");

    assertThat(finnAvvik.hentListeMedAvvik()).contains(
        BESTILL_ORIGINAL, BESTILL_RESKANNING, BESTILL_SPLITTING, ENDRE_FAGOMRADE, FEILFORE_SAK, INNG_TIL_UTG_DOKUMENT
    );
  }
}
