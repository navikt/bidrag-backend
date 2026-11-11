package no.nav.bidrag.sak.security

import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.SpringH2TestRunner
import no.nav.bidrag.sak.security.authentication.ldap.LdapUserService
import no.nav.bidrag.sak.service.BidragSakService
import no.nav.bidrag.transport.sak.BidragssakPipDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID

class AuthenticationIntegrationTest : SpringH2TestRunner() {
    @MockkBean
    private lateinit var bidragSakService: BidragSakService

    @MockkBean
    private lateinit var ldapUserService: LdapUserService

    private val saksnummer = Saksnummer("2600000")
    private val jwtPath = "bidrag-sak/sak/${saksnummer.verdi}/fogdhistorikk"
    private val basicPath = "bidrag-sak/pip/sak/${saksnummer.verdi}"
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
        every { ldapUserService.authenticate("testbruker", passord, any()) } returns true

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
    fun `Basic-PIP godtar gyldig Basic-innlogging uten JWT`() {
        val passord = UUID.randomUUID().toString()
        every { ldapUserService.authenticate("testbruker", passord, any()) } returns true

        val response = hent(basicPath, HttpHeaders().apply { setBasicAuth("testbruker", passord) })

        response.statusCode shouldBe HttpStatus.OK
        verify(exactly = 1) { bidragSakService.finnPipFor(saksnummer) }
    }

    @Test
    fun `Basic-klient logger inn automatisk etter challenge`() {
        val passord = UUID.randomUUID().toString()
        every { ldapUserService.authenticate("testbruker", passord, any()) } returns true
        val authenticator = object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication = PasswordAuthentication("testbruker", passord.toCharArray())
        }

        HttpClient.newBuilder().authenticator(authenticator).build().use { client ->
            val request = HttpRequest.newBuilder(URI.create("http://localhost:$port/$basicPath")).GET().build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            response.statusCode() shouldBe HttpStatus.OK.value()
        }
        verify(exactly = 1) { ldapUserService.authenticate("testbruker", passord, any()) }
        verify(exactly = 1) { bidragSakService.finnPipFor(saksnummer) }
    }

    @Test
    fun `Basic-PIP avviser feil passord`() {
        val passord = UUID.randomUUID().toString()
        every { ldapUserService.authenticate("testbruker", passord, any()) } returns false

        val response = hent(basicPath, HttpHeaders().apply { setBasicAuth("testbruker", passord) })

        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        response.headers.getFirst(HttpHeaders.WWW_AUTHENTICATE) shouldBe "Basic realm=\"bidrag-sak\""
        verify(exactly = 0) { bidragSakService.finnPipFor(any()) }
    }

    @Test
    fun `Basic-PIP avviser kall uten autentisering`() {
        val response = hent(basicPath)

        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        response.headers.getFirst(HttpHeaders.WWW_AUTHENTICATE) shouldBe "Basic realm=\"bidrag-sak\""
        verify(exactly = 0) { bidragSakService.finnPipFor(any()) }
    }

    @Test
    fun `JWT erstatter ikke Basic-innlogging på Basic-PIP`() {
        val response = hent(basicPath, HttpHeaders().apply { setBearerAuth(lokalTestToken()) })

        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        verify(exactly = 0) { bidragSakService.finnPipFor(any()) }
    }

    @Test
    fun `helseendepunktet kan fortsatt brukes uten autentisering`() {
        val response = hent("actuator/health/ping")

        response.statusCode shouldBe HttpStatus.OK
    }
}
