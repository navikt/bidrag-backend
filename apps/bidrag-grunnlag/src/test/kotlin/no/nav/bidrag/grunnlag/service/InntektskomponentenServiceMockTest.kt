package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.InntektskomponentenConsumer
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.HentInntektListeRequest
import no.nav.bidrag.grunnlag.exception.RestResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.math.BigDecimal

@DisplayName("InntektskomponentenServiceMockTest")
@ExtendWith(MockitoExtension::class)
class InntektskomponentenServiceMockTest {

    @InjectMocks
    private lateinit var inntektskomponentenService: InntektskomponentenService

    @Mock
    private lateinit var inntektskomponentenConsumerMock: InntektskomponentenConsumer

    @Test
    fun `OK respons ved hent av abonnerte inntekter`() {
        val hentInntektListeRequest = TestUtil.byggHentInntektListeRequest()
        val hentInntektListeResponse = TestUtil.byggHentInntektListeResponse()
        Mockito.`when`(inntektskomponentenConsumerMock.hentInntekter(hentInntektListeRequest, true))
            .thenReturn(RestResponse.Success(hentInntektListeResponse))
        val hentInntektListeResponsIntern = inntektskomponentenService.hentInntekt(hentInntektListeRequest)

        Mockito.verify(inntektskomponentenConsumerMock, Mockito.times(1))
            .hentInntekter(MockitoHelper.any(HentInntektListeRequest::class.java), MockitoHelper.any(Boolean::class.java))

        assertAll(
            { assertThat(hentInntektListeResponsIntern).isNotNull() },
            { assertThat(hentInntektListeResponsIntern.httpStatus.is2xxSuccessful) },
            { assertThat(hentInntektListeResponsIntern.arbeidsInntektMaanedIntern?.isNotEmpty()) },
            { assertThat(hentInntektListeResponsIntern.arbeidsInntektMaanedIntern?.size).isEqualTo(1) },
            {
                assertThat(
                    hentInntektListeResponsIntern.arbeidsInntektMaanedIntern?.get(0)?.arbeidsInntektInformasjonIntern?.inntektIntern?.isNotEmpty(),
                )
            },
            {
                assertThat(
                    hentInntektListeResponsIntern.arbeidsInntektMaanedIntern?.get(0)?.arbeidsInntektInformasjonIntern?.inntektIntern?.size,
                ).isEqualTo(
                    1,
                )
            },
            {
                assertThat(
                    hentInntektListeResponsIntern.arbeidsInntektMaanedIntern?.get(0)?.arbeidsInntektInformasjonIntern?.inntektIntern?.get(0)?.beloep,
                ).isEqualTo(
                    BigDecimal.valueOf(10000),
                )
            },
        )
    }

    @Test
    fun `Feil ved hent av abonnerte inntekter, henter inntekter uten abonnement`() {
        val hentInntektListeRequest = TestUtil.byggHentInntektListeRequest()
        val hentInntektListeResponse = TestUtil.byggHentInntektListeResponse()
        Mockito.`when`(inntektskomponentenConsumerMock.hentInntekter(hentInntektListeRequest, true))
            .thenReturn(RestResponse.Failure("Feilmelding", HttpStatus.NOT_FOUND, HttpClientErrorException(HttpStatus.NOT_FOUND)))
        Mockito.`when`(inntektskomponentenConsumerMock.hentInntekter(hentInntektListeRequest, false))
            .thenReturn(RestResponse.Success(hentInntektListeResponse))
        val hentInntektListeResponsIntern = inntektskomponentenService.hentInntekt(hentInntektListeRequest)

        Mockito.verify(inntektskomponentenConsumerMock, Mockito.times(2))
            .hentInntekter(MockitoHelper.any(HentInntektListeRequest::class.java), MockitoHelper.any(Boolean::class.java))

        assertAll(
            { assertThat(hentInntektListeResponsIntern).isNotNull() },
            { assertThat(hentInntektListeResponsIntern.httpStatus.is2xxSuccessful) },
            { assertThat(hentInntektListeResponsIntern.arbeidsInntektMaanedIntern?.isNotEmpty()) },
            { assertThat(hentInntektListeResponsIntern.arbeidsInntektMaanedIntern?.size).isEqualTo(1) },
            {
                assertThat(
                    hentInntektListeResponsIntern.arbeidsInntektMaanedIntern?.get(0)?.arbeidsInntektInformasjonIntern?.inntektIntern?.isNotEmpty(),
                )
            },
            {
                assertThat(
                    hentInntektListeResponsIntern.arbeidsInntektMaanedIntern?.get(0)?.arbeidsInntektInformasjonIntern?.inntektIntern?.size,
                ).isEqualTo(
                    1,
                )
            },
            {
                assertThat(
                    hentInntektListeResponsIntern.arbeidsInntektMaanedIntern?.get(0)?.arbeidsInntektInformasjonIntern?.inntektIntern?.get(0)?.beloep,
                ).isEqualTo(
                    BigDecimal.valueOf(10000),
                )
            },
        )
    }

    @Test
    fun `Feil ved hent av abonnerte inntekter og feil ved hent av inntekter uten abonnement`() {
        val hentInntektListeRequest = TestUtil.byggHentInntektListeRequest()
        Mockito.`when`(inntektskomponentenConsumerMock.hentInntekter(hentInntektListeRequest, true))
            .thenReturn(RestResponse.Failure("Feilmelding", HttpStatus.NOT_FOUND, HttpClientErrorException(HttpStatus.NOT_FOUND)))
        Mockito.`when`(inntektskomponentenConsumerMock.hentInntekter(hentInntektListeRequest, false))
            .thenReturn(RestResponse.Failure("Feilmelding", HttpStatus.NOT_FOUND, HttpClientErrorException(HttpStatus.NOT_FOUND)))
        val hentInntektListeResponsIntern = inntektskomponentenService.hentInntekt(hentInntektListeRequest)

        Mockito.verify(inntektskomponentenConsumerMock, Mockito.times(2))
            .hentInntekter(MockitoHelper.any(HentInntektListeRequest::class.java), MockitoHelper.any(Boolean::class.java))

        assertAll(
            { assertThat(hentInntektListeResponsIntern).isNotNull() },
            { assertThat(hentInntektListeResponsIntern.httpStatus.is4xxClientError) },
            { assertThat(hentInntektListeResponsIntern.arbeidsInntektMaanedIntern?.isEmpty()) },
        )
    }
}

object MockitoHelper {
    fun <T> any(type: Class<T>): T = Mockito.any(type)
    fun <T> any(): T = Mockito.any()
}
