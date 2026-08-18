package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.grunnlag.HentGrunnlagFeiltype
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.familiebasak.FamilieBaSakConsumer
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.BisysStønadstype
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
class HentUtvidetBarnetrygdOgSmåbarnstilleggServiceMockTest {

    @InjectMocks
    private lateinit var hentUbstService: HentUtvidetBarnetrygdOgSmåbarnstilleggService

    @Mock
    private lateinit var ubstConsumerMock: FamilieBaSakConsumer

    @Test
    fun `Skal returnere grunnlag og ikke feil når consumer-response er SUCCESS`() {
        Mockito.`when`(ubstConsumerMock.hentFamilieBaSak(any())).thenReturn(RestResponse.Success(TestUtil.byggFamilieBaSakResponse()))

        val ubstRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val ubstListe = hentUbstService.hentUbst(
            ubstRequestListe = ubstRequestListe,
        )

        Mockito.verify(ubstConsumerMock, Mockito.times(1)).hentFamilieBaSak(any())

        assertAll(
            { assertThat(ubstListe).isNotNull() },
            { assertThat(ubstListe.grunnlagListe).isNotEmpty() },
            { assertThat(ubstListe.grunnlagListe).hasSize(2) },
            { assertThat(ubstListe.grunnlagListe[0].type).isEqualTo(BisysStønadstype.UTVIDET.toString()) },
            { assertThat(ubstListe.grunnlagListe[0].beløp).isEqualTo(BigDecimal.valueOf(1000.11)) },
            { assertThat(ubstListe.grunnlagListe[1].type).isEqualTo(BisysStønadstype.UTVIDET.toString()) },
            { assertThat(ubstListe.grunnlagListe[1].beløp).isEqualTo(BigDecimal.valueOf(2000.22)) },
            { assertThat(ubstListe.feilrapporteringListe).isEmpty() },
        )
    }

    @Test
    fun `Skal returnere feil og tomt grunnlag fra utvidet barnetrygd og småbarnstillegg når consumer-response er FAILURE`() {
        Mockito.`when`(ubstConsumerMock.hentFamilieBaSak(any())).thenReturn(
            RestResponse.Failure(
                message = "Ikke funnet",
                statusCode = HttpStatus.NOT_FOUND,
                restClientException = HttpClientErrorException(HttpStatus.NOT_FOUND),
            ),
        )

        val ubstRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val ubstListe = hentUbstService.hentUbst(
            ubstRequestListe = ubstRequestListe,
        )

        Mockito.verify(ubstConsumerMock, Mockito.times(1)).hentFamilieBaSak(any())

        assertAll(
            { assertThat(ubstListe).isNotNull() },
            { assertThat(ubstListe.grunnlagListe).isEmpty() },
            { assertThat(ubstListe.feilrapporteringListe).hasSize(1) },
            { assertThat(ubstListe.feilrapporteringListe[0].grunnlagstype).isEqualTo(GrunnlagRequestType.UTVIDET_BARNETRYGD_OG_SMÅBARNSTILLEGG) },
            { assertThat(ubstListe.feilrapporteringListe[0].personId).isEqualTo(ubstRequestListe[0].personId) },
            { assertThat(ubstListe.feilrapporteringListe[0].periodeFra).isEqualTo(ubstRequestListe[0].periodeFra) },
            { assertThat(ubstListe.feilrapporteringListe[0].periodeTil).isNull() },
            { assertThat(ubstListe.feilrapporteringListe[0].feiltype).isEqualTo(HentGrunnlagFeiltype.FUNKSJONELL_FEIL) },
            { assertThat(ubstListe.feilrapporteringListe[0].feilmelding).isEqualTo("Ikke funnet") },
        )
    }
}
