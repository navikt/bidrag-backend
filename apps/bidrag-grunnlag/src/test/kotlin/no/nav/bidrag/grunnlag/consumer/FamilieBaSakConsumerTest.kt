package no.nav.bidrag.grunnlag.consumer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.familiebasak.FamilieBaSakConsumer
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.FamilieBaSakRequest
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.FamilieBaSakResponse
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
@DisplayName("FamilieBaSakConsumerTest")
internal class FamilieBaSakConsumerTest {
    @MockK
    private lateinit var restTemplateMock: RestTemplate

    @MockK
    private lateinit var grunnlagConsumerMock: GrunnlagConsumer

    private lateinit var familieBaSakConsumer: FamilieBaSakConsumer

    @BeforeEach
    fun setup() {
        familieBaSakConsumer = FamilieBaSakConsumer(
            URI("http://localhost"),
            restTemplateMock,
            grunnlagConsumerMock,
        )
    }

    @Test
    fun `hentFamilieBaSak skal returnere ok respons`() {
        val personident = Personident("12345678901")
        val request = FamilieBaSakRequest(personIdent = personident.verdi, fraDato = LocalDate.now())
        val response = TestUtil.byggFamilieBaSakResponse()
        val httpEntity = HttpEntity(request)
        val responseEntity = ResponseEntity(response, HttpStatus.OK)

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<FamilieBaSakResponse>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/api/bisys/hent-utvidet-barnetrygd"),
                HttpMethod.POST,
                httpEntity,
                FamilieBaSakResponse::class.java,
            )
        } returns responseEntity

        // Consumer-kall
        val restResponse = familieBaSakConsumer.hentFamilieBaSak(request)

        // Assertions
        restResponse is RestResponse.Success
        (restResponse as RestResponse.Success).body shouldBe response
    }

    @Test
    fun `hentFamilieBaSak skal håndtere exception`() {
        val personident = Personident("12345678901")
        val request = FamilieBaSakRequest(personIdent = personident.verdi, fraDato = LocalDate.now())
        val httpEntity = HttpEntity(request)

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<FamilieBaSakResponse>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/api/bisys/hent-utvidet-barnetrygd"),
                HttpMethod.POST,
                httpEntity,
                FamilieBaSakResponse::class.java,
            )
        } throws HttpClientErrorException(HttpStatus.BAD_REQUEST)

        // Consumer-kall
        val restResponse = familieBaSakConsumer.hentFamilieBaSak(request)

        // Assertions
        restResponse is RestResponse.Failure
        (restResponse as RestResponse.Failure).statusCode shouldBe HttpStatus.BAD_REQUEST
    }
}
