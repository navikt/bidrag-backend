package no.nav.bidrag.sak.controller

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.SpringTestRunner
import no.nav.bidrag.sak.util.BidragssakProvider
import no.nav.bidrag.transport.sak.BidragssakPipDto
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.message.BasicHeader
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.nio.charset.StandardCharsets
import java.util.Base64

class PipControllerIT : SpringTestRunner() {
    @Autowired
    private lateinit var bidragssakProvider: BidragssakProvider

    private lateinit var authorization: BasicHeader

    private val restTemplate = RestTemplate()

    @BeforeEach
    fun setauthorizationHeader() {
        setAuthorizationHeader(TEST_USER_ID, TEST_USER_PWD)
    }

    private fun setAuthorizationHeader(
        userId: String,
        pwd: String,
    ) {
        authorization =
            BasicHeader(
                HttpHeaders.AUTHORIZATION,
                BASIC_AUTHENTICATION_PREFIX + base64EncodeCredentials(userId, pwd),
            )
        setAuthHeaderInRestTemplate(authorization)
    }

    private fun setAuthHeaderInRestTemplate(authHeader: BasicHeader) {
        val httpClient: CloseableHttpClient = HttpClients.custom().setDefaultHeaders(listOf(authHeader)).build()
        restTemplate.requestFactory = HttpComponentsClientHttpRequestFactory(httpClient)
    }

    private fun makeFullContextPath(): String = "http://localhost:$port/bidrag-sak"

    private fun forPipUrl(saksnummer: Saksnummer): String = UriComponentsBuilder
        .fromUriString(
            makeFullContextPath() + PipController.PIP_SAK + '/' + saksnummer.verdi,
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

    /**
     * Testing that access is denied if security constraints are not met.
     * Happy path tested indirectly in functional tests.
     */
    @Nested
    @DisplayName("AccessSecurity negative tests")
    internal inner class AccessSecurity {
        @Test
        @Disabled
        fun `401 response if authorization header is missing`() {
            // Provided authorization header is missing
            setAuthHeaderInRestTemplate(BasicHeader("", ""))

            // When PIP is requested
            val e =
                shouldThrow<HttpClientErrorException> {
                    restTemplate.getForEntity(forPipUrl(Saksnummer("1234567")), BidragssakPipDto::class.java)
                }
            // Then respond with unauthorized
            e.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }

        @Test
        @Disabled
        fun `401 response if password is incorrect`() {
            // Provided password is incorrect
            setAuthorizationHeader(TEST_USER_ID, TEST_USER_PWD + "rubbish")

            // When PIP is requested
            val e =
                shouldThrow<HttpClientErrorException> {
                    restTemplate.getForEntity(forPipUrl(Saksnummer("1234567")), BidragssakPipDto::class.java)
                }

            // Then respond with unauthorized
            e.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }

        @Test
        fun `Authentication successful`() {
            // Provided
            val saksnummer = bidragssakProvider.lagreSakTilDatabase()

            // When requesting PIP (with correct credentials)
            val response =
                restTemplate.getForEntity(forPipUrl(saksnummer), BidragssakPipDto::class.java)

            // Then 2XX-response
            response.statusCode shouldBe HttpStatus.OK
        }
    }

    companion object {
        private const val TEST_USER_ID = "srvtjhorse"
        private const val TEST_USER_PWD = "secret"
        private const val BASIC_AUTHENTICATION_PREFIX = "Basic "

        /**
         * Returns a Base64 encoded string comprised of username:password
         *
         * @param username to encode
         * @param password to encode
         * @return Base64 encoded username:password string
         */
        fun base64EncodeCredentials(
            username: String,
            password: String,
        ): String {
            val credentials = "$username:$password"
            val encodedCredentials = Base64.getEncoder().encode(credentials.toByteArray())
            return String(encodedCredentials, StandardCharsets.UTF_8)
        }
    }
}
