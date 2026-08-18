package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.grunnlag.HentGrunnlagFeiltype
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.familiekssak.FamilieKsSakConsumer
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
class HentKontantstøtteServiceMockTest {

    @InjectMocks
    private lateinit var hentKontantstøtteService: HentKontantstøtteService

    @Mock
    private lateinit var kontantstøtteConsumerMock: FamilieKsSakConsumer

    @Test
    fun `Skal returnere grunnlag og ikke feil når consumer-response er SUCCESS`() {
        Mockito.`when`(kontantstøtteConsumerMock.hentKontantstøtte(any())).thenReturn(RestResponse.Success(TestUtil.byggKontantstøtteResponse()))

        val kontantstøtteRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val kontantstøtteListe = hentKontantstøtteService.hentKontantstøtte(
            kontantstøtteRequestListe = kontantstøtteRequestListe,
            historiskeIdenterMap = emptyMap(),
        )

        Mockito.verify(kontantstøtteConsumerMock, Mockito.times(1)).hentKontantstøtte(any())

        assertAll(
            { assertThat(kontantstøtteListe).isNotNull() },
            { assertThat(kontantstøtteListe.grunnlagListe).isNotEmpty() },
            { assertThat(kontantstøtteListe.grunnlagListe).hasSize(3) },
            { assertThat(kontantstøtteListe.grunnlagListe[0].barnPersonId).isEqualTo(TestUtil.BARN_PERSON_ID_KONTANTSTOTTE) },
            { assertThat(kontantstøtteListe.grunnlagListe[1].barnPersonId).isEqualTo("15544332211") },
            { assertThat(kontantstøtteListe.grunnlagListe[2].barnPersonId).isEqualTo(TestUtil.BARN_PERSON_ID_KONTANTSTOTTE) },
            { assertThat(kontantstøtteListe.feilrapporteringListe).isEmpty() },
        )
    }

    @Test
    fun `Skal returnere feil og tomt grunnlag fra kontantstøtte når consumer-response er FAILURE`() {
        Mockito.`when`(kontantstøtteConsumerMock.hentKontantstøtte(any())).thenReturn(
            RestResponse.Failure(
                message = "Ikke funnet",
                statusCode = HttpStatus.NOT_FOUND,
                restClientException = HttpClientErrorException(HttpStatus.NOT_FOUND),
            ),
        )

        val kontantstøtteRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val kontantstøtteListe = hentKontantstøtteService.hentKontantstøtte(
            kontantstøtteRequestListe = kontantstøtteRequestListe,
            historiskeIdenterMap = emptyMap(),
        )

        Mockito.verify(kontantstøtteConsumerMock, Mockito.times(1)).hentKontantstøtte(any())

        assertAll(
            { assertThat(kontantstøtteListe).isNotNull() },
            { assertThat(kontantstøtteListe.grunnlagListe).isEmpty() },
            { assertThat(kontantstøtteListe.feilrapporteringListe).isNotEmpty() },
            { assertThat(kontantstøtteListe.feilrapporteringListe).hasSize(1) },
            { assertThat(kontantstøtteListe.feilrapporteringListe[0].grunnlagstype).isEqualTo(GrunnlagRequestType.KONTANTSTØTTE) },
            { assertThat(kontantstøtteListe.feilrapporteringListe[0].personId).isEqualTo(kontantstøtteRequestListe[0].personId) },
            { assertThat(kontantstøtteListe.feilrapporteringListe[0].periodeFra).isEqualTo(kontantstøtteRequestListe[0].periodeFra) },
            { assertThat(kontantstøtteListe.feilrapporteringListe[0].periodeTil).isNull() },
            { assertThat(kontantstøtteListe.feilrapporteringListe[0].feiltype).isEqualTo(HentGrunnlagFeiltype.FUNKSJONELL_FEIL) },
            { assertThat(kontantstøtteListe.feilrapporteringListe[0].feilmelding).isEqualTo("Ikke funnet") },
        )
    }
}
