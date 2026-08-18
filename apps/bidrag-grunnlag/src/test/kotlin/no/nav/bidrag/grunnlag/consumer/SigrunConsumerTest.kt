package no.nav.bidrag.grunnlag.consumer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.skattegrunnlag.SigrunConsumer
import no.nav.bidrag.grunnlag.consumer.skattegrunnlag.api.HentSummertSkattegrunnlagRequest
import no.nav.bidrag.grunnlag.consumer.skattegrunnlag.api.HentSummertSkattegrunnlagResponse
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
@DisplayName("SigrunConsumerTest")
internal class SigrunConsumerTest {
    @MockK
    private lateinit var restTemplateMock: RestTemplate

    @MockK
    private lateinit var grunnlagConsumerMock: GrunnlagConsumer

    private lateinit var sigrunConsumer: SigrunConsumer

    private val uri = URI("http://localhost")

    @BeforeEach
    fun setup() {
        sigrunConsumer = SigrunConsumer(
            uri,
            restTemplateMock,
            grunnlagConsumerMock,
        )
    }

    @Test
    fun `hentSummertSkattegrunnlag skal returnere ok respons`() {
        val request = TestUtil.byggHentSkattegrunnlagRequest()
        val response = TestUtil.byggHentSkattegrunnlagResponse()
        val httpEntity = HttpEntity(request)
        val responseEntity = ResponseEntity(response, HttpStatus.OK)

        every { grunnlagConsumerMock.initHttpEntitySkattegrunnlag(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<HentSummertSkattegrunnlagResponse>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create(uriBuilder(request)),
                HttpMethod.POST,
                httpEntity,
                HentSummertSkattegrunnlagResponse::class.java,
            )
        } returns responseEntity

        // Consumer-kall
        val restResponse = sigrunConsumer.hentSummertSkattegrunnlag(request)

        // Assertions
        restResponse is RestResponse.Success
        (restResponse as RestResponse.Success).body shouldBe response
    }

    @Test
    fun `hentSummertSkattegrunnlag skal håndtere exception`() {
        val request = TestUtil.byggHentSkattegrunnlagRequest()
        val httpEntity = HttpEntity(request)

        every { grunnlagConsumerMock.initHttpEntitySkattegrunnlag(body = request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<HentSummertSkattegrunnlagResponse>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create(uriBuilder(request)),
                HttpMethod.POST,
                httpEntity,
                HentSummertSkattegrunnlagResponse::class.java,
            )
        } throws HttpClientErrorException(HttpStatus.BAD_REQUEST)

        // Consumer-kall
        val restResponse = sigrunConsumer.hentSummertSkattegrunnlag(request)

        // Assertions
        restResponse is RestResponse.Failure
        (restResponse as RestResponse.Failure).statusCode shouldBe HttpStatus.BAD_REQUEST
    }

    private fun uriBuilder(request: HentSummertSkattegrunnlagRequest) = UriComponentsBuilder
        .fromUri(uri)
        .pathSegment("api/v2/summertskattegrunnlag")
        .build()
        .toUriString()
}
