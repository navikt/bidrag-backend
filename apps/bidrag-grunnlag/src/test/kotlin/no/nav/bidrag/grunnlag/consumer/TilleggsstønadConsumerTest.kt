package no.nav.bidrag.grunnlag.consumer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import no.nav.bidrag.grunnlag.TestUtil
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
internal class TilleggsstønadConsumerTest {
    @MockK
    private lateinit var restTemplateMock: RestTemplate

    @MockK
    private lateinit var grunnlagConsumerMock: GrunnlagConsumer

    private lateinit var tilleggsstønadConsumer: TilleggsstønadConsumer

    @BeforeEach
    fun setup() {
        tilleggsstønadConsumer = TilleggsstønadConsumer(
            URI("http://localhost"),
            restTemplateMock,
            grunnlagConsumerMock,
        )
    }

    @Test
    fun `hentTilleggsstønad skal returnere ok respons`() {
        val request = TestUtil.byggTilleggsstønadRequest()
        val response = TestUtil.byggTilleggsstønadResponse()
        val httpEntity = HttpEntity(request)
        val responseEntity = ResponseEntity(response, HttpStatus.OK)

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<TilleggsstønadResponse>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/api/ekstern/vedtak/tilsyn-barn"),
                HttpMethod.POST,
                httpEntity,
                TilleggsstønadResponse::class.java,
            )
        } returns responseEntity

        // Consumer-kall
        val restResponse = tilleggsstønadConsumer.hentTilleggsstønad(request)

        // Assertions
        restResponse is RestResponse.Success
        (restResponse as RestResponse.Success).body shouldBe response
    }

    @Test
    fun `hentTilleggsstønad skal håndtere exception`() {
        val request = TestUtil.byggTilleggsstønadRequest()
        val httpEntity = HttpEntity(request)

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<TilleggsstønadResponse>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/api/ekstern/vedtak/tilsyn-barn"),
                HttpMethod.POST,
                httpEntity,
                TilleggsstønadResponse::class.java,
            )
        } throws HttpClientErrorException(HttpStatus.BAD_REQUEST)

        // Consumer-kall
        val restResponse = tilleggsstønadConsumer.hentTilleggsstønad(request)

        // Assertions
        restResponse is RestResponse.Failure
        (restResponse as RestResponse.Failure).statusCode shouldBe HttpStatus.BAD_REQUEST
    }
}
