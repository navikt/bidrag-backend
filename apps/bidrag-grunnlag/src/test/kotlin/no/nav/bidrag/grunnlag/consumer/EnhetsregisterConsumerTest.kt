package no.nav.bidrag.grunnlag.consumer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.EnhetsregisterConsumer
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api.HentEnhetsregisterRequest
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api.HentEnhetsregisterResponse
import no.nav.bidrag.grunnlag.exception.RestResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@ExtendWith(MockKExtension::class)
@DisplayName("EnhetsregisterConsumerTest")
internal class EnhetsregisterConsumerTest {
    @MockK
    private lateinit var restTemplateMock: RestTemplate

    @MockK
    private lateinit var grunnlagConsumerMock: GrunnlagConsumer

    private lateinit var enhetsregisterConsumer: EnhetsregisterConsumer

    private val uri = URI("http://localhost")

    @BeforeEach
    fun setup() {
        enhetsregisterConsumer = EnhetsregisterConsumer(
            uri,
            restTemplateMock,
            grunnlagConsumerMock,
        )
    }

    @Test
    fun `hentEnhetsinfo skal returnere ok respons`() {
        val organisasjonsnummer = "12345678"
        val request = HentEnhetsregisterRequest(organisasjonsnummer = organisasjonsnummer)
        val response = HentEnhetsregisterResponse(organisasjonsnummer = organisasjonsnummer)
        val httpEntity = HttpEntity(request)
        val responseEntity = ResponseEntity(response, HttpStatus.OK)

        every { grunnlagConsumerMock.initHttpEntityEreg(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<HentEnhetsregisterResponse>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create(uriBuilder(request)),
                HttpMethod.GET,
                httpEntity,
                HentEnhetsregisterResponse::class.java,
            )
        } returns responseEntity

        // Consumer-kall
        val restResponse = enhetsregisterConsumer.hentEnhetsinfo(request)

        // Assertions
        restResponse is RestResponse.Success
        (restResponse as RestResponse.Success).body shouldBe response
    }

    @Test
    fun `hentEnhetsinfo skal håndtere exception`() {
        val organisasjonsnummer = "12345678"
        val request = HentEnhetsregisterRequest(organisasjonsnummer = organisasjonsnummer)
        val httpEntity = HttpEntity(request)

        every { grunnlagConsumerMock.initHttpEntityEreg(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<HentEnhetsregisterResponse>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create(uriBuilder(request)),
                HttpMethod.GET,
                httpEntity,
                HentEnhetsregisterResponse::class.java,
            )
        } throws HttpClientErrorException(HttpStatus.BAD_REQUEST)

        // Consumer-kall
        val restResponse = enhetsregisterConsumer.hentEnhetsinfo(request)

        // Assertions
        restResponse is RestResponse.Failure
        (restResponse as RestResponse.Failure).statusCode shouldBe HttpStatus.BAD_REQUEST
    }

    private fun uriBuilder(request: HentEnhetsregisterRequest) = UriComponentsBuilder
        .fromUri(uri)
        .pathSegment(byggEregUrl(request))
        .build()
        .toUriString()

    private fun byggEregUrl(request: HentEnhetsregisterRequest): String {
        val url = "v2/organisasjon/${request.organisasjonsnummer}/noekkelinfo"
        return if (!request.gyldigDato.isNullOrBlank()) url.plus("?gyldigDato=${request.gyldigDato}") else url
    }
}
