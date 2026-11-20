package no.nav.bidrag.beregn.core.consumer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.commons.util.CustomJacksonHttpMessageConverter
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate

class ValutakursConsumerTest {
    private val factory = mockk<ClientHttpRequestFactory>()
    private val request = mockk<ClientHttpRequest>(relaxed = true)
    private val response = mockk<ClientHttpResponse>(relaxed = true)
    private val consumer = ValutakursConsumer(
        URI("https://bidrag-grunnlag.test/prefiks"),
        RestTemplate(factory).apply { messageConverters = listOf(CustomJacksonHttpMessageConverter()) },
    )

    @Test
    fun `skal hente og lese lagret halvårskurs uten å bruke observasjonsdatoen som gyldig fra`() {
        every { factory.createRequest(any(), HttpMethod.GET) } returns request
        every { request.execute() } returns response
        every { response.statusCode } returns HttpStatus.OK
        every { response.headers } returns HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        every { response.body } answers {
            ByteArrayInputStream(
                """
                {
                  "valutakursgrunnlagId": 42,
                  "aktiv": false,
                  "brukFra": "2024-07-01T00:00:00",
                  "brukTil": "2025-01-01T00:00:00",
                  "hentetTidspunkt": "2024-07-01T12:00:00",
                  "observasjonsdato": "2024-06-01",
                  "kurs": 11.45,
                  "multiplikator": 0,
                  "basisvaluta": "EUR",
                  "kvoteringsvaluta": "NOK",
                  "feiletHenting": false,
                  "status": "HENTET"
                }
                """.trimIndent().toByteArray(),
            )
        }

        val resultat = consumer.hentValutakurs(Valutakode.EUR, LocalDate.of(2024, 7, 1))

        resultat.brukFra shouldBe LocalDate.of(2024, 7, 1).atStartOfDay()
        resultat.brukTil shouldBe LocalDate.of(2025, 1, 1).atStartOfDay()
        resultat.kurs shouldBe BigDecimal("11.45")
        verify(exactly = 1) {
            factory.createRequest(URI("https://bidrag-grunnlag.test/prefiks/valutakursgrunnlag?valutakode=EUR&dato=2024-07-01"), HttpMethod.GET)
        }
        verify { response.close() }
    }

    @ParameterizedTest
    @ValueSource(ints = [404, 422, 503])
    fun `skal videreføre feil fra bidrag-grunnlag uten reservekurs`(status: Int) {
        every { factory.createRequest(any(), HttpMethod.GET) } returns request
        every { request.execute() } returns response
        every { response.statusCode } returns HttpStatus.valueOf(status)
        every { response.statusText } returns "Kurs er ikke tilgjengelig"
        every { response.headers } returns HttpHeaders()
        every { response.body } answers { ByteArrayInputStream(ByteArray(0)) }

        shouldThrow<RestClientResponseException> {
            consumer.hentValutakurs(Valutakode.EUR, LocalDate.of(2024, 7, 1))
        }.statusCode.value() shouldBe status
    }
}
