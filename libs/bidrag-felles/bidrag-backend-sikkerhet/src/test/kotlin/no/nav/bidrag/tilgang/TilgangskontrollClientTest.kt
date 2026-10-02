package no.nav.bidrag.tilgang

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.tilgang.OpprinnelseTilgangsbeslutning
import no.nav.bidrag.transport.tilgang.TilgangskontrollResponse
import no.nav.bidrag.transport.tilgang.TilgangskontrollResponseDetaljer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals

class TilgangskontrollClientTest {
    private val restTemplate = RestTemplate()
    private val mockServer = MockRestServiceServer.bindTo(restTemplate).build()
    private val client = TilgangskontrollClient(RestClient.builder(restTemplate).build())

    @Test
    fun `sjekkTilgangSaksnummer kaller tilgangsapi for sak`() {
        mockServer.expect(requestTo("/v2/api/tilgang/sak"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""{"harTilgang":true, "detaljer":[]}"""),
            )

        assertEquals(TilgangskontrollResponse(harTilgang = true), client.sjekkTilgangSaksnummer(Saksnummer("1234567")))
        mockServer.verify()
    }

    @Test
    fun `sjekkTilgangPerson returnerer detaljer ved avslag`() {
        mockServer.expect(requestTo("/v2/api/tilgang/person"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        """{"harTilgang":false,"detaljer":[{"harTilgang":false,"begrunnelse":"Skjermet","opprinnelseTilgangsbeslutning":"TILGANGSMASKIN"}]}""",
                    ),
            )

        val forventet = TilgangskontrollResponse(
            harTilgang = false,
            detaljer = listOf(
                TilgangskontrollResponseDetaljer(false, "Skjermet", OpprinnelseTilgangsbeslutning.TILGANGSMASKIN),
            ),
        )
        assertEquals(forventet, client.sjekkTilgangPerson(Personident("12345678901")))
        mockServer.verify()
    }

    @Test
    fun `sjekkTilgangSaksnummer kaster ved serverfeil`() {
        mockServer.expect(requestTo("/v2/api/tilgang/sak"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        assertThrows<HttpServerErrorException> { client.sjekkTilgangSaksnummer(Saksnummer("1234567")) }
        mockServer.verify()
    }

    @Test
    fun `sjekkTilgangPerson kaster ved tomt svar`() {
        mockServer.expect(requestTo("/v2/api/tilgang/person"))
            .andRespond(withStatus(HttpStatus.OK))

        assertThrows<IllegalStateException> { client.sjekkTilgangPerson(Personident("12345678901")) }
        mockServer.verify()
    }
}
