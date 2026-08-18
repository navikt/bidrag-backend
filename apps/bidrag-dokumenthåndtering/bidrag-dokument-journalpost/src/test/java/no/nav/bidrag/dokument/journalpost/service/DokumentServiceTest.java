package no.nav.bidrag.dokument.journalpost.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import no.nav.bidrag.dokument.journalpost.configuration.BidragDokumentJournalpostConfig.SaksbehandlerOidcTokenManager;
import no.nav.bidrag.dokument.journalpost.dokument.DokumentConsumer;
import no.nav.bidrag.dokument.journalpost.dokument.DokumentTilgangConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import no.nav.bidrag.dokument.journalpost.consumer.BrevserverConsumer;
import org.springframework.jms.core.JmsTemplate;

@DisplayName("DokumentService")
@ExtendWith(MockitoExtension.class)
class DokumentServiceTest {

  private DokumentTilgangConsumer dokumentTilgangConsumer;
  private DokumentService dokumentService;
  @Mock
  private JmsTemplate jmsTemplateMock;
  @Mock
  private SaksbehandlerOidcTokenManager saksbehandlerOidcTokenManagerMock;
  @Mock
  private DokumentConsumer dokumentConsumer;
  @Mock
  private JournalpostService journalpostService;
  @Mock
  private BrevserverConsumer brevserverConsumer;
  @BeforeEach
  void initDokumentService() {
    dokumentTilgangConsumer = new DokumentTilgangConsumer(
     "brevSys", "password",
        jmsTemplateMock, saksbehandlerOidcTokenManagerMock);

    dokumentService = new DokumentService(
        "http://nav.no/brevserverUrl", "brevSys",
        journalpostService, dokumentTilgangConsumer, dokumentConsumer, brevserverConsumer, false);
  }

  @BeforeEach
  void mockSaksbehandlerOidcTokenManager() {
    when(saksbehandlerOidcTokenManagerMock.hentSaksbehandler()).thenReturn("jactor");
  }


  @Test
  @DisplayName("skal gi 'encoded' mbdok-url tilbake til kaller av tjeneste")
  void skalGiEncodedMbdokUrlTilKallerAvTjeneste() {
    var muligDokumentUrlDto = Optional.ofNullable(dokumentService.lagTilgangUrl("dokumentreferanse"));

    assertThat(muligDokumentUrlDto).hasValueSatisfying(dokumentUrlDto -> assertAll(
        () -> assertThat(dokumentUrlDto.getDokumentUrl()).startsWith("mbdok://brevklient/system/brevSys/dokument/dokumentreferanse"),
        () -> assertThat(dokumentUrlDto.getDokumentUrl()).contains("?token=dokumentreferanse-"),
        () -> assertThat(dokumentUrlDto.getDokumentUrl()).contains("http%3A%2F%2Fnav.no%2FbrevserverUrl")
    ));
  }

  @Test
  @DisplayName("skal legge på et token som identifiserer unik dokument url")
  void skalLeggePaToken() {
    var tidsstempel = String.valueOf(System.currentTimeMillis());
    var dokumentUrlDto = dokumentService.lagTilgangUrl("dokRef");

    assertThat(dokumentUrlDto.getDokumentUrl())
        .contains("?token=dokRef-" + tidsstempel.substring(0, tidsstempel.length() - 2));
  }

  @Test
  @Disabled
  @DisplayName("skal sende en jms melding (som xml)")
  void skalSendeXmlMelding() {
    var currentTimeInMs = String.valueOf(System.currentTimeMillis());
    var messageCaptor = ArgumentCaptor.forClass(Object.class);

    dokumentService.lagTilgangUrl("dokRef");

    verify(jmsTemplateMock).convertAndSend(messageCaptor.capture());
    var melding = String.valueOf(messageCaptor.getValue());

    assertAll(
        () -> assertThat(melding).contains("saksbehandler=\"jactor\""),
        () -> assertThat(melding).contains("passord=\"sysPass\""),
        () -> assertThat(melding).contains("klientToken=\"dokRef-" + currentTimeInMs.substring(0, currentTimeInMs.length() - 3)),
        () -> assertThat(melding).contains("<brev  brevref=\"dokRef\"/>"),
        () -> assertThat(melding).contains("sysid=\"brevSys\"")
    );
  }

}