package no.nav.bidrag.sak.controller

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.SpringTestRunner
import no.nav.bidrag.sak.util.BidragssakProvider
import no.nav.bidrag.transport.sak.BidragssakPipDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

class PipControllerIT : SpringTestRunner() {
    @Autowired
    private lateinit var bidragssakProvider: BidragssakProvider

    private val restTemplate = RestTemplate()

    @BeforeEach
    fun setAuthorizationHeader() {
        restTemplate.interceptors = listOf(
            ClientHttpRequestInterceptor { request, body, execution ->
                request.headers.setBearerAuth(lokalTestToken(listOf("sak-pip")))
                execution.execute(request, body)
            },
        )
    }

    private fun makeFullContextPath(): String = "http://localhost:$port"

    private fun forPipUrl(saksnummer: Saksnummer): String = UriComponentsBuilder
        .fromUriString(
            makeFullContextPath() + PipController.PIP_SAK_V2 + '/' + saksnummer.verdi,
        ).toUriString()

    @Test
    fun `should get status NOT_FOUND if sak is missing`() {
        val e =
            shouldThrow<HttpClientErrorException> {
                restTemplate.getForEntity(forPipUrl(Saksnummer("959559593")), BidragssakPipDto::class.java)
            }

        e.statusCode shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `should return sak`() {
        val saksnummer = bidragssakProvider.lagreSakTilDatabase()
        val response = restTemplate.getForEntity(forPipUrl(saksnummer), BidragssakPipDto::class.java)
        response shouldNotBe null
        response.statusCode shouldBe HttpStatus.OK
        response.body shouldNotBe null
    }

    @Test
    fun `skal gi 'not found' når saksnummer ikke er tall`() {
        val e =
            shouldThrow<HttpClientErrorException> {
                restTemplate.getForEntity(forPipUrl(Saksnummer("saksnr.")), BidragssakPipDto::class.java)
            }

        e.statusCode shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `skal gi 'not found' når lengden til saksnummeret ikke er 7 siffer`() {
        val e =
            shouldThrow<HttpClientErrorException> {
                restTemplate.getForEntity(forPipUrl(Saksnummer("123")), BidragssakPipDto::class.java)
            }

        e.statusCode shouldBe HttpStatus.NOT_FOUND
    }
}
