package no.nav.bidrag.sak.security

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.SpringH2TestRunner
import no.nav.bidrag.sak.service.BidragSakService
import no.nav.bidrag.transport.sak.BidragssakPipDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.util.UUID

class AuthenticationIntegrationTest : SpringH2TestRunner() {
    @MockkBean
    private lateinit var bidragSakService: BidragSakService

    private val saksnummer = Saksnummer("2600000")
    private val jwtPath = "bidrag-sak/sak/${saksnummer.verdi}/fogdhistorikk"
    private val claimsPath = "v2/pip/sak/${saksnummer.verdi}"

    @BeforeEach
    fun oppsett() {
        every { bidragSakService.finnFogdhistorikk(saksnummer) } returns emptyList()
        every { bidragSakService.finnPipFor(saksnummer) } returns BidragssakPipDto(saksnummer = saksnummer, avsluttet = false, roller = emptyList())
    }

    private fun hent(path: String, headers: HttpHeaders = HttpHeaders()) = testRestTemplate.exchange(
        "http://localhost:$port/$path",
        HttpMethod.GET,
        HttpEntity<Void>(headers),
        String::class.java,
    )

    @Test
    fun `JWT-beskyttet endepunkt avviser kall uten token før tjenesten kalles`() {
        val response = hent(jwtPath)

        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        response.headers.getFirst(HttpHeaders.WWW_AUTHENTICATE) shouldBe "Bearer"
        verify(exactly = 0) { bidragSakService.finnFogdhistorikk(any()) }
    }

    @Test
    fun `JWT-beskyttet endepunkt avviser ugyldig token`() {
        val response = hent(jwtPath, HttpHeaders().apply { setBearerAuth("ugyldig") })

        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        response.headers.getFirst(HttpHeaders.WWW_AUTHENTICATE) shouldBe "Bearer"
        verify(exactly = 0) { bidragSakService.finnFogdhistorikk(any()) }
    }

    @Test
    fun `JWT-beskyttet endepunkt avviser token med ugyldig signatur`() {
        val token = lokalTestToken().substringBeforeLast('.') + ".AA"

        val response = hent(jwtPath, HttpHeaders().apply { setBearerAuth(token) })

        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        verify(exactly = 0) { bidragSakService.finnFogdhistorikk(any()) }
    }

    @Test
    fun `JWT-beskyttet endepunkt godtar gyldig token`() {
        val response = hent(jwtPath, HttpHeaders().apply { setBearerAuth(lokalTestToken()) })

        response.statusCode shouldBe HttpStatus.OK
        verify(exactly = 1) { bidragSakService.finnFogdhistorikk(saksnummer) }
    }

    @Test
    fun `Basic-innlogging erstatter ikke JWT på JWT-beskyttet endepunkt`() {
        val passord = UUID.randomUUID().toString()

        val response = hent(jwtPath, HttpHeaders().apply { setBasicAuth("testbruker", passord) })

        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        verify(exactly = 0) { bidragSakService.finnFogdhistorikk(any()) }
    }

    @Test
    fun `Azure-PIP avviser token uten påkrevd rolle`() {
        val response = hent(claimsPath, HttpHeaders().apply { setBearerAuth(lokalTestToken()) })

        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        verify(exactly = 0) { bidragSakService.finnPipFor(any()) }
    }

    @Test
    fun `Azure-PIP godtar token med påkrevd rolle`() {
        val response = hent(claimsPath, HttpHeaders().apply { setBearerAuth(lokalTestToken(listOf("sak-pip"))) })

        response.statusCode shouldBe HttpStatus.OK
        verify(exactly = 1) { bidragSakService.finnPipFor(saksnummer) }
    }

    @Test
    fun `Azure-PIP avviser Basic-innlogging`() {
        val passord = UUID.randomUUID().toString()

        val response = hent(claimsPath, HttpHeaders().apply { setBasicAuth("testbruker", passord) })

        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        response.headers.getFirst(HttpHeaders.WWW_AUTHENTICATE) shouldBe "Bearer"
        verify(exactly = 0) { bidragSakService.finnPipFor(any()) }
    }

    @Test
    fun `Azure-PIP avviser kall uten autentisering`() {
        val response = hent(claimsPath)

        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        response.headers.getFirst(HttpHeaders.WWW_AUTHENTICATE) shouldBe "Bearer"
        verify(exactly = 0) { bidragSakService.finnPipFor(any()) }
    }

    @Test
    fun `helseendepunktet kan fortsatt brukes uten autentisering`() {
        val response = hent("actuator/health/ping")

        response.statusCode shouldBe HttpStatus.OK
    }
}
