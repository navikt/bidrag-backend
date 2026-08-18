package no.nav.bidrag.dokument.journalpost.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.SECURED_TEST;
import static no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles.TEST;
import static no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger.enMottaksregistrertJournalpost;
import static no.nav.bidrag.dokument.journalpost.utils.TestUtilsKt.initHttpEntity;
import static no.nav.bidrag.dokument.journalpost.utils.TestUtilsKt.prefixId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import no.nav.bidrag.commons.web.EnhetFilter;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest;
import no.nav.bidrag.dokument.journalpost.TestDataManager;
import no.nav.bidrag.dokument.journalpost.hendelse.JournalpostKafkaEventProducer;
import no.nav.bidrag.dokument.journalpost.model.DokumentType;
import no.nav.bidrag.dokument.journalpost.model.Fagomrade;
import no.nav.bidrag.dokument.journalpost.model.Journalstatus;
import no.nav.bidrag.dokument.journalpost.utils.CustomHeader;
import no.nav.bidrag.transport.dokument.BehandleAvvikshendelseResponse;
import no.nav.bidrag.transport.dokument.JournalpostHendelse;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@ActiveProfiles({TEST, SECURED_TEST})
@DisplayName("JournalpostController og JournalpostHendelse")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BidragDokumentJournalpostLocalTest.class, properties = "STS_URL=junit")
@EnableWireMock(@ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
public class JournalpostControllerJournalpostHendelseTest {

  @Autowired
  private TestDataManager testDataManager;
  @Autowired
  private TestRestTemplate httpHeaderTestRestTemplate;

  @MockitoBean
  private JournalpostKafkaEventProducer journalpostKafkaEventProducerMock;

  @Test
  @DisplayName("skal publisering JournalpostHendelse som kafka melding")
  @Disabled
  void skalPublisereJournalpostHendelseSomKafkaMelding() {
    stubFor(
        get(urlMatching("/organisasjon.*")).willReturn(aResponse()
            .withHeader(HttpHeaders.CONNECTION, "close")
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json").withBody("{"
            + "\"ident\": \"21321\""
            + "}"))
    );
    var gammeltEnhetsnummer = "007";
    var enMottaksregistrertJournalpost = testDataManager.opprett(
        enMottaksregistrertJournalpost()
            .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
            .medFagomrade(Fagomrade.BIDRAG_DATABASE)
            .medGjelder("123123123")
            .medJournalforendeEnhet(gammeltEnhetsnummer)
    );

    var nyttEnhetsnummer = "1001";
    var pathAvvik = "/journal/%s/avvik".formatted(prefixId(enMottaksregistrertJournalpost));
    var avvikshendelse = """
        {
          "avvikType":"OVERFOR_TIL_ANNEN_ENHET",
          "detaljer": {
            "gammeltEnhetsnummer":"%s",
            "nyttEnhetsnummer"   :"%s"
          }
        }
        """.stripIndent().formatted(gammeltEnhetsnummer, nyttEnhetsnummer);

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        pathAvvik,
        HttpMethod.POST,
        initHttpEntity(avvikshendelse, new CustomHeader(EnhetFilter.X_ENHET_HEADER, gammeltEnhetsnummer)),
        BehandleAvvikshendelseResponse.class
    );

    assertThat(responseEntity.getStatusCode()).as("status").isEqualTo(HttpStatus.OK);

    var kafkaEventProducerCaptor = ArgumentCaptor.forClass(JournalpostHendelse.class);
    verify(journalpostKafkaEventProducerMock).publish(kafkaEventProducerCaptor.capture());
    var journalpostHendelse = kafkaEventProducerCaptor.getValue();

    assertAll(
        () -> assertThat(journalpostHendelse.getEnhet()).as("enhet").isEqualTo(nyttEnhetsnummer),
        () -> assertThat(journalpostHendelse.getFnr()).as("fnr").isEqualTo("123123123"),
        () -> assertThat(journalpostHendelse.getFagomrade()).as("fagomrade").isEqualTo(Fagomrade.BIDRAG),
        () -> assertThat(journalpostHendelse.getJournalstatus()).as("journalstatus").isEqualTo(Journalstatus.MOTTAKSREGISTRERT)
    );
  }

  @Test
  @DisplayName("skal feile tjenestekall når exeption oppstår ved publisering av kafka melding")
  @Disabled("Den eneste programatiske feilkilden til dette er hvis json-mapping ikke kan gjøres ved publisering. Derfor JournalpostHendelseMapperTest")
  void skalFeileTjenestekallNarExceptionOppstarNarKafkaMeldingPubliseres() {
    var enMottaksregistrertJournalpost = testDataManager.opprett(
        enMottaksregistrertJournalpost()
            .medDokumentType(DokumentType.INNGAENDE_DOKUMENT)
            .medFagomrade(Fagomrade.BIDRAG_DATABASE)
    );

    var pathAvvik = "/journal/%s/avvik".formatted(prefixId(enMottaksregistrertJournalpost));
    var avvikshendelse = """
        {
          "avvikType":"OVERFOR_TIL_ANNEN_ENHET",
          "detaljer": {
            "gammeltEnhetsnummer":"1",
            "nyttEnhetsnummer"   :"2"
          }
        }
        """.stripIndent();

    doThrow(new IllegalArgumentException("something fishy happened!"))
        .when(journalpostKafkaEventProducerMock).publish(any());

    var responseEntity = httpHeaderTestRestTemplate.exchange(
        pathAvvik,
        HttpMethod.POST,
        initHttpEntity(avvikshendelse, new CustomHeader(EnhetFilter.X_ENHET_HEADER, "2")),
        BehandleAvvikshendelseResponse.class
    );

    assertThat(responseEntity.getStatusCode()).as("status").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
