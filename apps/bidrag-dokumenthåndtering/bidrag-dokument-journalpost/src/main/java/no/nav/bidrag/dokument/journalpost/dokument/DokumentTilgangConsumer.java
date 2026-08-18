package no.nav.bidrag.dokument.journalpost.dokument;

import io.micrometer.core.annotation.Timed;
import no.nav.bidrag.dokument.journalpost.configuration.BidragDokumentJournalpostConfig.SaksbehandlerOidcTokenManager;
import no.nav.bidrag.dokument.journalpost.dto.Brev;
import no.nav.bidrag.dokument.journalpost.dto.Dokumenttilgang;
import no.nav.bidrag.dokument.journalpost.exception.JmsConsumerException;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public class DokumentTilgangConsumer {
  private static final String DOKUMENTTILGANG_MODUS = "frabrevlager";

  private final String systemId;
  private final String systemPassword;
  private final JmsTemplate jmsTemplate;
  private final SaksbehandlerOidcTokenManager saksbehandlerOidcTokenManager;

  public DokumentTilgangConsumer(
      String systemId,
      String systemPassword,
      JmsTemplate jmsTemplate,
      SaksbehandlerOidcTokenManager saksbehandlerOidcTokenManager
  ) {
    this.systemId = systemId;
    this.systemPassword = systemPassword;
    this.jmsTemplate = jmsTemplate;
    this.saksbehandlerOidcTokenManager = saksbehandlerOidcTokenManager;
  }

  @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 500))
  @Timed("hentDokumentTilgang")
  private void sendMsg(Dokumenttilgang dokumenttilgang) {
    try {
      jmsTemplate.convertAndSend(dokumenttilgang);
      // Legg til en liten delay
      Thread.sleep(500);
    } catch (JmsException | InterruptedException jmsException){
      throw new JmsConsumerException(jmsException);
    }
  }

  @Timed("bestillDokumentTilgang")
  public Dokumenttilgang bestillDokumenttilgang(String dokref){
    var dokumenttilgang = opprettDokumentTilgangRequest(dokref);
    this.sendMsg(dokumenttilgang);
    return dokumenttilgang;
  }

  private Dokumenttilgang opprettDokumentTilgangRequest(String dokref){
    var saksbehandler = saksbehandlerOidcTokenManager.hentSaksbehandler();
    var token = dokref + "-" + System.currentTimeMillis();
    return new Dokumenttilgang(
        saksbehandler,
        systemPassword,
        token,
        systemId,
        DOKUMENTTILGANG_MODUS,
        new Brev(dokref)
    );
  }

}
