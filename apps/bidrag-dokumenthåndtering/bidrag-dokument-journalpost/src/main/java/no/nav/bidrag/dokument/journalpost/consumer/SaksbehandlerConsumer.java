package no.nav.bidrag.dokument.journalpost.consumer;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpost.SECURE_LOGGER;
import static no.nav.bidrag.dokument.journalpost.configuration.CacheConfig.SAKSBEHANDLER_CACHE;

import java.util.Optional;
import no.nav.bidrag.dokument.journalpost.dto.Saksbehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class SaksbehandlerConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(SaksbehandlerConsumer.class);

  private final RestTemplate restTemplate;

  public SaksbehandlerConsumer(RestTemplate restTemplate){
    this.restTemplate = restTemplate;
  }

  @SuppressWarnings("ConstantConditions")
  @Cacheable(SAKSBEHANDLER_CACHE)
  public Optional<Saksbehandler> hentSaksbehandler(String ident) {
    if (ident != null && ident.startsWith("srv")){
      LOGGER.info("hentSaksbehandler: Bruker er systembruker ({})", ident);
      return Optional.of(new Saksbehandler(ident, ident));
    }

    var saksbehandlerResponse = restTemplate.exchange("/saksbehandler/info/" + ident, HttpMethod.GET, null, Saksbehandler.class);

    if (saksbehandlerResponse == null || !saksbehandlerResponse.hasBody()){
      LOGGER.warn("Fikk ingen saksbehandlerinformasjon for ident {}", ident);
      return Optional.empty();
    }

    SECURE_LOGGER.info("Hentet saksbehandlerinformasjon: {}", saksbehandlerResponse.getBody());

    return Optional.of(saksbehandlerResponse.getBody());
  }
}
