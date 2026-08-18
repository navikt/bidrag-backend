package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.grunnlag.HentGrunnlagFeiltype
import no.nav.bidrag.domene.enums.person.Familierelasjon
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.bidragperson.BidragPersonConsumer
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.any
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class HentRelatertePersonerServiceMockTest {

    @InjectMocks
    private lateinit var hentRelatertePersonerService: HentRelatertePersonerService

    @Mock
    private lateinit var bidragPersonConsumerMock: BidragPersonConsumer

    @Test
    fun `Skal returnere grunnlag og ikke feil når consumer-response er SUCCESS`() {
        `when`(bidragPersonConsumerMock.hentHusstandsmedlemmer(any()))
            .thenReturn(RestResponse.Success(TestUtil.byggHentHusstandsmedlemmerResponse()))
        `when`(bidragPersonConsumerMock.hentForelderBarnRelasjon(any()))
            .thenReturn(RestResponse.Success(TestUtil.byggHentForelderBarnRelasjonerResponse()))
        `when`(bidragPersonConsumerMock.hentNavnFødselOgDød(any()))
            .thenReturn(RestResponse.Success(TestUtil.byggHentNavnFoedselOgDoedResponse()))
        `when`(bidragPersonConsumerMock.hentSivilstand(any()))
            .thenReturn(RestResponse.Success(TestUtil.byggHentSivilstandResponse()))

        val relatertPersonRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val relatertPersonListe = hentRelatertePersonerService.hentRelatertePersoner(relatertPersonRequestListe = relatertPersonRequestListe)

        verify(bidragPersonConsumerMock, Mockito.times(1)).hentHusstandsmedlemmer(any())
        verify(bidragPersonConsumerMock, Mockito.times(4)).hentForelderBarnRelasjon(any())
        verify(bidragPersonConsumerMock, Mockito.times(3)).hentNavnFødselOgDød(any())

        assertAll(
            { assertThat(relatertPersonListe).isNotNull() },
            { assertThat(relatertPersonListe.grunnlagListe).isNotEmpty() },
            { assertThat(relatertPersonListe.grunnlagListe).hasSize(4) },
            { assertThat(relatertPersonListe.grunnlagListe[0].gjelderPersonId).isEqualTo("111") },
            { assertThat(relatertPersonListe.grunnlagListe[1].gjelderPersonId).isEqualTo("555") },
            { assertThat(relatertPersonListe.grunnlagListe[2].gjelderPersonId).isEqualTo("222") },
            { assertThat(relatertPersonListe.grunnlagListe[3].gjelderPersonId).isEqualTo("333") },
            { assertThat(relatertPersonListe.grunnlagListe[0].borISammeHusstandDtoListe).isNotEmpty },
            { assertThat(relatertPersonListe.grunnlagListe[1].borISammeHusstandDtoListe).isNotEmpty },
            { assertThat(relatertPersonListe.grunnlagListe[2].borISammeHusstandDtoListe).isEmpty() },
            { assertThat(relatertPersonListe.grunnlagListe[3].borISammeHusstandDtoListe).isEmpty() },
            { assertThat(relatertPersonListe.feilrapporteringListe).isEmpty() },
        )
    }

    @Test
    fun `Skal returnere feil og tomt grunnlag fra relatertePersoner når consumer-response er FAILURE`() {
        `when`(bidragPersonConsumerMock.hentHusstandsmedlemmer(any())).thenReturn(
            RestResponse.Failure(
                message = "Ikke funnet",
                statusCode = HttpStatus.NOT_FOUND,
                restClientException = HttpClientErrorException(HttpStatus.NOT_FOUND),
            ),
        )

        `when`(bidragPersonConsumerMock.hentForelderBarnRelasjon(any())).thenReturn(
            RestResponse.Failure(
                message = "Ikke funnet",
                statusCode = HttpStatus.NOT_FOUND,
                restClientException = HttpClientErrorException(HttpStatus.NOT_FOUND),
            ),
        )

        `when`(bidragPersonConsumerMock.hentSivilstand(any())).thenReturn(
            RestResponse.Failure(
                message = "Ikke funnet",
                statusCode = HttpStatus.NOT_FOUND,
                restClientException = HttpClientErrorException(HttpStatus.NOT_FOUND),
            ),
        )

        val relatertPersonRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val relatertPersonListe = hentRelatertePersonerService.hentRelatertePersoner(
            relatertPersonRequestListe = relatertPersonRequestListe,
        )

        verify(bidragPersonConsumerMock, Mockito.times(1)).hentHusstandsmedlemmer(any())

        assertAll(
            { assertThat(relatertPersonListe).isNotNull() },
            { assertThat(relatertPersonListe.grunnlagListe).isEmpty() },
            { assertThat(relatertPersonListe.feilrapporteringListe).isNotEmpty() },
            { assertThat(relatertPersonListe.feilrapporteringListe).hasSize(2) },
            { assertThat(relatertPersonListe.feilrapporteringListe[0].grunnlagstype).isEqualTo(GrunnlagRequestType.HUSSTANDSMEDLEMMER_OG_EGNE_BARN) },
            { assertThat(relatertPersonListe.feilrapporteringListe[0].personId).isEqualTo(relatertPersonRequestListe[0].personId) },
            { assertThat(relatertPersonListe.feilrapporteringListe[0].periodeFra).isNull() },
            { assertThat(relatertPersonListe.feilrapporteringListe[0].periodeTil).isNull() },
            { assertThat(relatertPersonListe.feilrapporteringListe[0].feiltype).isEqualTo(HentGrunnlagFeiltype.FUNKSJONELL_FEIL) },
            { assertThat(relatertPersonListe.feilrapporteringListe[0].feilmelding).isEqualTo("Ikke funnet") },
            { assertThat(relatertPersonListe.feilrapporteringListe[1].grunnlagstype).isEqualTo(GrunnlagRequestType.HUSSTANDSMEDLEMMER_OG_EGNE_BARN) },
            { assertThat(relatertPersonListe.feilrapporteringListe[1].personId).isEqualTo(relatertPersonRequestListe[0].personId) },
            { assertThat(relatertPersonListe.feilrapporteringListe[1].periodeFra).isNull() },
            { assertThat(relatertPersonListe.feilrapporteringListe[1].periodeTil).isNull() },
            { assertThat(relatertPersonListe.feilrapporteringListe[1].feiltype).isEqualTo(HentGrunnlagFeiltype.FUNKSJONELL_FEIL) },
            { assertThat(relatertPersonListe.feilrapporteringListe[1].feilmelding).isEqualTo("Ikke funnet") },
        )
    }

    @Test
    fun `Test beregning av borISammeHusstandDtoListe`() {
        val relatertPersonRequestListe = listOf(
            PersonIdOgPeriodeRequest(
                personId = "personident",
                periodeFra = LocalDate.parse("2021-03-21"),
                periodeTil = LocalDate.parse("2024-03-21"),
            ),
        )

        `when`(bidragPersonConsumerMock.hentHusstandsmedlemmer(any()))
            .thenReturn(RestResponse.Success(TestUtil.byggHentToHusstandsmedlemmer()))
        `when`(bidragPersonConsumerMock.hentForelderBarnRelasjon(any()))
            .thenReturn(RestResponse.Success(TestUtil.byggHentForelderBarnRelasjonerResponse()))
        `when`(bidragPersonConsumerMock.hentNavnFødselOgDød(any()))
            .thenReturn(RestResponse.Success(TestUtil.byggHentNavnFoedselOgDoedResponse()))
        `when`(bidragPersonConsumerMock.hentSivilstand(any()))
            .thenReturn(RestResponse.Success(TestUtil.byggHentSivilstandResponse()))

        val relatertPersonListe = hentRelatertePersonerService.hentRelatertePersoner(relatertPersonRequestListe)

        assertAll(
            { assertThat(relatertPersonListe).isNotNull() },
            { assertThat(relatertPersonListe.grunnlagListe[0].gjelderPersonId).isEqualTo("111") },
            { assertThat(relatertPersonListe.grunnlagListe[0].borISammeHusstandDtoListe).isNotEmpty },

        )
    }

    @Test
    fun `Test beregning av relaterte personer`() {
        val relatertPersonRequestListe = listOf(
            PersonIdOgPeriodeRequest(
                personId = "123456",
                periodeFra = LocalDate.parse("2021-03-21"),
                periodeTil = LocalDate.parse("2024-03-21"),
            ),
        )

        `when`(bidragPersonConsumerMock.hentHusstandsmedlemmer(any()))
            .thenReturn(RestResponse.Success(TestUtil.byggHentHusstandsmedlemmerSærbidrag()))
        `when`(bidragPersonConsumerMock.hentForelderBarnRelasjon(any()))
            .thenReturn(RestResponse.Success(TestUtil.byggHentForelderBarnRelasjonerResponse()))
        `when`(bidragPersonConsumerMock.hentForelderBarnRelasjon(Personident("111")))
            .thenReturn(RestResponse.Success(TestUtil.byggHentForelderBarnRelasjonerForBarn()))
        `when`(bidragPersonConsumerMock.hentForelderBarnRelasjon(Personident("222")))
            .thenReturn(RestResponse.Success(TestUtil.byggHentForelderBarnRelasjonerForBarn()))
        `when`(bidragPersonConsumerMock.hentForelderBarnRelasjon(Personident("333")))
            .thenReturn(RestResponse.Success(TestUtil.byggHentForelderBarnRelasjonerForBarn()))
        `when`(bidragPersonConsumerMock.hentNavnFødselOgDød(any()))
            .thenReturn(RestResponse.Success(TestUtil.byggHentNavnFoedselOgDoedResponse()))
        `when`(bidragPersonConsumerMock.hentSivilstand(any()))
            .thenReturn(RestResponse.Success(TestUtil.byggHentSivilstandMedRelatertVedSivilstand()))

        val relatertPersonListe =
            hentRelatertePersonerService.hentRelatertePersoner(relatertPersonRequestListe).grunnlagListe.sortedWith(compareBy { it.gjelderPersonId })

        assertAll(
            { assertThat(relatertPersonListe).isNotNull() },
            { assertThat(relatertPersonListe[0].gjelderPersonId).isEqualTo("111") },
            { assertThat(relatertPersonListe[0].relasjon).isEqualTo(Familierelasjon.BARN) },
            { assertThat(relatertPersonListe[0].borISammeHusstandDtoListe).isNotEmpty },

            { assertThat(relatertPersonListe[1].gjelderPersonId).isEqualTo("222") },
            { assertThat(relatertPersonListe[1].relasjon).isEqualTo(Familierelasjon.BARN) },
            { assertThat(relatertPersonListe[1].borISammeHusstandDtoListe).isEmpty() },

            { assertThat(relatertPersonListe[2].gjelderPersonId).isEqualTo("333") },
            { assertThat(relatertPersonListe[2].relasjon).isEqualTo(Familierelasjon.BARN) },
            { assertThat(relatertPersonListe[2].borISammeHusstandDtoListe).isEmpty() },

            { assertThat(relatertPersonListe[3].gjelderPersonId).isEqualTo("666") },
            { assertThat(relatertPersonListe[3].relasjon).isEqualTo(Familierelasjon.INGEN) },
            { assertThat(relatertPersonListe[3].borISammeHusstandDtoListe).isNotEmpty },

            { assertThat(relatertPersonListe[4].gjelderPersonId).isEqualTo("777") },
            { assertThat(relatertPersonListe[4].relasjon).isEqualTo(Familierelasjon.EKTEFELLE) },
            { assertThat(relatertPersonListe[4].borISammeHusstandDtoListe).isNotEmpty },

            { assertThat(relatertPersonListe[5].gjelderPersonId).isEqualTo("888") },
            { assertThat(relatertPersonListe[5].relasjon).isEqualTo(Familierelasjon.MOTPART_TIL_FELLES_BARN) },
            { assertThat(relatertPersonListe[5].borISammeHusstandDtoListe).isNotEmpty },

            { assertThat(relatertPersonListe[6].gjelderPersonId).isEqualTo("999") },
            { assertThat(relatertPersonListe[6].relasjon).isEqualTo(Familierelasjon.MOR) },
            { assertThat(relatertPersonListe[6].borISammeHusstandDtoListe).isNotEmpty },

        )
    }
}
