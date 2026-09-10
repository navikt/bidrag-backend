package no.nav.bidrag.automatiskjobb.consumer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.vedtak.Vedtakskilde
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.LocalDateTime

class BidragVedtakConsumerTest {
    private val restTemplate = RestTemplate()
    private val mockServer: MockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build()
    private val consumer = BidragVedtakConsumer(URI("http://bidrag-vedtak"), restTemplate)

    private val request = OpprettVedtakRequestDto(
        kilde = Vedtakskilde.AUTOMATISK,
        type = Vedtakstype.ENDRING_MOTTAKER,
        vedtakstidspunkt = LocalDateTime.now(),
        unikReferanse = "endring_mottaker_test",
        grunnlagListe = emptyList(),
    )

    @Test
    fun `skal returnere eksisterende vedtaksid ved 409 Conflict`() {
        mockServer.expect(requestTo("http://bidrag-vedtak/vedtak"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.CONFLICT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"vedtaksid": 555}"""),
            )

        val respons = consumer.opprettVedtak(request)

        respons.vedtaksid shouldBe 555
        mockServer.verify()
    }

    @Test
    fun `skal kaste videre ved andre feil enn 409`() {
        mockServer.expect(requestTo("http://bidrag-vedtak/vedtak"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        shouldThrow<HttpStatusCodeException> {
            consumer.opprettVedtak(request)
        }
        mockServer.verify()
    }

    @Test
    fun `skal returnere respons ved suksess`() {
        mockServer.expect(requestTo("http://bidrag-vedtak/vedtak"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    """{"vedtaksid": 42, "engangsbeløpReferanseListe": []}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val respons = consumer.opprettVedtak(request)

        respons.vedtaksid shouldBe 42
        mockServer.verify()
    }
}
