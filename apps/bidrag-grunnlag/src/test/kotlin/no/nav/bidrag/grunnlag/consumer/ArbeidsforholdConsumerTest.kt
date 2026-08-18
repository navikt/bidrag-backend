package no.nav.bidrag.grunnlag.consumer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.ArbeidsforholdConsumer
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api.Arbeidsforhold
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api.HentArbeidsforholdRequest
import no.nav.bidrag.grunnlag.exception.RestResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@ExtendWith(MockKExtension::class)
@DisplayName("ArbeidsforholdConsumerTest")
internal class ArbeidsforholdConsumerTest {
    @MockK
    private lateinit var restTemplateMock: RestTemplate

    @MockK
    private lateinit var grunnlagConsumerMock: GrunnlagConsumer

    private lateinit var arbeidsforholdConsumer: ArbeidsforholdConsumer

    private val uri = URI("http://localhost")

    @BeforeEach
    fun setup() {
        arbeidsforholdConsumer = ArbeidsforholdConsumer(
            uri,
            restTemplateMock,
            grunnlagConsumerMock,
        )
    }

    @Test
    fun `hentArbeidsforhold skal returnere ok respons`() {
        val personident = "12345678901"
        val request = HentArbeidsforholdRequest(arbeidstakerId = personident)
        val response = TestUtil.byggArbeidsforholdResponse()
        val httpEntity = HttpEntity(request)
        val responseEntity = ResponseEntity(response, HttpStatus.OK)
        val responseType = object : ParameterizedTypeReference<List<Arbeidsforhold>>() {}

        every { grunnlagConsumerMock.initHttpEntityAareg(body = request, ident = request.arbeidstakerId) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<List<Arbeidsforhold>>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create(uriBuilder()),
                HttpMethod.GET,
                httpEntity,
                responseType,
            )
        } returns responseEntity

        // Consumer-kall
        val restResponse = arbeidsforholdConsumer.hentArbeidsforhold(request)

        // Assertions
        restResponse is RestResponse.Success
        (restResponse as RestResponse.Success).body shouldBe response
    }

    @Test
    fun `hentArbeidsforhold skal håndtere exception`() {
        val personident = "12345678901"
        val request = HentArbeidsforholdRequest(arbeidstakerId = personident)
        val httpEntity = HttpEntity(request)
        val responseType = object : ParameterizedTypeReference<List<Arbeidsforhold>>() {}

        every { grunnlagConsumerMock.initHttpEntityAareg(body = request, ident = request.arbeidstakerId) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<List<Arbeidsforhold>>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create(uriBuilder()),
                HttpMethod.GET,
                httpEntity,
                responseType,
            )
        } throws HttpClientErrorException(HttpStatus.BAD_REQUEST)

        // Consumer-kall
        val restResponse = arbeidsforholdConsumer.hentArbeidsforhold(request)

        // Assertions
        restResponse is RestResponse.Failure
        (restResponse as RestResponse.Failure).statusCode shouldBe HttpStatus.BAD_REQUEST
    }

    private fun uriBuilder() = UriComponentsBuilder
        .fromUri(uri)
        .pathSegment("api/v2/arbeidstaker/arbeidsforhold")
        .build()
        .toUriString()
}
