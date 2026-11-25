package no.nav.bidrag.grunnlag.consumer.valutakurser

import no.nav.bidrag.grunnlag.exception.RestResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.time.YearMonth

class FrankfurterConsumerTest {
    private val uri = "https://api.frankfurter.dev/v2/rates?from=2024-12-01&to=2024-12-31&group=month&base=USD&quotes=NOK"
    private val restTemplate = RestTemplate()
    private val server = MockRestServiceServer.createServer(restTemplate)
    private val consumer = FrankfurterConsumer(URI.create("https://api.frankfurter.dev"), restTemplate)

    @Test
    fun `henter månedskurs fra Frankfurter`() {
        server.expect(requestTo(uri)).andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """[{"date":"2024-12-01","base":"USD","quote":"NOK","rate":10.8}]""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val resultat = consumer.hentMaanedskurs("USD", YearMonth.of(2024, 12))

        val rates = assertInstanceOf(RestResponse.Success::class.java, resultat).body as List<FrankfurterRate>
        assertEquals(FrankfurterRate(LocalDate.of(2024, 12, 1), "USD", "NOK", BigDecimal("10.8")), rates.single())
        server.verify()
    }

    @Test
    fun `returnerer feilresultat når Frankfurter feiler`() {
        server.expect(ExpectedCount.times(3), requestTo(uri)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        val resultat = consumer.hentMaanedskurs("USD", YearMonth.of(2024, 12))

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertInstanceOf(RestResponse.Failure::class.java, resultat).statusCode)
        server.verify()
    }
}
