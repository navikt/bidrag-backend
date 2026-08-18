package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.grunnlag.HentGrunnlagFeiltype
import no.nav.bidrag.domene.enums.inntekt.Skattegrunnlagstype
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.skattegrunnlag.SigrunConsumer
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
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class HentSkattegrunnlagServiceMockTest {

    @InjectMocks
    private lateinit var hentSkattegrunnlagService: HentSkattegrunnlagService

    @Mock
    private lateinit var sigrunConsumerMock: SigrunConsumer

    @Test
    fun `Skal returnere grunnlag og ikke feil når consumer-response er SUCCESS`() {
        Mockito.`when`(sigrunConsumerMock.hentSummertSkattegrunnlag(any()))
            .thenReturn(RestResponse.Success(TestUtil.byggHentSkattegrunnlagResponse()))

        val skattegrunnlagRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val skattegrunnlagListe = hentSkattegrunnlagService.hentSkattegrunnlag(
            skattegrunnlagRequestListe = skattegrunnlagRequestListe,
        )

        Mockito.verify(sigrunConsumerMock, Mockito.times(1)).hentSummertSkattegrunnlag(any())

        assertAll(
            { assertThat(skattegrunnlagListe).isNotNull() },
            { assertThat(skattegrunnlagListe.grunnlagListe).isNotEmpty() },
            { assertThat(skattegrunnlagListe.grunnlagListe).hasSize(1) },
            { assertThat(skattegrunnlagListe.grunnlagListe[0].skattegrunnlagspostListe).hasSize(2) },
            {
                assertThat(skattegrunnlagListe.grunnlagListe[0].skattegrunnlagspostListe[0].skattegrunnlagType)
                    .isEqualTo(Skattegrunnlagstype.ORDINÆR.toString())
            },
            {
                assertThat(skattegrunnlagListe.grunnlagListe[0].skattegrunnlagspostListe[1].skattegrunnlagType)
                    .isEqualTo(Skattegrunnlagstype.SVALBARD.toString())
            },
            { assertThat(skattegrunnlagListe.feilrapporteringListe).isEmpty() },
        )
    }

    @Test
    fun `Skal returnere tomt grunnlag når person-id er bnr eller npid`() {
        val skattegrunnlagRequestListe = listOf(
            PersonIdOgPeriodeRequest(
                personId = TestUtil.genererBnrEllerNpid(),
                periodeFra = LocalDate.parse("2023-01-01"),
                periodeTil = LocalDate.parse("2024-01-01"),
            ),
        )

        val skattegrunnlagListe = hentSkattegrunnlagService.hentSkattegrunnlag(
            skattegrunnlagRequestListe = skattegrunnlagRequestListe,
        )

        Mockito.verify(sigrunConsumerMock, Mockito.times(0)).hentSummertSkattegrunnlag(any())

        assertAll(
            { assertThat(skattegrunnlagListe).isNotNull() },
            { assertThat(skattegrunnlagListe.grunnlagListe).isEmpty() },
            { assertThat(skattegrunnlagListe.feilrapporteringListe).isEmpty() },
        )
    }

    @Test
    fun `Skal returnere feil og tomt grunnlag fra skattegrunnlag når consumer-response er FAILURE`() {
        Mockito.`when`(sigrunConsumerMock.hentSummertSkattegrunnlag(any())).thenReturn(
            RestResponse.Failure(
                message = "Ikke funnet",
                statusCode = HttpStatus.NOT_FOUND,
                restClientException = HttpClientErrorException(HttpStatus.NOT_FOUND),
            ),
        )

        val skattegrunnlagRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val skattegrunnlagListe = hentSkattegrunnlagService.hentSkattegrunnlag(
            skattegrunnlagRequestListe = skattegrunnlagRequestListe,
        )

        Mockito.verify(sigrunConsumerMock, Mockito.times(1)).hentSummertSkattegrunnlag(any())

        assertAll(
            { assertThat(skattegrunnlagListe).isNotNull() },
            { assertThat(skattegrunnlagListe.grunnlagListe).isEmpty() },
            { assertThat(skattegrunnlagListe.feilrapporteringListe).isNotEmpty() },
            { assertThat(skattegrunnlagListe.feilrapporteringListe).hasSize(1) },
            { assertThat(skattegrunnlagListe.feilrapporteringListe[0].grunnlagstype).isEqualTo(GrunnlagRequestType.SKATTEGRUNNLAG) },
            { assertThat(skattegrunnlagListe.feilrapporteringListe[0].personId).isEqualTo(skattegrunnlagRequestListe[0].personId) },
            { assertThat(skattegrunnlagListe.feilrapporteringListe[0].periodeFra).isEqualTo(skattegrunnlagRequestListe[0].periodeFra) },
            { assertThat(skattegrunnlagListe.feilrapporteringListe[0].periodeTil).isEqualTo(skattegrunnlagRequestListe[0].periodeTil) },
            { assertThat(skattegrunnlagListe.feilrapporteringListe[0].feiltype).isEqualTo(HentGrunnlagFeiltype.FUNKSJONELL_FEIL) },
            { assertThat(skattegrunnlagListe.feilrapporteringListe[0].feilmelding).isEqualTo("Ikke funnet") },
        )
    }
}
