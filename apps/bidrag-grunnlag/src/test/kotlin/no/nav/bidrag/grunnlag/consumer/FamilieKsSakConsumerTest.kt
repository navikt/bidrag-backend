package no.nav.bidrag.grunnlag.consumer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.familiekssak.FamilieKsSakConsumer
import no.nav.bidrag.grunnlag.consumer.familiekssak.api.BisysDto
import no.nav.bidrag.grunnlag.consumer.familiekssak.api.BisysResponsDto
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
@DisplayName("FamilieKsSakConsumerTest")
internal class FamilieKsSakConsumerTest {
    @MockK
    private lateinit var restTemplateMock: RestTemplate

    @MockK
    private lateinit var grunnlagConsumerMock: GrunnlagConsumer

    private lateinit var familieKsSakConsumer: FamilieKsSakConsumer

    @BeforeEach
    fun setup() {
        familieKsSakConsumer = FamilieKsSakConsumer(
            URI("http://localhost"),
            restTemplateMock,
            grunnlagConsumerMock,
        )
    }

    @Test
    fun `hentFamilieKsSak skal returnere ok respons`() {
        val personident = "12345678901"
        val request = BisysDto(fom = LocalDate.now(), identer = listOf(personident))
        val response = TestUtil.byggKontantstøtteResponse()
        val httpEntity = HttpEntity(request)
        val responseEntity = ResponseEntity(response, HttpStatus.OK)

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<BisysResponsDto>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/api/bisys/hent-utbetalingsinfo"),
                HttpMethod.POST,
                httpEntity,
                BisysResponsDto::class.java,
            )
        } returns responseEntity

        // Consumer-kall
        val restResponse = familieKsSakConsumer.hentKontantstøtte(request)

        // Assertions
        restResponse is RestResponse.Success
        (restResponse as RestResponse.Success).body shouldBe response
    }

    @Test
    fun `hentFamilieKsSak skal håndtere exception`() {
        val personident = "12345678901"
        val request = BisysDto(fom = LocalDate.now(), identer = listOf(personident))
        val httpEntity = HttpEntity(request)

        every { grunnlagConsumerMock.initHttpEntity(request) } returns httpEntity

        every {
            grunnlagConsumerMock.logResponse(any(), any(), any(), any(), any<RestResponse<BisysResponsDto>>())
        } just runs

        every {
            restTemplateMock.exchange(
                URI.create("http://localhost/api/bisys/hent-utbetalingsinfo"),
                HttpMethod.POST,
                httpEntity,
                BisysResponsDto::class.java,
            )
        } throws HttpClientErrorException(HttpStatus.BAD_REQUEST)

        // Consumer-kall
        val restResponse = familieKsSakConsumer.hentKontantstøtte(request)

        // Assertions
        restResponse is RestResponse.Failure
        (restResponse as RestResponse.Failure).statusCode shouldBe HttpStatus.BAD_REQUEST
    }
}
