package no.nav.bidrag.organisasjon.consumer

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.bidrag.organisasjon.consumer.dto.SkjermingRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange

@ExtendWith(MockKExtension::class)
internal class SkjermingConsumerTest {
    @InjectMockKs
    private lateinit var skjermingConsumer: SkjermingConsumer

    @RelaxedMockK
    private lateinit var restTemplateMock: RestTemplate

    @Test
    fun `skal bruke riktige parametre i sti til tjeneste`() {
        every {
            restTemplateMock.exchange<Boolean>(
                any<String>(),
                any(),
                any(),
            )
        } returns responseEntity()
        val headers = initRequestHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val skjermingRequest = SkjermingRequest(IDENT)
        skjermingConsumer.erPersonSkjermet(skjermingRequest)
        verify {
            restTemplateMock
                .exchange<Boolean>(
                    "/skjermet",
                    HttpMethod.POST,
                    HttpEntity<Any>(skjermingRequest, headers),
                )
        }
    }

    @Test
    fun `sjekk at respons fra Skjerming API mappes korrekt`() {
        every {
            restTemplateMock.exchange<Boolean>(
                any<String>(),
                any(),
                any(),
            )
        } returns responseEntity()
        val skjermingResponse = skjermingConsumer.erPersonSkjermet(SkjermingRequest(IDENT))
        assertSoftly {
            skjermingResponse shouldBe true
        }
    }

    companion object {
        private const val IDENT = "12345678900"

        private fun responseEntity(): ResponseEntity<Boolean> = ResponseEntity(true, HttpStatus.OK)
    }
}
