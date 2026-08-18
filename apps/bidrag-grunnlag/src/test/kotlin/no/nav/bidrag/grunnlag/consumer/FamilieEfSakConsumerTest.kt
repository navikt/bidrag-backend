package no.nav.bidrag.grunnlag.consumer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.familieefsak.FamilieEfSakConsumer
import no.nav.bidrag.grunnlag.consumer.familieefsak.api.BarnetilsynRequest
import no.nav.bidrag.grunnlag.consumer.familieefsak.api.BarnetilsynResponse
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
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@DisplayName("FamilieEfSakConsumerTest")
internal class FamilieEfSakConsumerTest {
    @MockK
    private lateinit var restTemplateMock: RestTemplate

    @MockK
    private lateinit var grunnlagConsumerMock: GrunnlagConsumer

    private lateinit var familieEfSakConsumer: FamilieEfSakConsumer

    @BeforeEach
    fun setup() {
        familieEfSakConsumer = FamilieEfSakConsumer(
            URI("http://localhost"),
            restTemplateMock,
            grunnlagConsumerMock,
        )
    }

    @Test
    fun `hentFamilieEfSak skal returnere ok respons`() {
        val personident = "12345678901"
        val request = BarnetilsynRequest(ident = personident, fomDato = LocalDate.now())
        val response = TestUtil.byggBarnetilsynResponse()
        val httpEntity = HttpEntity(request)
        val responseEntity = ResponseEntity(response, HttpStatus.OK)

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<BarnetilsynResponse>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/api/ekstern/bisys/perioder-barnetilsyn"),
                HttpMethod.POST,
                httpEntity,
                BarnetilsynResponse::class.java,
            )
        } returns responseEntity

        // Consumer-kall
        val restResponse = familieEfSakConsumer.hentBarnetilsyn(request)

        // Assertions
        restResponse is RestResponse.Success
        (restResponse as RestResponse.Success).body shouldBe response
    }

    @Test
    fun `hentFamilieEfSak skal håndtere exception`() {
        val personident = "12345678901"
        val request = BarnetilsynRequest(ident = personident, fomDato = LocalDate.now())
        val httpEntity = HttpEntity(request)

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<BarnetilsynResponse>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/api/ekstern/bisys/perioder-barnetilsyn"),
                HttpMethod.POST,
                httpEntity,
                BarnetilsynResponse::class.java,
            )
        } throws HttpClientErrorException(HttpStatus.BAD_REQUEST)

        // Consumer-kall
        val restResponse = familieEfSakConsumer.hentBarnetilsyn(request)

        // Assertions
        restResponse is RestResponse.Failure
        (restResponse as RestResponse.Failure).statusCode shouldBe HttpStatus.BAD_REQUEST
    }
}
