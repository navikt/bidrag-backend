package no.nav.bidrag.grunnlag.consumer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.aap.AapConsumer
import no.nav.bidrag.grunnlag.consumer.aap.api.HentBarnetilleggAAPResponse
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.TilleggsstønadResponse
import no.nav.bidrag.grunnlag.consumer.tilleggsstønad.TilleggsstønadConsumer
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
import java.net.URI

@ExtendWith(MockKExtension::class)
@DisplayName("TilleggsstønadConsumerTest")
internal class AapConsumerTest {
    @MockK
    private lateinit var restTemplateMock: RestTemplate

    @MockK
    private lateinit var grunnlagConsumerMock: GrunnlagConsumer

    private lateinit var aapConsumer: AapConsumer

    @BeforeEach
    fun setup() {
        aapConsumer = AapConsumer(
            URI("http://localhost"),
            restTemplateMock,
            grunnlagConsumerMock,
        )
    }

    @Test
    fun `hentBarnetillegg skal returnere ok respons`() {
        val request = TestUtil.byggHentBarnetilleggAapRequest()
        val response = TestUtil.byggHentBarnetilleggAapResponse()
        val httpEntity = HttpEntity(request)
        val responseEntity = ResponseEntity(response, HttpStatus.OK)

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<HentBarnetilleggAAPResponse>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/bisys/barnetillegg"),
                HttpMethod.POST,
                httpEntity,
                HentBarnetilleggAAPResponse::class.java,
            )
        } returns responseEntity

        // Consumer-kall
        val restResponse = aapConsumer.hentBarnetillegg(request)

        // Assertions
        restResponse is RestResponse.Success
        (restResponse as RestResponse.Success).body shouldBe response
    }

    @Test
    fun `hentBarnetillegg skal håndtere exception`() {
        val request = TestUtil.byggHentBarnetilleggAapRequest()
        val httpEntity = HttpEntity(request)

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<HentBarnetilleggAAPResponse>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/bisys/barnetillegg"),
                HttpMethod.POST,
                httpEntity,
                HentBarnetilleggAAPResponse::class.java,
            )
        } throws HttpClientErrorException(HttpStatus.BAD_REQUEST)

        // Consumer-kall
        val restResponse = aapConsumer.hentBarnetillegg(request)

        // Assertions
        restResponse is RestResponse.Failure
        (restResponse as RestResponse.Failure).statusCode shouldBe HttpStatus.BAD_REQUEST
    }
}
