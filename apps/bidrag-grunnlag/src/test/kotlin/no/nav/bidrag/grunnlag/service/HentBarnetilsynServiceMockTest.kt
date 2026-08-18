package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.grunnlag.HentGrunnlagFeiltype
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.familieefsak.FamilieEfSakConsumer
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

@ExtendWith(MockitoExtension::class)
class HentBarnetilsynServiceMockTest {

    @InjectMocks
    private lateinit var hentBarnetilsynService: HentBarnetilsynService

    @Mock
    private lateinit var barnetilsynConsumerMock: FamilieEfSakConsumer

    @Test
    fun `Skal returnere grunnlag og ikke feil når consumer-response er SUCCESS`() {
        Mockito.`when`(barnetilsynConsumerMock.hentBarnetilsyn(any())).thenReturn(RestResponse.Success(TestUtil.byggBarnetilsynResponse()))

        val barnetilsynRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val barnetilsynListe = hentBarnetilsynService.hentBarnetilsyn(
            barnetilsynRequestListe = barnetilsynRequestListe,
        )

        Mockito.verify(barnetilsynConsumerMock, Mockito.times(1)).hentBarnetilsyn(any())

        assertAll(
            { assertThat(barnetilsynListe).isNotNull() },
            { assertThat(barnetilsynListe.grunnlagListe).isNotEmpty() },
            { assertThat(barnetilsynListe.grunnlagListe).hasSize(2) },
            { assertThat(barnetilsynListe.grunnlagListe[0].barnPersonId).isEqualTo(TestUtil.BARN_PERSON_ID_BARNETILSYN_1) },
            { assertThat(barnetilsynListe.grunnlagListe[1].barnPersonId).isEqualTo(TestUtil.BARN_PERSON_ID_BARNETILSYN_2) },
            { assertThat(barnetilsynListe.feilrapporteringListe).isEmpty() },
        )
    }

    @Test
    fun `Skal returnere feil og tomt grunnlag fra barnetilsyn når consumer-response er FAILURE`() {
        Mockito.`when`(barnetilsynConsumerMock.hentBarnetilsyn(any()))
            .thenReturn(
                RestResponse.Failure(
                    message = "Ikke funnet",
                    statusCode = HttpStatus.NOT_FOUND,
                    restClientException = HttpClientErrorException(HttpStatus.NOT_FOUND),
                ),
            )

        val barnetilsynRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val barnetilsynListe = hentBarnetilsynService.hentBarnetilsyn(
            barnetilsynRequestListe = barnetilsynRequestListe,
        )

        Mockito.verify(barnetilsynConsumerMock, Mockito.times(1)).hentBarnetilsyn(any())

        assertAll(
            { assertThat(barnetilsynListe).isNotNull() },
            { assertThat(barnetilsynListe.grunnlagListe).isEmpty() },
            { assertThat(barnetilsynListe.feilrapporteringListe).isNotEmpty() },
            { assertThat(barnetilsynListe.feilrapporteringListe).hasSize(1) },
            { assertThat(barnetilsynListe.feilrapporteringListe[0].grunnlagstype).isEqualTo(GrunnlagRequestType.BARNETILSYN) },
            { assertThat(barnetilsynListe.feilrapporteringListe[0].personId).isEqualTo(barnetilsynRequestListe[0].personId) },
            { assertThat(barnetilsynListe.feilrapporteringListe[0].periodeFra).isEqualTo(barnetilsynRequestListe[0].periodeFra) },
            { assertThat(barnetilsynListe.feilrapporteringListe[0].periodeTil).isNull() },
            { assertThat(barnetilsynListe.feilrapporteringListe[0].feiltype).isEqualTo(HentGrunnlagFeiltype.FUNKSJONELL_FEIL) },
            { assertThat(barnetilsynListe.feilrapporteringListe[0].feilmelding).isEqualTo("Ikke funnet") },
        )
    }
}
