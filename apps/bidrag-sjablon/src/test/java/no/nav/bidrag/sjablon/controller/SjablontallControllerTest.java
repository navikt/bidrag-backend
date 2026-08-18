package no.nav.bidrag.sjablon.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.List;
import no.nav.bidrag.sjablon.BidragSjablon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(
    classes = {BidragSjablon.class, RestTemplate.class},
    webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("SjablontallController")
class SjablontallControllerTest {

  @LocalServerPort private int port;

  @Autowired private RestTemplate restTemplate;

  @Test
  @DisplayName("skal hente sjablontall sjabloner")
  void skalHenteSjablontallSjabloner() {

    var listeResponse =
        restTemplate.exchange(
            "http://localhost:" + port + "/bidrag-sjablon/sjablontall",
            HttpMethod.GET,
            null,
            List.class);

    assertAll(
        () ->
            assertThat(listeResponse.getStatusCode())
                .as("listeResponse.statusCode")
                .isEqualTo(HttpStatus.OK));
  }
}
