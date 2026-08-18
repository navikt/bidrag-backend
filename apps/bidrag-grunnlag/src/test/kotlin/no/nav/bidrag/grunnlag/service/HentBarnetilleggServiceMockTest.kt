package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.grunnlag.HentGrunnlagFeiltype
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.pensjon.PensjonConsumer
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
class HentBarnetilleggServiceMockTest {

    @InjectMocks
    private lateinit var hentBarnetilleggService: HentBarnetilleggService

    @Mock
    private lateinit var pensjonConsumerMock: PensjonConsumer

    @Test
    fun `Skal returnere grunnlag og ikke feil når consumer-response er SUCCESS`() {
        Mockito.`when`(pensjonConsumerMock.hentBarnetilleggPensjon(any()))
            .thenReturn(RestResponse.Success(TestUtil.byggHentBarnetilleggPensjonResponse()))

        val barnetilleggPensjonRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val barnetilleggPensjonListe = hentBarnetilleggService.hentBarnetilleggPensjon(
            barnetilleggPensjonRequestListe = barnetilleggPensjonRequestListe,
        )

        Mockito.verify(pensjonConsumerMock, Mockito.times(1)).hentBarnetilleggPensjon(any())

        assertAll(
            { assertThat(barnetilleggPensjonListe).isNotNull() },
            { assertThat(barnetilleggPensjonListe.grunnlagListe).isNotEmpty() },
            { assertThat(barnetilleggPensjonListe.grunnlagListe).hasSize(2) },
            { assertThat(barnetilleggPensjonListe.grunnlagListe[0].beløpBrutto).isEqualTo(BigDecimal.valueOf(1001.45)) },
            { assertThat(barnetilleggPensjonListe.grunnlagListe[1].beløpBrutto).isEqualTo(BigDecimal.valueOf(2002.5)) },
            { assertThat(barnetilleggPensjonListe.feilrapporteringListe).isEmpty() },
        )
    }

    @Test
    fun `Skal returnere feil og tomt grunnlag fra barnetillegg når consumer-response er FAILURE`() {
        Mockito.`when`(pensjonConsumerMock.hentBarnetilleggPensjon(any())).thenReturn(
            RestResponse.Failure(
                message = "Ikke funnet",
                statusCode = HttpStatus.NOT_FOUND,
                restClientException = HttpClientErrorException(HttpStatus.NOT_FOUND),
            ),
        )

        val barnetilleggPensjonRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val barnetilleggPensjonListe = hentBarnetilleggService.hentBarnetilleggPensjon(
            barnetilleggPensjonRequestListe = barnetilleggPensjonRequestListe,
        )

        Mockito.verify(pensjonConsumerMock, Mockito.times(1)).hentBarnetilleggPensjon(any())

        assertAll(
            { assertThat(barnetilleggPensjonListe).isNotNull() },
            { assertThat(barnetilleggPensjonListe.grunnlagListe).isEmpty() },
            { assertThat(barnetilleggPensjonListe.feilrapporteringListe).isNotEmpty() },
            { assertThat(barnetilleggPensjonListe.feilrapporteringListe).hasSize(1) },
            { assertThat(barnetilleggPensjonListe.feilrapporteringListe[0].grunnlagstype).isEqualTo(GrunnlagRequestType.BARNETILLEGG) },
            { assertThat(barnetilleggPensjonListe.feilrapporteringListe[0].personId).isEqualTo(barnetilleggPensjonRequestListe[0].personId) },
            { assertThat(barnetilleggPensjonListe.feilrapporteringListe[0].periodeFra).isEqualTo(barnetilleggPensjonRequestListe[0].periodeFra) },
            {
                assertThat(barnetilleggPensjonListe.feilrapporteringListe[0].periodeTil).isEqualTo(
                    barnetilleggPensjonRequestListe[0].periodeTil.minusDays(1),
                )
            },
            { assertThat(barnetilleggPensjonListe.feilrapporteringListe[0].feiltype).isEqualTo(HentGrunnlagFeiltype.FUNKSJONELL_FEIL) },
            { assertThat(barnetilleggPensjonListe.feilrapporteringListe[0].feilmelding).isEqualTo("Ikke funnet") },
        )
    }
}
