package no.nav.bidrag.dokument.journalpost.service;

import static java.util.Collections.singletonList;
import static no.nav.bidrag.dokument.journalpost.AvvikshendelseBuilder.enAvvikshendelse;
import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.TEST;
import static no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger.enJournalpost;
import static no.nav.bidrag.dokument.journalpost.model.JournalHendelseBygger.enJournalHendelse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.LocalDateTime;
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest;
import no.nav.bidrag.dokument.journalpost.TestDataManager;
import no.nav.bidrag.dokument.journalpost.entity.JournalHendelse;
import no.nav.bidrag.dokument.journalpost.exception.CharacterOverflowException;
import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostKafkaEventProducer;
import no.nav.bidrag.dokument.journalpost.model.Avvikstype;
import no.nav.bidrag.dokument.journalpost.model.Fagomrade;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@ActiveProfiles(TEST)
@DisplayName("JournalHendelseService")
@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BidragDokumentJournalpostLocalTest.class)
@EnableWireMock(value = @ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
class HendelseServiceTest {

  @Autowired
  private HendelseService hendelseService;
  @Autowired
  private TestDataManager testDataManager;
  @MockitoBean
  private JournalpostKafkaEventProducer journalpostKafkaEventProducerMock;

  @Nested
  @DisplayName("for behandling av avvik")
  class AvviksDetaljer {

    @BeforeEach
    void slettEksisterendeJournalHendelser() {
      testDataManager.slettJournalhendelser();
    }

    @ParameterizedTest
    @EnumSource(Avvikstype.class)
    @DisplayName("skal lage opprettet tidspunkt på hendelse")
    void skalLageOpprettetTidspunkt(Avvikstype avvikstype) {
      var beforeTime = LocalDateTime.now();
      var journalpost = testDataManager.opprett(enJournalpost().medJournalforendeEnhet("1234"));

      hendelseService.lagHendelseFor(
          enJournalHendelse()
              .med(journalpost)
              .med(enAvvikshendelse().med(avvikstype).medNyttFagomrade(Fagomrade.BIDRAG).medEnhetsnummer("1234").medGammeltEnhetsnummer("1234"))
              .bygg()
      );

      var journalHendelser = testDataManager.lesJournalHendelser(journalpost.getJournalpostId());

      assertThat(journalHendelser).as("hendelse opprettet").hasSize(1);
      assertThat(journalHendelser.get(0).getOpprettet()).as("opprettet tidspunkt")
          .isAfter(beforeTime)
          .isBefore(LocalDateTime.now());
    }

    @ParameterizedTest
    @EnumSource(Avvikstype.class)
    @DisplayName("skal legge brukerident til saksbehandler")
    void skalLeggeTilBrukerident(Avvikstype avvikstype) {
      var journalpost = testDataManager.opprett(enJournalpost().medJournalforendeEnhet("1234"));

      hendelseService.lagHendelseFor(
          enJournalHendelse()
              .med(journalpost)
              .med(enAvvikshendelse().med(avvikstype).medNyttFagomrade(Fagomrade.BIDRAG).medGammeltEnhetsnummer("1234"))
              .medBrukerident("jb007")
              .bygg()
      );

      var journalHendelser = testDataManager.lesJournalHendelser(journalpost.getJournalpostId());

      assertThat(journalHendelser).as("hendelse opprettet").hasSize(1);
      assertThat(journalHendelser.get(0).getBrukerIdent()).as("brukerident").isEqualTo("jb007");
    }

    @ParameterizedTest
    @EnumSource(Avvikstype.class)
    @DisplayName("skal legge til enhet når journalhendelse opprettes")
    void skalLeggeTilEnhetForHendelsen(Avvikstype avvikstype) {
      var journalpost = testDataManager.opprett(enJournalpost().medJournalforendeEnhet("1234"));

      hendelseService.lagHendelseFor(
          enJournalHendelse()
              .med(journalpost)
              .med(enAvvikshendelse().med(avvikstype).medOpprettetAvEnhet("1771").medNyttFagomrade(Fagomrade.BIDRAG).medGammeltEnhetsnummer("1234"))
              .bygg()
      );

      var journalHendelser = testDataManager.lesJournalHendelser(journalpost.getJournalpostId());

      assertThat(journalHendelser).as("opprettet av enhet").extracting(JournalHendelse::getEnhet)
          .isEqualTo(singletonList("1771"));
    }

    @ParameterizedTest
    @EnumSource(value = Avvikstype.class, names = "BESTILL_.*", mode = EnumSource.Mode.MATCH_ANY)
    @DisplayName("skal opprette hendelse for avvik")
    void skalOppretteDoctypeEndretForAvvik(Avvikstype avvikstype) {
      var journalpost = testDataManager.opprett(enJournalpost());
      hendelseService.lagHendelseFor(
          enJournalHendelse()
              .med(journalpost)
              .med(enAvvikshendelse().med(avvikstype))
              .bygg()
      );

      var journalHendelsaer = testDataManager.lesJournalHendelser(journalpost.getJournalpostId());

      assertThat(journalHendelsaer).as("hendelse").extracting(JournalHendelse::getHendelse)
          .isEqualTo(singletonList("AVVIK_" + avvikstype.name()));
    }

    @ParameterizedTest
    @EnumSource(value = Avvikstype.class, names = "BESTILL_.*", mode = EnumSource.Mode.MATCH_ANY)
    @DisplayName("skal legge til brukers beskrivelse når et avvik behandles")
    void skalLeggeBrukersBeskrivelseSammenMedAvviketsBeskrivelseTilHendelse(Avvikstype avvikstype) {
      var journalpost = testDataManager.opprett(enJournalpost());
      hendelseService.lagHendelseFor(
          enJournalHendelse()
              .med(journalpost)
              .med(enAvvikshendelse().med(avvikstype).medBeskrivelse("This is Sparta!"))
              .bygg()
      );

      var muligBeskrivelse = testDataManager.lesJournalHendelser(journalpost.getJournalpostId()).stream()
          .map(JournalHendelse::getBeskrivelse)
          .findFirst();

      assertThat(muligBeskrivelse).as("beskrivelse").isPresent()
          .hasValueSatisfying(string -> assertThat(string).containsSequence("This is Sparta!"));
    }

    @ParameterizedTest
    @EnumSource(value = Avvikstype.class, names = {"BESTILL_RESKANNING", "BESTILL_SPLITTING"})
    @DisplayName("skal lagre journalhendelse med beskrivelse på 1000 tegn")
    void skalLagreJournalHendelseMedBeskrivelsePaTusenTegn(Avvikstype avvikstype) {
      var beskrivelseFraBruker = StringUtils.rightPad("This is Sparta!", 1000, 'x');
      var journalpost = testDataManager.opprett(enJournalpost());

      hendelseService.lagHendelseFor(
          enJournalHendelse()
              .med(journalpost)
              .med(enAvvikshendelse().med(avvikstype).medBeskrivelse(beskrivelseFraBruker))
              .bygg()
      );

      var journalHendelsaer = testDataManager.lesJournalHendelser(journalpost.getJournalpostId());

      assertThat(journalHendelsaer).as("breskrivelse").extracting(JournalHendelse::getBeskrivelse)
          .isEqualTo(singletonList(beskrivelseFraBruker));
    }

    @ParameterizedTest
    @EnumSource(value = Avvikstype.class, names = "BESTILL_.*", mode = EnumSource.Mode.MATCH_ANY)
    @DisplayName("skal ikke lagre journalhendelse med beskrivelse på 1001 tegn")
    void skalIkkeLagreJournalHendelseMedBeskrivelsePaTusenOgEtTegn(Avvikstype avvikstype) {
      var beskrivelseFraBruker = StringUtils.rightPad("This is Sparta!", 1001, 'x');
      var journalpost = testDataManager.opprett(enJournalpost());
      var journalhendelse = enJournalHendelse()
          .med(journalpost)
          .med(enAvvikshendelse().med(avvikstype).medBeskrivelse(beskrivelseFraBruker))
          .bygg();

      assertThatExceptionOfType(CharacterOverflowException.class).isThrownBy(() -> hendelseService.lagHendelseFor(journalhendelse))
          .withMessage("Beskrivelse kan max være 1000 tegn!");
    }
  }
}
