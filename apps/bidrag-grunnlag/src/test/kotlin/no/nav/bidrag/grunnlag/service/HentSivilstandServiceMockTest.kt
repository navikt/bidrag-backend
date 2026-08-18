package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.grunnlag.HentGrunnlagFeiltype
import no.nav.bidrag.domene.enums.person.SivilstandskodePDL
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
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

@ExtendWith(MockitoExtension::class)
class HentSivilstandServiceMockTest {

    @InjectMocks
    private lateinit var hentSivilstandService: HentSivilstandService

    @Mock
    private lateinit var personConsumerMock: BidragPersonConsumer

    @Test
    fun `Skal returnere grunnlag og ikke feil når consumer-response er SUCCESS`() {
        Mockito.`when`(personConsumerMock.hentSivilstand(any())).thenReturn(RestResponse.Success(TestUtil.byggHentSivilstandResponse()))

        val sivilstandRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val sivilstandListe = hentSivilstandService.hentSivilstand(
            sivilstandRequestListe = sivilstandRequestListe,
        )

        Mockito.verify(personConsumerMock, Mockito.times(1)).hentSivilstand(any())

        assertAll(
            { assertThat(sivilstandListe).isNotNull() },
            { assertThat(sivilstandListe.grunnlagListe).isNotEmpty() },
            { assertThat(sivilstandListe.grunnlagListe).hasSize(3) },
            { assertThat(sivilstandListe.grunnlagListe[0].type).isEqualTo(SivilstandskodePDL.SEPARERT_PARTNER) },
            { assertThat(sivilstandListe.grunnlagListe[1].type).isEqualTo(SivilstandskodePDL.ENKE_ELLER_ENKEMANN) },
            { assertThat(sivilstandListe.grunnlagListe[2].type).isEqualTo(SivilstandskodePDL.GJENLEVENDE_PARTNER) },
            { assertThat(sivilstandListe.feilrapporteringListe).isEmpty() },
        )
    }

    @Test
    fun `Skal returnere feil og tomt grunnlag fra sivilstand når consumer-response er FAILURE`() {
        Mockito.`when`(personConsumerMock.hentSivilstand(any()))
            .thenReturn(RestResponse.Failure("Ikke funnet", HttpStatus.NOT_FOUND, HttpClientErrorException(HttpStatus.NOT_FOUND)))

        val sivilstandRequestListe = listOf(TestUtil.byggPersonIdOgPeriodeRequest())

        val sivilstandListe = hentSivilstandService.hentSivilstand(
            sivilstandRequestListe = sivilstandRequestListe,
        )

        Mockito.verify(personConsumerMock, Mockito.times(1)).hentSivilstand(any())

        assertAll(
            { assertThat(sivilstandListe).isNotNull() },
            { assertThat(sivilstandListe.grunnlagListe).isEmpty() },
            { assertThat(sivilstandListe.feilrapporteringListe).isNotEmpty() },
            { assertThat(sivilstandListe.feilrapporteringListe).hasSize(1) },
            { assertThat(sivilstandListe.feilrapporteringListe[0].grunnlagstype).isEqualTo(GrunnlagRequestType.SIVILSTAND) },
            { assertThat(sivilstandListe.feilrapporteringListe[0].personId).isEqualTo(sivilstandRequestListe[0].personId) },
            { assertThat(sivilstandListe.feilrapporteringListe[0].periodeFra).isNull() },
            { assertThat(sivilstandListe.feilrapporteringListe[0].periodeTil).isNull() },
            { assertThat(sivilstandListe.feilrapporteringListe[0].feiltype).isEqualTo(HentGrunnlagFeiltype.FUNKSJONELL_FEIL) },
            { assertThat(sivilstandListe.feilrapporteringListe[0].feilmelding).isEqualTo("Ikke funnet") },
        )
    }
}
