package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.grunnlag.HentGrunnlagFeiltype
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.ArbeidsforholdConsumer
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.EnhetsregisterConsumer
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api.HentEnhetsregisterResponse
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
class HentArbeidsforholdServiceMockTest {

    @InjectMocks
    private lateinit var hentArbeidsforholdService: HentArbeidsforholdService

    @Mock
    private lateinit var arbeidsforholdConsumerMock: ArbeidsforholdConsumer

    @Mock
    private lateinit var enhetsregisterConsumerMock: EnhetsregisterConsumer

    @Test
    fun `Skal returnere grunnlag og ikke feil når consumer-response er SUCCESS`() {
        Mockito.`when`(arbeidsforholdConsumerMock.hentArbeidsforhold(any())).thenReturn(RestResponse.Success(TestUtil.byggArbeidsforholdResponse()))
        Mockito.`when`(enhetsregisterConsumerMock.hentEnhetsinfo(any())).thenReturn(RestResponse.Success(HentEnhetsregisterResponse()))

        val arbeidsforholdRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val arbeidsforholdListe = hentArbeidsforholdService.hentArbeidsforhold(
            arbeidsforholdRequestListe = arbeidsforholdRequestListe,
        )

        Mockito.verify(arbeidsforholdConsumerMock, Mockito.times(1)).hentArbeidsforhold(any())

        assertAll(
            { assertThat(arbeidsforholdListe).isNotNull() },
            { assertThat(arbeidsforholdListe.grunnlagListe).isNotEmpty() },
            { assertThat(arbeidsforholdListe.grunnlagListe).hasSize(2) },
        )
    }

    @Test
    fun `Skal returnere feil og tomt grunnlag fra arbeidsforhold når consumer-response er FAILURE`() {
        Mockito.`when`(arbeidsforholdConsumerMock.hentArbeidsforhold(any()))
            .thenReturn(
                RestResponse.Failure(
                    message = "Ikke funnet",
                    statusCode = HttpStatus.NOT_FOUND,
                    restClientException = HttpClientErrorException(HttpStatus.NOT_FOUND),
                ),
            )

        val arbeidsforholdRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val arbeidsforholdListe = hentArbeidsforholdService.hentArbeidsforhold(
            arbeidsforholdRequestListe = arbeidsforholdRequestListe,
        )

        Mockito.verify(arbeidsforholdConsumerMock, Mockito.times(1)).hentArbeidsforhold(any())

        assertAll(
            { assertThat(arbeidsforholdListe).isNotNull() },
            { assertThat(arbeidsforholdListe.grunnlagListe).isEmpty() },
            { assertThat(arbeidsforholdListe.feilrapporteringListe).isNotEmpty() },
            { assertThat(arbeidsforholdListe.feilrapporteringListe).hasSize(1) },
            { assertThat(arbeidsforholdListe.feilrapporteringListe[0].grunnlagstype).isEqualTo(GrunnlagRequestType.ARBEIDSFORHOLD) },
            { assertThat(arbeidsforholdListe.feilrapporteringListe[0].personId).isEqualTo(arbeidsforholdRequestListe[0].personId) },
            { assertThat(arbeidsforholdListe.feilrapporteringListe[0].periodeFra).isNull() },
            { assertThat(arbeidsforholdListe.feilrapporteringListe[0].periodeTil).isNull() },
            { assertThat(arbeidsforholdListe.feilrapporteringListe[0].feiltype).isEqualTo(HentGrunnlagFeiltype.FUNKSJONELL_FEIL) },
            { assertThat(arbeidsforholdListe.feilrapporteringListe[0].feilmelding).isEqualTo("Ikke funnet") },
        )
    }

    @Test
    fun `Skal returnere feil og grunnlag når consumer-response fra arbeidsforhold er SUCCESS og consumer-response fra enhetsregister er FAILURE`() {
        Mockito.`when`(arbeidsforholdConsumerMock.hentArbeidsforhold(any())).thenReturn(RestResponse.Success(TestUtil.byggArbeidsforholdResponse()))
        Mockito.`when`(enhetsregisterConsumerMock.hentEnhetsinfo(any()))
            .thenReturn(
                RestResponse.Failure(
                    message = "Ikke funnet",
                    statusCode = HttpStatus.NOT_FOUND,
                    restClientException = HttpClientErrorException(HttpStatus.NOT_FOUND),
                ),
            )

        val arbeidsforholdRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val arbeidsforholdListe = hentArbeidsforholdService.hentArbeidsforhold(
            arbeidsforholdRequestListe = arbeidsforholdRequestListe,
        )

        Mockito.verify(arbeidsforholdConsumerMock, Mockito.times(1)).hentArbeidsforhold(any())
        Mockito.verify(enhetsregisterConsumerMock, Mockito.times(1)).hentEnhetsinfo(any())

        assertAll(
            { assertThat(arbeidsforholdListe).isNotNull() },
            { assertThat(arbeidsforholdListe.grunnlagListe).isNotEmpty() },
            { assertThat(arbeidsforholdListe.grunnlagListe).hasSize(2) },
            { assertThat(arbeidsforholdListe.feilrapporteringListe).isNotEmpty() },
            { assertThat(arbeidsforholdListe.feilrapporteringListe).hasSize(1) },
            { assertThat(arbeidsforholdListe.feilrapporteringListe[0].grunnlagstype).isEqualTo(GrunnlagRequestType.ARBEIDSFORHOLD) },
            { assertThat(arbeidsforholdListe.feilrapporteringListe[0].personId).isEqualTo(arbeidsforholdRequestListe[0].personId) },
            { assertThat(arbeidsforholdListe.feilrapporteringListe[0].periodeFra).isNull() },
            { assertThat(arbeidsforholdListe.feilrapporteringListe[0].periodeTil).isNull() },
            { assertThat(arbeidsforholdListe.feilrapporteringListe[0].feiltype).isEqualTo(HentGrunnlagFeiltype.FUNKSJONELL_FEIL) },
            { assertThat(arbeidsforholdListe.feilrapporteringListe[0].feilmelding).isEqualTo("Ikke funnet") },
        )
    }
}
