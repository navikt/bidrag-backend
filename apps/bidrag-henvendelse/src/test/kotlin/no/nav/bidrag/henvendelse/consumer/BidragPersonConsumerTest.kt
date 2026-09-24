package no.nav.bidrag.henvendelse.consumer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.henvendelse.aop.PersonIkkeFunnetException
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpResponse
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate
import java.net.URI

/**
 * Vi ber bidrag-person om AKTORID uten historikk, men filtrerer likevel selv. Grunnen er at
 * `grupper` og `inkludereHistoriske` er noe den andre tjenesten avgjør hva den gjør med, og
 * en aktørid vi ikke skulle hatt gir oppslag på feil person i henvendelsesløsningen.
 */
class BidragPersonConsumerTest {
    private val restTemplate = RestTemplate()
    private val mockServer = MockRestServiceServer.bindTo(restTemplate).build()
    private val consumer = BidragPersonConsumer(URI.create("http://bidrag-person"), restTemplate)

    @Test
    fun `skal hente den gjeldende aktøriden`() {
        stub(
            """
            [
              { "ident": "$FNR", "historisk": false, "gruppe": "FOLKEREGISTERIDENT" },
              { "ident": "1000000000000", "historisk": true, "gruppe": "AKTORID" },
              { "ident": "$AKTØRID", "historisk": false, "gruppe": "AKTORID" }
            ]
            """.trimIndent(),
        )

        consumer.hentAktørid(Personident(FNR)) shouldBe AKTØRID
    }

    @Test
    fun `skal gi null når personen bare har historiske aktørider`() {
        stub("""[ { "ident": "1000000000000", "historisk": true, "gruppe": "AKTORID" } ]""")

        consumer.hentAktørid(Personident(FNR)) shouldBe null
    }

    @Test
    fun `skal gi null når personen ikke har noen identer`() {
        stub("[]")

        consumer.hentAktørid(Personident(FNR)) shouldBe null
    }

    @Test
    fun `skal kaste PersonIkkeFunnetException når bidrag-person svarer 404`() {
        // Uten oversettelsen her ville en ukjent ident blitt 502 "Feil ved kall mot tjeneste".
        mockServer
            .expect(requestTo("http://bidrag-person/personidenter"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        shouldThrow<PersonIkkeFunnetException> { consumer.hentAktørid(Personident(FNR)) }
    }

    @Test
    fun `skal kaste PersonIkkeFunnetException også når 404 kommer som HttpServerErrorException`() {
        // AbstractRestClient.validerOgPakkUt lager en HttpServerErrorException av enhver ikke-2xx
        // respons RestTemplate ikke selv har kastet på, og beholder 404 i statusfeltet. Fanger vi
        // bare HttpClientErrorException.NotFound, går den formen rett forbi.
        val restTemplate = RestTemplate().apply { errorHandler = IngenFeilhåndtering() }
        val server = MockRestServiceServer.bindTo(restTemplate).build()
        val consumer = BidragPersonConsumer(URI.create("http://bidrag-person"), restTemplate)
        server
            .expect(requestTo("http://bidrag-person/personidenter"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        shouldThrow<PersonIkkeFunnetException> { consumer.hentAktørid(Personident(FNR)) }
    }

    private fun stub(respons: String) {
        mockServer
            .expect(requestTo("http://bidrag-person/personidenter"))
            .andRespond(withSuccess(respons, MediaType.APPLICATION_JSON))
    }

    companion object {
        private const val FNR = "17490123474"
        private const val AKTØRID = "2000012345678"
    }
}

/** Lar ikke-2xx passere som respons, slik oppsettet i nais gjør. */
private class IngenFeilhåndtering : ResponseErrorHandler {
    override fun hasError(response: ClientHttpResponse) = false

    override fun handleError(
        url: URI,
        method: HttpMethod,
        response: ClientHttpResponse,
    ) = Unit
}
