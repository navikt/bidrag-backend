package no.nav.bidrag.dokument.journalpost.consumer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI

@DisplayName("BrevserverConsumer")
class BrevserverConsumerTest {
    private lateinit var restTemplate: RestTemplate
    private lateinit var mockServer: MockRestServiceServer
    private lateinit var brevserverConsumer: BrevserverConsumer

    private val baseUrl = "https://brevserver-nais.intern.nav.no"

    @BeforeEach
    fun setup() {
        restTemplate = RestTemplate()
        mockServer = MockRestServiceServer.createServer(restTemplate)
        brevserverConsumer = BrevserverConsumer(URI.create(baseUrl), restTemplate)
    }

    @Test
    @DisplayName("skal hente dokument som bytearray fra riktig url")
    fun skalHenteDokument() {
        val pdf = "%PDF-1.4 innhold".toByteArray()
        mockServer
            .expect(requestTo("$baseUrl/brevweb/rest/hentdokument/bisys/brevref-123"))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andRespond(withSuccess(pdf, MediaType.APPLICATION_PDF))

        val respons = brevserverConsumer.hentDokument("brevref-123")

        respons shouldBe pdf
        mockServer.verify()
    }

    @Test
    @DisplayName("skal kaste exception når brevserver feiler")
    fun skalKasteExceptionVedFeil() {
        mockServer
            .expect(requestTo("$baseUrl/brevweb/rest/hentdokument/bisys/brevref-123"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        shouldThrow<HttpServerErrorException> {
            brevserverConsumer.hentDokument("brevref-123")
        }
    }
}
