package no.nav.bidrag.dokument.journalpost;

import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.SECURED_TEST;
import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.TEST;

import com.nimbusds.jose.JOSEObjectType;
import java.util.List;
import java.util.Map;
import no.nav.bidrag.commons.util.CustomJacksonHttpMessageConverter;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@Configuration
@Profile({TEST, SECURED_TEST})
@EnableWireMock(@ConfigureWireMock(port = 0))
@Lazy
public class TestRestTemplateConfiguration {

  @LocalServerPort
  private int port;
  @Autowired
  private MockOAuth2Server mockOAuth2Server;
  @Bean
  TestRestTemplate httpHeaderTestRestTemplate() {

    return new TestRestTemplate(new RestTemplateBuilder()
        .rootUri("http://localhost:"+port+"/bidrag-dokument-journalpost")
        .defaultMessageConverters()
        .additionalMessageConverters(new CustomJacksonHttpMessageConverter())
        .additionalInterceptors((request, body, execution) -> {
          request.getHeaders().add(HttpHeaders.AUTHORIZATION, generateBearerToken());
          return execution.execute(request, body);
        }));
  }


  private String generateBearerToken() {
    var iss = mockOAuth2Server.issuerUrl("aad");
    var newIssuer = iss.newBuilder().host("localhost").build();
    var token = mockOAuth2Server.issueToken("aad", "aud-localhost", new DefaultOAuth2TokenCallback("aad", "aud-localhost", JOSEObjectType.JWT.getType(), List.of("aud-localhost"), Map.of("iss", newIssuer.toString()), 3600));
    return "Bearer " + token.serialize();
  }
}
