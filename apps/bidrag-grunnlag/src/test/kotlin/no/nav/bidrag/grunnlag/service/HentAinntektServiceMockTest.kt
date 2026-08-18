package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.grunnlag.HentGrunnlagFeiltype
import no.nav.bidrag.domene.enums.vedtak.Formål
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.HentInntektListeResponseIntern
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
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class HentAinntektServiceMockTest {

    @InjectMocks
    private lateinit var hentAinntektService: HentAinntektService

    @Mock
    private lateinit var inntektskomponentenServiceMock: InntektskomponentenService

    @Test
    fun `Skal returnere grunnlag og ikke feil når InntektskomponentenService-respons er OK`() {
        Mockito.`when`(inntektskomponentenServiceMock.hentInntekt(any())).thenReturn(TestUtil.byggHentInntektListeResponseIntern())

        val ainntektRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val ainntektListe = hentAinntektService.hentAinntekt(
            ainntektRequestListe = ainntektRequestListe,
            formål = Formål.BIDRAG,
        )

        Mockito.verify(inntektskomponentenServiceMock, Mockito.times(1)).hentInntekt(any())

        assertAll(
            { assertThat(ainntektListe).isNotNull() },
            { assertThat(ainntektListe.grunnlagListe).isNotEmpty() },
            { assertThat(ainntektListe.grunnlagListe).hasSize(1) },
            { assertThat(ainntektListe.grunnlagListe[0].ainntektspostListe).isNotEmpty() },
            { assertThat(ainntektListe.grunnlagListe[0].ainntektspostListe).hasSize(1) },
            { assertThat(ainntektListe.feilrapporteringListe).isEmpty() },
        )
    }

    @Test
    fun `Skal returnere tomt grunnlag når person-id er bnr eller npid`() {
        val ainntektRequestListe = listOf(
            PersonIdOgPeriodeRequest(
                personId = TestUtil.genererBnrEllerNpid(),
                periodeFra = LocalDate.parse("2023-01-01"),
                periodeTil = LocalDate.parse("2024-01-01"),
            ),
        )

        val ainntektListe = hentAinntektService.hentAinntekt(
            ainntektRequestListe = ainntektRequestListe,
            formål = Formål.BIDRAG,
        )

        Mockito.verify(inntektskomponentenServiceMock, Mockito.times(0)).hentInntekt(any())

        assertAll(
            { assertThat(ainntektListe).isNotNull() },
            { assertThat(ainntektListe.grunnlagListe).isEmpty() },
            { assertThat(ainntektListe.feilrapporteringListe).isEmpty() },
        )
    }

    @Test
    fun `Skal returnere feil og tomt grunnlag fra ainntekt når InntektskomponentenService-respons ikke er OK`() {
        Mockito.`when`(inntektskomponentenServiceMock.hentInntekt(any())).thenReturn(
            HentInntektListeResponseIntern(
                httpStatus = HttpStatus.NOT_FOUND,
                melding = "Ikke funnet",
                arbeidsInntektMaanedIntern = emptyList(),
                false,
            ),
        )

        val ainntektRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val ainntektListe = hentAinntektService.hentAinntekt(
            ainntektRequestListe = ainntektRequestListe,
            formål = Formål.BIDRAG,
        )

        Mockito.verify(inntektskomponentenServiceMock, Mockito.times(1)).hentInntekt(any())

        assertAll(
            { assertThat(ainntektListe).isNotNull() },
            { assertThat(ainntektListe.grunnlagListe).isEmpty() },
            { assertThat(ainntektListe.feilrapporteringListe).isNotEmpty() },
            { assertThat(ainntektListe.feilrapporteringListe).hasSize(1) },
            { assertThat(ainntektListe.feilrapporteringListe[0].grunnlagstype).isEqualTo(GrunnlagRequestType.AINNTEKT) },
            { assertThat(ainntektListe.feilrapporteringListe[0].personId).isEqualTo(ainntektRequestListe[0].personId) },
            { assertThat(ainntektListe.feilrapporteringListe[0].periodeFra).isEqualTo(ainntektRequestListe[0].periodeFra) },
            { assertThat(ainntektListe.feilrapporteringListe[0].periodeTil).isEqualTo(ainntektRequestListe[0].periodeTil) },
            { assertThat(ainntektListe.feilrapporteringListe[0].feiltype).isEqualTo(HentGrunnlagFeiltype.FUNKSJONELL_FEIL) },
            { assertThat(ainntektListe.feilrapporteringListe[0].feilmelding).isEqualTo("Ikke funnet") },
        )
    }
}
