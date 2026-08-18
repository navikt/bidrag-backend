package no.nav.bidrag.dokument.journalpost.service;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.TEST;
import static no.nav.bidrag.dokument.journalpost.service.TokenInformationService.SAKSBEHANDLER_NAVN_UKJENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import no.nav.bidrag.dokument.journalpost.configuration.BidragDokumentJournalpostConfig.SaksbehandlerOidcTokenManager;
import no.nav.bidrag.dokument.journalpost.consumer.SaksbehandlerConsumer;
import no.nav.bidrag.dokument.journalpost.dto.Saksbehandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("TokenInformationService")
@ActiveProfiles(TEST)
@ExtendWith(MockitoExtension.class)
public class TokenInformationServiceTest {

  @Mock
  private SaksbehandlerConsumer saksbehandlerConsumerMock;

  @Mock
  private SaksbehandlerOidcTokenManager saksbehandlerOidcTokenManagerMock;

  @InjectMocks
  private TokenInformationService tokenInformationService;

  @Test
  @DisplayName("Skal hente saksbehandlersbrukerid fra token under alle omstedigheter")
  void skalHenteSaksbehandlersBrukeridFraTokenUnderAlleOmstedigheter() {
    var saksbehandlersBrukerid = "s123456";

    when(saksbehandlerOidcTokenManagerMock.hentSaksbehandler()).thenReturn(saksbehandlersBrukerid);

    var hentetBrukerid = tokenInformationService.hentSaksbehandlersBrukerid();

    assertThat(saksbehandlersBrukerid).isEqualTo(hentetBrukerid);
  }

  @Test
  @DisplayName("Skal hente saksbehandlers navn dersom dette er kjent")
  void skalHenteSaksbehandlersNavnDersomDetteErkjent() {
    var saksbehandlersBrukerid = "s123456";
    var saksbehandler = new Saksbehandler(saksbehandlersBrukerid, "Pelle Parafin");

    when(saksbehandlerOidcTokenManagerMock.hentSaksbehandler()).thenReturn(saksbehandlersBrukerid);
    when(saksbehandlerConsumerMock.hentSaksbehandler(saksbehandlersBrukerid)).thenReturn(Optional.of(saksbehandler));

    var saksbehandlersNavn = tokenInformationService.hentSaksbehandlersNavn();

    assertThat(saksbehandler).extracting(Saksbehandler::getNavn).isEqualTo(saksbehandlersNavn);
  }

  @Test
  @DisplayName("Skal returnere 'navn ukjent' dersom navn mangler")
  void skalReturnereNavnUkjentDersomNavnMangler() {

    var saksbehandlersBrukerid = "s123456";

    when(saksbehandlerOidcTokenManagerMock.hentSaksbehandler()).thenReturn(saksbehandlersBrukerid);
    when(saksbehandlerConsumerMock.hentSaksbehandler(saksbehandlersBrukerid)).thenReturn(Optional.empty());

    var saksbehandlersNavn = tokenInformationService.hentSaksbehandlersNavn();

    assertThat(saksbehandlersNavn).isEqualTo(SAKSBEHANDLER_NAVN_UKJENT);
  }
}
