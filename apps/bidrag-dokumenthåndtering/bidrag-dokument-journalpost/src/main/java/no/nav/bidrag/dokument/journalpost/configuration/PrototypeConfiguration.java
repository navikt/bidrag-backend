package no.nav.bidrag.dokument.journalpost.configuration;

import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.bidrag.dokument.journalpost.consumer.OppgaveConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class PrototypeConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(PrototypeConfiguration.class);

  @Bean
  @Scope("prototype")
  public HttpHeaderRestTemplate httpHeaderRestTemplate() {
    HttpHeaderRestTemplate httpHeaderRestTemplate = new HttpHeaderRestTemplate(new HttpComponentsClientHttpRequestFactory());
    httpHeaderRestTemplate.addHeaderGenerator(CorrelationIdFilter.CORRELATION_ID_HEADER, CorrelationIdFilter::fetchCorrelationIdForThread);

    return httpHeaderRestTemplate;
  }

  @Bean
  @Scope("prototype")
  public OppgaveConsumer oppgaveConsumer(
      HttpHeaderRestTemplate httpHeaderRestTemplate,
      @Value("${OPPGAVE_OPPGAVER_URL}") String oppgaverUrl
  ) {
    httpHeaderRestTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(oppgaverUrl));

    return new OppgaveConsumer(httpHeaderRestTemplate);
  }
}
