package no.nav.bidrag.dokument.consumer

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.bidrag.transport.dokument.DistribuerJournalpostRequest
import no.nav.bidrag.transport.dokument.DokumentTilgangResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory

internal class ConsumerUriEncodingTest {
    @Test
    fun `finnAvvik encodes query parameters before calling downstream service`() {
        val restTemplate = restTemplate("https://journalpost.test")
        val server = MockRestServiceServer.createServer(restTemplate)
        val consumer = BidragDokumentConsumer("journalpost", restTemplate, "https://journalpost.test", SimpleMeterRegistry())

        server
            .expect(requestTo("https://journalpost.test/journal/BID-1/avvik?saksnummer=sak%2F1%3Fekstern%3Dtrue"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("[]"))

        consumer.finnAvvik("sak/1?ekstern=true", "BID-1")

        server.verify()
    }

    @Test
    fun `hentDokument encodes document references before calling downstream service`() {
        val restTemplate = restTemplate("https://journalpost.test")
        val server = MockRestServiceServer.createServer(restTemplate)
        val consumer = BidragDokumentConsumer("journalpost", restTemplate, "https://journalpost.test", SimpleMeterRegistry())

        server
            .expect(requestTo("https://journalpost.test/dokument/BID-1/http%3A%2F%2Fevil.test%2Fdokument%2F1%3Ffoo%3Dbar"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_OCTET_STREAM).body(byteArrayOf(1, 2, 3)))

        consumer.hentDokument("BID-1", "http://evil.test/dokument/1?foo=bar")

        server.verify()
    }

    @Test
    fun `hentTilgangUrl encodes document reference path variables before calling downstream service`() {
        val restTemplate = restTemplate("https://journalpost.test")
        val server = MockRestServiceServer.createServer(restTemplate)
        val consumer = DokumentTilgangConsumer(restTemplate)

        server
            .expect(requestTo("https://journalpost.test/tilgang/dokumentreferanse/http%3A%2F%2Fevil.test%2Fdokument%2F1%3Ffoo%3Dbar"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"dokumentUrl":"https://journalpost.test/dokument/1","type":"PDF"}"""),
            )

        val response = consumer.hentTilgangUrl(null, "http://evil.test/dokument/1?foo=bar")

        server.verify()
        assertEquals(DokumentTilgangResponse("https://journalpost.test/dokument/1", "PDF"), response)
    }

    @Test
    fun `finnJournalposter encodes case numbers before calling downstream service`() {
        val restTemplate = restTemplate("https://journalpost.test")
        val server = MockRestServiceServer.createServer(restTemplate)
        val consumer = BidragDokumentConsumer("journalpost", restTemplate, "https://journalpost.test", SimpleMeterRegistry())

        server
            .expect(requestTo("https://journalpost.test/sak/sak%2F1%3Fekstern=true/journal?fagomrade=BID"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("[]"))

        consumer.finnJournalposter("sak/1?ekstern=true", listOf("BID"))

        server.verify()
    }

    @Test
    fun `distribuerJournalpost encodes batchId query parameter before calling downstream service`() {
        val restTemplate = restTemplate("https://journalpost.test")
        val server = MockRestServiceServer.createServer(restTemplate)
        val consumer = BidragDokumentConsumer("journalpost", restTemplate, "https://journalpost.test", SimpleMeterRegistry())

        server
            .expect(requestTo("https://journalpost.test/journal/distribuer/BID-1?batchId=batch%2F1%3Fekstern%3Dtrue"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"bestillingsId":"1","journalpostId":"BID-1"}"""),
            )

        consumer.distribuerJournalpost("BID-1", "batch/1?ekstern=true", DistribuerJournalpostRequest())

        server.verify()
    }

    private fun restTemplate(baseUrl: String) = RestTemplate().apply { uriTemplateHandler = DefaultUriBuilderFactory(baseUrl) }
}
