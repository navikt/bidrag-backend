package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.grunnlag.HentGrunnlagFeiltype
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.tilleggsstønad.TilleggsstønadConsumer
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
class HentTilleggsstønadServiceMockTest {

    @InjectMocks
    private lateinit var hentTilleggsstønadService: HentTilleggsstønadService

    @Mock
    private lateinit var tilleggsstønadConsumerMock: TilleggsstønadConsumer

    @Test
    fun `Skal returnere grunnlag og ikke feil når consumer-response er SUCCESS`() {
        Mockito.`when`(tilleggsstønadConsumerMock.hentTilleggsstønad(any())).thenReturn(RestResponse.Success(TestUtil.byggTilleggsstønadResponse()))

        val tilleggsstønadRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val tilleggsstønadListe = hentTilleggsstønadService.hentTilleggsstønad(
            tilleggsstønadRequestListe = tilleggsstønadRequestListe,
        )

        Mockito.verify(tilleggsstønadConsumerMock, Mockito.times(1)).hentTilleggsstønad(any())

        assertAll(
            { assertThat(tilleggsstønadListe).isNotNull() },
            { assertThat(tilleggsstønadListe.grunnlagListe).isNotEmpty() },
            { assertThat(tilleggsstønadListe.grunnlagListe).hasSize(1) },
            { assertThat(tilleggsstønadListe.grunnlagListe[0].partPersonId).isEqualTo("personident") },
            { assertThat(tilleggsstønadListe.grunnlagListe[0].harInnvilgetVedtak).isTrue() },
            { assertThat(tilleggsstønadListe.feilrapporteringListe).isEmpty() },
        )
    }

    @Test
    fun `Skal returnere feil og tomt grunnlag fra barnetilsyn når consumer-response er FAILURE`() {
        Mockito.`when`(tilleggsstønadConsumerMock.hentTilleggsstønad(any()))
            .thenReturn(
                RestResponse.Failure(
                    message = "Ikke funnet",
                    statusCode = HttpStatus.NOT_FOUND,
                    restClientException = HttpClientErrorException(HttpStatus.NOT_FOUND),
                ),
            )

        val tilleggsstønadRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val tilleggsstønadListe = hentTilleggsstønadService.hentTilleggsstønad(
            tilleggsstønadRequestListe = tilleggsstønadRequestListe,
        )

        Mockito.verify(tilleggsstønadConsumerMock, Mockito.times(1)).hentTilleggsstønad(any())

        assertAll(
            { assertThat(tilleggsstønadListe).isNotNull() },
            { assertThat(tilleggsstønadListe.grunnlagListe).isEmpty() },
            { assertThat(tilleggsstønadListe.feilrapporteringListe).isNotEmpty() },
            { assertThat(tilleggsstønadListe.feilrapporteringListe).hasSize(1) },
            { assertThat(tilleggsstønadListe.feilrapporteringListe[0].grunnlagstype).isEqualTo(GrunnlagRequestType.TILLEGGSSTØNAD) },
            { assertThat(tilleggsstønadListe.feilrapporteringListe[0].personId).isEqualTo(tilleggsstønadRequestListe[0].personId) },
            { assertThat(tilleggsstønadListe.feilrapporteringListe[0].periodeTil).isNull() },
            { assertThat(tilleggsstønadListe.feilrapporteringListe[0].feiltype).isEqualTo(HentGrunnlagFeiltype.FUNKSJONELL_FEIL) },
            { assertThat(tilleggsstønadListe.feilrapporteringListe[0].feilmelding).isEqualTo("Ikke funnet") },
        )
    }
}
