package no.nav.bidrag.dokument.journalpost.consumer;

import java.util.Optional;
import no.nav.bidrag.dokument.journalpost.UrlsForApplication;
import no.nav.bidrag.dokument.journalpost.model.Enhet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

public class NorgConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(NorgConsumer.class);

  private final RestTemplate restTemplate;

  public NorgConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public Optional<Enhet> hentEnhetsinformasjon(String enhetsnummer) {
    LOGGER.info("Henter enhetsinformasjon med enhetsnummer: {}", enhetsnummer);

    try {
      var enhetResponse = restTemplate.exchange("/enhet/" + enhetsnummer, HttpMethod.GET, null, Enhet.class);

      if (enhetResponse.getBody() == null) {
        LOGGER.warn("Fant ingen enhetsinformasjon for {}, httpStatus {}", enhetsnummer, enhetResponse.getStatusCode());
      }

      return Optional.ofNullable(enhetResponse.getBody());
    } catch (Exception e) {
      LOGGER.error("Feilet ved henting av enhetsinformasjon for enhetsnummer {}", enhetsnummer, e);

      return Optional.empty();
    }
  }
}
