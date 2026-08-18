package no.nav.bidrag.dokument.journalpost.consumer;

import java.util.Optional;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.dokument.journalpost.dto.Oppgave;
import no.nav.bidrag.dokument.journalpost.dto.OpprettOppgaveResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("ConstantConditions")
public class OppgaveConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(OppgaveConsumer.class);

  private final RestTemplate restTemplate;

  public OppgaveConsumer(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public HttpResponse<OpprettOppgaveResponse> opprett(Oppgave oppgave) {

    var responseEntity = restTemplate.postForEntity("/", new HttpEntity<>(oppgave), OpprettOppgaveResponse.class);
    Optional.ofNullable(responseEntity.getBody())
        .ifPresentOrElse(opprettetOppgave->LOGGER.info("Oppgave opprettet {}", opprettetOppgave), () -> LOGGER.error("Det skjedde en feil opprettelse av oppgave {}", oppgave));

    return new HttpResponse<>(responseEntity);
  }

  public void leggTilSikkerhet(ClientHttpRequestInterceptor interceptor) {
    if (restTemplate instanceof HttpHeaderRestTemplate) {
      restTemplate.getInterceptors().add(interceptor);
    }
  }
}
