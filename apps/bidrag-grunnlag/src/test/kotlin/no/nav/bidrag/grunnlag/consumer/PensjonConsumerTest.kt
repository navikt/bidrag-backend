package no.nav.bidrag.grunnlag.consumer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.pensjon.PensjonConsumer
import no.nav.bidrag.grunnlag.consumer.pensjon.api.BarnetilleggPensjon
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
import java.net.URI

@ExtendWith(MockKExtension::class)
@DisplayName("PensjonConsumerTest")
internal class PensjonConsumerTest {
    @MockK
    private lateinit var restTemplateMock: RestTemplate

    @MockK
    private lateinit var grunnlagConsumerMock: GrunnlagConsumer

    private lateinit var pensjonConsumer: PensjonConsumer

    @BeforeEach
    fun setup() {
        pensjonConsumer = PensjonConsumer(
            URI("http://localhost"),
            restTemplateMock,
            grunnlagConsumerMock,
        )
    }

    @Test
    fun `hentBarnetilleggPensjon skal returnere ok respons`() {
        val request = TestUtil.byggHentBarnetilleggPensjonRequest()
        val response = TestUtil.byggHentBarnetilleggPensjonResponse()
        val httpEntity = HttpEntity(request)
        val responseEntity = ResponseEntity(response, HttpStatus.OK)
        val responseType = object : ParameterizedTypeReference<List<BarnetilleggPensjon>>() {}

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<List<BarnetilleggPensjon>>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/pen/api/barnetillegg/search"),
                HttpMethod.POST,
                httpEntity,
                responseType,
            )
        } returns responseEntity

        // Consumer-kall
        val restResponse = pensjonConsumer.hentBarnetilleggPensjon(request)

        // Assertions
        restResponse is RestResponse.Success
        (restResponse as RestResponse.Success).body shouldBe response
    }

    @Test
    fun `hentBarnetilleggPensjon skal håndtere exception`() {
        val request = TestUtil.byggHentBarnetilleggPensjonRequest()
        val httpEntity = HttpEntity(request)
        val responseType = object : ParameterizedTypeReference<List<BarnetilleggPensjon>>() {}

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<List<BarnetilleggPensjon>>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/pen/api/barnetillegg/search"),
                HttpMethod.POST,
                httpEntity,
                responseType,
            )
        } throws HttpClientErrorException(HttpStatus.BAD_REQUEST)

        // Consumer-kall
        val restResponse = pensjonConsumer.hentBarnetilleggPensjon(request)

        // Assertions
        restResponse is RestResponse.Failure
        (restResponse as RestResponse.Failure).statusCode shouldBe HttpStatus.BAD_REQUEST
    }
}
