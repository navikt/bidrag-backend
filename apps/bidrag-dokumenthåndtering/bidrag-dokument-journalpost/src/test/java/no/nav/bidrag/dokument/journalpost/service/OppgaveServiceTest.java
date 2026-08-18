package no.nav.bidrag.dokument.journalpost.service;

import static no.nav.bidrag.dokument.journalpost.AvvikshendelseBuilder.enAvvikshendelse;
import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.TEST;
import static no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger.enJournalpost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest;
import no.nav.bidrag.dokument.journalpost.dto.Oppgave;
import no.nav.bidrag.dokument.journalpost.dto.OpprettOppgaveResponse;
import no.nav.bidrag.dokument.journalpost.entity.Journalpost;
import no.nav.bidrag.dokument.journalpost.model.Avvikstype;
import no.nav.bidrag.dokument.journalpost.model.DokumentType;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@ActiveProfiles(TEST)
@DisplayName("OppgaveService")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BidragDokumentJournalpostLocalTest.class)
@EnableWireMock(value = @ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
class OppgaveServiceTest {

  @Autowired
  private OppgaveService oppgaveService;

  @MockitoBean
  private HttpHeaderRestTemplate restTemplateMock;

  @Test
  @DisplayName("skal opprette en oppgave for Avvikstype.BESTILL_ORIGINAL")
  void skalOppretteOppgaveForBestillOrginal() {
    Journalpost journalpost = enJournalpost()
        .medJournalpostId(1001)
        .medDokumentreferanse("dokref")
        .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
        .medSkannetDato(LocalDate.now())
        .medBatchNavn("Andre Bidrag")
        .leggTilSaksnummer("dokref")
        .utenOrginalBestilt()
        .hent();

    when(restTemplateMock.postForEntity(anyString(), any(HttpEntity.class), eq(OpprettOppgaveResponse.class)))
        .thenReturn(new ResponseEntity<>(HttpStatus.I_AM_A_TEAPOT));

    var avviksbehandling = journalpost.startAvviksbehandling(
        enAvvikshendelse().med(Avvikstype.BESTILL_ORIGINAL)
            .medSaksnummer("avviksak")
            .medOpprettetAvEnhet("1001").byggAvvikshendelseIntern()
    );

    oppgaveService.opprettOppgave(avviksbehandling);

    @SuppressWarnings("unchecked") ArgumentCaptor<HttpEntity<Oppgave>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplateMock).postForEntity(eq("/"), entityCaptor.capture(), eq(OpprettOppgaveResponse.class));
    var oppgave = entityCaptor.getValue().getBody();

    assertThat(oppgave).as("oppgave").isNotNull();

    assertAll(
        () -> assertThat(oppgave.getJournalpostId()).as("oppgave.journalpostId").isEqualTo(String.valueOf(journalpost.getJournalpostId())),
        () -> assertThat(oppgave.getSaksreferanse()).as("oppgave.saksreferanse").isEqualTo("avviksak"),
        () -> assertThat(oppgave.getBeskrivelse()).as("oppgave.beskrivelse (generell)")
            .contains("Originalbestilling: Vi ber om å få tilsendt papirdokumentet av vedlagte skannede dokument, se link."),
        () -> assertThat(oppgave.getBeskrivelse()).as("oppgave.beskrivelse (skannet dato)")
            .contains("Dokumentet ble skannet " + LocalDate.now()),
        () -> assertThat(oppgave.getBeskrivelse()).as("oppgave.beskrivelse (batch navn)")
            .contains("med batchnavnet Andre Bidrag."),
        () -> assertThat(oppgave.getOpprettetAvEnhetsnr()).as("oppgave.opprettetAvEnhetsnr").isEqualTo("1001")
    );
  }
}
