package no.nav.bidrag.grunnlag.consumer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.util.CustomJacksonHttpMessageConverter
import no.nav.bidrag.grunnlag.aop.DefaultExceptionHandler
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import java.net.SocketTimeoutException
import java.net.URI

class TilgangskontrollConsumerTest {
    private val restTemplate = RestTemplate(listOf(CustomJacksonHttpMessageConverter(commonObjectmapper)))
    private val server = MockRestServiceServer.bindTo(restTemplate).build()
    private val consumer = TilgangskontrollConsumer(restTemplate, URI("https://tilgangskontroll.example"))
    private val kontekst = mockk<TokenValidationContext>()
    private val claims = mockk<JwtTokenClaims>()
    private val endepunkt = "https://tilgangskontroll.example/v2/api/tilgang/brukertilganger"

    @BeforeEach
    fun oppsett() {
        mockkConstructor(SpringTokenValidationContextHolder::class)
        mockkStatic(TokenUtils::class)
        every { anyConstructed<SpringTokenValidationContextHolder>().getTokenValidationContext() } returns kontekst
        every { kontekst.hasValidToken() } returns true
        every { kontekst.hasTokenFor("aad") } returns true
        every { kontekst.getClaims("aad") } returns claims
        every { TokenUtils.erApplikasjonsbruker() } returns false
        every { claims.getStringClaim("NAVident") } returns "Z999999"
        every { claims.getStringClaim("preferred_username") } returns "test@example.invalid"
        every { claims.getStringClaim("sub") } returns "bruker-subjekt"
        every { claims.getStringClaim("oid") } returns "bruker-objekt"
    }

    @AfterEach
    fun ryddOpp() {
        unmockkAll()
        server.verify()
    }

    @Test
    fun `skal tillate administrator fra tilgangskontroll`() {
        server.expect(requestTo(endepunkt)).andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(brukertilganger(true), MediaType.APPLICATION_JSON))

        consumer.krevAdministrasjonstilgang()
    }

    @Test
    fun `skal avvise bruker uten administrasjonstilgang`() {
        server.expect(requestTo(endepunkt))
            .andRespond(withSuccess(brukertilganger(false), MediaType.APPLICATION_JSON))

        assertThrows<ResponseStatusException> { consumer.krevAdministrasjonstilgang() }
            .statusCode shouldBe HttpStatus.FORBIDDEN
    }

    @Test
    fun `skal avvise applikasjonsbruker uten kall til tilgangskontroll`() {
        every { TokenUtils.erApplikasjonsbruker() } returns true

        assertThrows<ResponseStatusException> { consumer.krevAdministrasjonstilgang() }
            .statusCode shouldBe HttpStatus.FORBIDDEN
    }

    @Test
    fun `skal avvise token med identisk sub og oid uten kall til tilgangskontroll`() {
        every { claims.getStringClaim("sub") } returns "bruker-objekt"

        assertThrows<ResponseStatusException> { consumer.krevAdministrasjonstilgang() }
            .statusCode shouldBe HttpStatus.FORBIDDEN
    }

    @ParameterizedTest
    @ValueSource(strings = ["NAVident", "preferred_username", "sub", "oid"])
    fun `skal kreve brukerclaims uten kall til tilgangskontroll`(claim: String) {
        every { claims.getStringClaim(claim) } returns " "

        assertThrows<ResponseStatusException> { consumer.krevAdministrasjonstilgang() }
            .statusCode shouldBe HttpStatus.FORBIDDEN
    }

    @Test
    fun `skal avvise annen utsteder uten kall til tilgangskontroll`() {
        every { kontekst.hasTokenFor("aad") } returns false

        assertThrows<ResponseStatusException> { consumer.krevAdministrasjonstilgang() }
            .statusCode shouldBe HttpStatus.FORBIDDEN
    }

    @ParameterizedTest
    @ValueSource(ints = [401, 403, 500])
    fun `skal skille tekniske feil fra tilgangsavslag uten å eksponere svarinnhold`(status: Int) {
        server.expect(requestTo(endepunkt))
            .andRespond(withStatus(HttpStatus.valueOf(status)).body("sensitivt-downstream-innhold"))

        val feil = assertThrows<ResponseStatusException> { consumer.krevAdministrasjonstilgang() }

        feil.statusCode shouldBe HttpStatus.SERVICE_UNAVAILABLE
        feil.reason shouldBe "Kunne ikke hente brukertilganger"
        feil.cause shouldBe null
        val respons = DefaultExceptionHandler().handleResponseStatusExceptions(feil)
        respons.statusCode shouldBe HttpStatus.SERVICE_UNAVAILABLE
        respons.toString().contains("sensitivt-downstream-innhold") shouldBe false
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "ugyldig-json", "{}"])
    fun `skal avvise tom eller ugyldig respons`(body: String) {
        server.expect(requestTo(endepunkt)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON))

        assertThrows<ResponseStatusException> { consumer.krevAdministrasjonstilgang() }
            .statusCode shouldBe HttpStatus.SERVICE_UNAVAILABLE
    }

    @Test
    fun `skal stoppe ved timeout uten å eksponere feildetaljer`() {
        server.expect(requestTo(endepunkt)).andRespond(withException(SocketTimeoutException("sensitive detaljer")))

        val feil = assertThrows<ResponseStatusException> { consumer.krevAdministrasjonstilgang() }

        feil.statusCode shouldBe HttpStatus.SERVICE_UNAVAILABLE
        feil.reason shouldBe "Kunne ikke hente brukertilganger"
        feil.cause shouldBe null
    }

    private fun brukertilganger(administrasjon: Boolean) = """
        {
          "bisysTilgang": false,
          "utlandTilgang": false,
          "leseSakTilgang": false,
          "behandleSakTilgang": false,
          "foreldreskapTilgang": false,
          "administrasjonTilgang": $administrasjon,
          "behandlingstemaer": []
        }
    """.trimIndent()
}
