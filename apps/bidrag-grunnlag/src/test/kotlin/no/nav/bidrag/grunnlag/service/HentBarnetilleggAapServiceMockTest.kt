package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.grunnlag.HentGrunnlagFeiltype
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.aap.AapConsumer
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.any
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class HentBarnetilleggAapServiceMockTest {

    @InjectMocks
    private lateinit var hentBarnetilleggAAPService: HentBarnetilleggAAPService

    @Mock
    private lateinit var aapConsumerMock: AapConsumer

    @Test
    fun `Skal returnere grunnlag og ikke feil når consumer-response er SUCCESS`() {
        Mockito.`when`(aapConsumerMock.hentBarnetillegg(any()))
            .thenReturn(RestResponse.Success(TestUtil.byggHentBarnetilleggAapResponse()))

        val barnetilleggPensjonRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val barnetilleggAapListe = hentBarnetilleggAAPService.hentBarnetillegg(
            request = barnetilleggPensjonRequestListe,
        )

        Mockito.verify(aapConsumerMock, Mockito.times(1)).hentBarnetillegg(any())

        assertAll(
            { assertThat(barnetilleggAapListe).isNotNull() },
            { assertThat(barnetilleggAapListe.grunnlagListe).isNotEmpty() },
            { assertThat(barnetilleggAapListe.grunnlagListe).hasSize(1) },
            { assertThat(barnetilleggAapListe.grunnlagListe[0].beløpBrutto).isEqualTo(BigDecimal.ONE) },
            { assertThat(barnetilleggAapListe.feilrapporteringListe).isEmpty() },
        )
    }

    @Test
    fun `Skal returnere feil og tomt grunnlag fra barnetillegg når consumer-response er FAILURE`() {
        Mockito.`when`(aapConsumerMock.hentBarnetillegg(any())).thenReturn(
            RestResponse.Failure(
                message = "Ikke funnet",
                statusCode = HttpStatus.NOT_FOUND,
                restClientException = HttpClientErrorException(HttpStatus.NOT_FOUND),
            ),
        )

        val request = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val barnetilleggAapListe = hentBarnetilleggAAPService.hentBarnetillegg(
            request = request,
        )

        Mockito.verify(aapConsumerMock, Mockito.times(1)).hentBarnetillegg(any())

        assertAll(
            { assertThat(barnetilleggAapListe).isNotNull() },
            { assertThat(barnetilleggAapListe.grunnlagListe).isEmpty() },
            { assertThat(barnetilleggAapListe.feilrapporteringListe).isNotEmpty() },
            { assertThat(barnetilleggAapListe.feilrapporteringListe).hasSize(1) },
            { assertThat(barnetilleggAapListe.feilrapporteringListe[0].grunnlagstype).isEqualTo(GrunnlagRequestType.BARNETILLEGG) },
            { assertThat(barnetilleggAapListe.feilrapporteringListe[0].personId).isEqualTo(request[0].personId) },
            { assertThat(barnetilleggAapListe.feilrapporteringListe[0].feiltype).isEqualTo(HentGrunnlagFeiltype.FUNKSJONELL_FEIL) },
            { assertThat(barnetilleggAapListe.feilrapporteringListe[0].feilmelding).isEqualTo("Ikke funnet") },
        )
    }
}
