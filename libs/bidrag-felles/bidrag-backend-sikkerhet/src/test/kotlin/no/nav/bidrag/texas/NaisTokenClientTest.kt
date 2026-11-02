package no.nav.bidrag.texas

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals

class NaisTokenClientTest {
    private val restTemplate = RestTemplate()
    private val server = MockRestServiceServer.bindTo(restTemplate).build()
    private val client = NaisTokenClient(
        client = RestClient.builder(restTemplate).build(),
        oboEndpoint = "http://texas/token/exchange",
        m2mEndpoint = "http://texas/token",
    )

    @Test
    fun `obo sender forespørsel til Texas og leser svaret`() {
        server.expect(requestTo("http://texas/token/exchange"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""{"identity_provider":"azuread","target":"api://target/.default","user_token":"incoming-token"}""", JsonCompareMode.STRICT))
            .andRespond(
                withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                    .body("""{"access_token":"obo-token","token_type":"Bearer","expires_in":3600}"""),
            )

        assertEquals(TexasResponse("obo-token", "Bearer", 3600), client.oboToken("api://target/.default", "incoming-token"))
        server.verify()
    }

    @Test
    fun `m2m sender forespørsel til Texas og leser svaret`() {
        server.expect(requestTo("http://texas/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().json("""{"identity_provider":"azuread","target":"api://target/.default"}""", JsonCompareMode.STRICT))
            .andRespond(
                withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON)
                    .body("""{"access_token":"m2m-token","token_type":"Bearer","expires_in":120}"""),
            )

        assertEquals(TexasResponse("m2m-token", "Bearer", 120), client.m2mToken("api://target/.default"))
        server.verify()
    }

    @Test
    fun `obo videresender feil fra Texas`() {
        server.expect(requestTo("http://texas/token/exchange"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED))

        assertThrows<HttpClientErrorException.Unauthorized> {
            client.oboToken("api://target/.default", "incoming-token")
        }
        server.verify()
    }

    @Test
    fun `m2m videresender feil fra Texas`() {
        server.expect(requestTo("http://texas/token"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.FORBIDDEN))

        assertThrows<HttpClientErrorException.Forbidden> {
            client.m2mToken("api://target/.default")
        }
        server.verify()
    }
}
