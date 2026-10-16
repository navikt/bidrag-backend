package no.nav.bidrag.grunnlag.consumer.valutakurser

import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.ecb.Frequency
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.toExchangeRates
import no.nav.bidrag.grunnlag.exception.RestResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate

class ECBConsumerTest {
    private val dato = LocalDate.of(2025, 6, 30)
    private val uri = "https://data-api.ecb.europa.eu/service/data/EXR/M.NOK.EUR.SP00.A?startPeriod=2025-06&endPeriod=2025-06"
    private val restTemplate = RestTemplate()
    private val server = MockRestServiceServer.createServer(restTemplate)
    private val consumer = ECBConsumer(URI.create("https://data-api.ecb.europa.eu"), restTemplate, GrunnlagConsumer())

    @Test
    fun `månedsdata fra ECB leses via felles consumer`() {
        val json = """
            {
              "header": {"id": "eksempel"},
              "dataSets": [{"series": {"0:0": {"observations": {"0": [11.584133333333332, 0, null]}}}}],
              "structure": {"dimensions": {
                "series": [
                  {"id": "FREQ", "values": [{"id": "M"}]},
                  {"id": "CURRENCY", "values": [{"id": "NOK"}]}
                ],
                "observation": [{"id": "TIME_PERIOD", "values": [{"id": "2025-06"}]}]
              }}
            }
        """.trimIndent()
        server.expect(requestTo(uri)).andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(json, MediaType.parseMediaType("application/vnd.sdmx.data+json;version=1.0.0-wd")))

        val resultat = consumer.hentValutakurs(Frequency.Monthly, listOf("NOK"), dato)

        val kurs = when (resultat) {
            is RestResponse.Success -> resultat.body.toExchangeRates().single()
            is RestResponse.Failure -> error("ECB returnerte ${resultat.statusCode}")
        }
        assertEquals("NOK", kurs.valuta)
        assertEquals(BigDecimal("11.584133333333332"), kurs.kurs)
        assertEquals(dato, kurs.kursDato)
        server.verify()
    }

    @Test
    fun `feil fra ECB returneres som feilresultat`() {
        server.expect(requestTo(uri)).andRespond(withStatus(HttpStatus.NOT_FOUND))

        val resultat = consumer.hentValutakurs(Frequency.Monthly, listOf("NOK"), dato)

        assertEquals(HttpStatus.NOT_FOUND, assertInstanceOf(RestResponse.Failure::class.java, resultat).statusCode)
        server.verify()
    }
}
