package no.nav.bidrag.grunnlag.consumer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.InntektskomponentenConsumer
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.tjenester.aordningen.inntektsinformasjon.response.HentInntektListeResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.util.function.Supplier

@ExtendWith(MockKExtension::class)
@DisplayName("InntektskomponentenConsumerTest")
internal class InntektskomponentenConsumerTest {
    @MockK
    private lateinit var restTemplateMock: RestTemplate

    @MockK
    private lateinit var grunnlagConsumerMock: GrunnlagConsumer

    private lateinit var inntektskomponentenConsumer: InntektskomponentenConsumer

    @MockK
    private lateinit var circuitBreakerFactoryMock: CircuitBreakerFactory<*, *>

    @MockK
    private lateinit var circuitBreakerMock: CircuitBreaker

    @BeforeEach
    fun setup() {
        every { circuitBreakerFactoryMock.create(any()) } returns circuitBreakerMock
        every { circuitBreakerMock.run(any<Supplier<RestResponse<HentInntektListeResponse>>>(), any()) } answers {
            firstArg<Supplier<RestResponse<HentInntektListeResponse>>>().get()
        }
        inntektskomponentenConsumer = InntektskomponentenConsumer(
            URI("http://localhost"),
            restTemplateMock,
            grunnlagConsumerMock,
            circuitBreakerFactoryMock,
        )
    }

    @Test
    fun `hentInntekter skal returnere ok respons`() {
        val request = TestUtil.byggHentInntektListeRequest()
        val response = TestUtil.byggHentInntektListeResponse()
        val httpEntity = HttpEntity(request)
        val responseEntity = ResponseEntity(response, HttpStatus.OK)

        every { grunnlagConsumerMock.initHttpEntityInntektskomponenten(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any<RestResponse<HentInntektListeResponse>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/rs/api/v1/hentinntektliste"),
                HttpMethod.POST,
                httpEntity,
                HentInntektListeResponse::class.java,
            )
        } returns responseEntity

        // Consumer-kall
        val restResponse = inntektskomponentenConsumer.hentInntekter(request = request, abonnerteInntekterRequest = false)

        // Assertions
        restResponse is RestResponse.Success
        (restResponse as RestResponse.Success).body shouldBe response
    }

    @Test
    fun `hentInntekter skal håndtere exception`() {
        val request = TestUtil.byggHentInntektListeRequest()
        val httpEntity = HttpEntity(request)

        every { grunnlagConsumerMock.initHttpEntityInntektskomponenten(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any<RestResponse<HentInntektListeResponse>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/rs/api/v1/hentinntektliste"),
                HttpMethod.POST,
                httpEntity,
                HentInntektListeResponse::class.java,
            )
        } throws HttpClientErrorException(HttpStatus.BAD_REQUEST)

        // Consumer-kall
        val restResponse = inntektskomponentenConsumer.hentInntekter(request = request, abonnerteInntekterRequest = false)

        // Assertions
        restResponse is RestResponse.Failure
        (restResponse as RestResponse.Failure).statusCode shouldBe HttpStatus.BAD_REQUEST
    }
}
