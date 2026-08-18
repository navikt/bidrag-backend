package no.nav.bidrag.grunnlag.service

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.bidrag.domene.enums.person.BarnType
import no.nav.bidrag.domene.enums.person.SivilstandskodePDL
import no.nav.bidrag.domene.enums.vedtak.Formål
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggAinntekt
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggAinntektBo
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggAinntektspost
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggAinntektspostBo
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggBarnetillegg
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggBarnetilleggBo
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggBarnetilsyn
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggBarnetilsynBo
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggEgetBarnIHusstanden
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggGrunnlagspakke
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggKontantstotte
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggKontantstotteBo
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggNyGrunnlagspakkeRequest
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggRelatertPersonBo
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggSivilstand
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggSivilstandBo
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggSkattegrunnlagSkatt
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggSkattegrunnlagSkattBo
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggSkattegrunnlagspost
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggSkattegrunnlagspostBo
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggUtvidetBarnetrygdOgSmaabarnstillegg
import no.nav.bidrag.grunnlag.TestUtil.Companion.byggUtvidetBarnetrygdOgSmaabarnstilleggBo
import no.nav.bidrag.grunnlag.bo.AinntektBo
import no.nav.bidrag.grunnlag.bo.AinntektspostBo
import no.nav.bidrag.grunnlag.bo.BarnetilleggBo
import no.nav.bidrag.grunnlag.bo.BarnetilsynBo
import no.nav.bidrag.grunnlag.bo.KontantstotteBo
import no.nav.bidrag.grunnlag.bo.RelatertPersonBo
import no.nav.bidrag.grunnlag.bo.SivilstandBo
import no.nav.bidrag.grunnlag.bo.SkattegrunnlagBo
import no.nav.bidrag.grunnlag.bo.SkattegrunnlagspostBo
import no.nav.bidrag.grunnlag.bo.UtvidetBarnetrygdOgSmaabarnstilleggBo
import no.nav.bidrag.grunnlag.consumer.bidragperson.BidragPersonConsumer
import no.nav.bidrag.transport.behandling.grunnlag.request.OpprettGrunnlagspakkeRequestDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("GrunnlagspakkeServiceMockTest")
@ExtendWith(MockitoExtension::class)
class GrunnlagspakkeServiceMockTest {

    @InjectMocks
    private lateinit var grunnlagspakkeService: GrunnlagspakkeService

    @Mock
    private lateinit var persistenceServiceMock: PersistenceService

    @Mock
    private lateinit var bidragPersonConsumerMock: BidragPersonConsumer

    @Spy
    private var meterRegistry: MeterRegistry = SimpleMeterRegistry()

    @Mock
    private lateinit var oppdaterGrunnlagspakkeService: OppdaterGrunnlagspakkeService

    @Captor
    private lateinit var opprettGrunnlagspakkeRequestDtoCaptor: ArgumentCaptor<OpprettGrunnlagspakkeRequestDto>

    @Captor
    private lateinit var ainntektBoCaptor: ArgumentCaptor<AinntektBo>

    @Captor
    private lateinit var ainntektspostBoCaptor: ArgumentCaptor<AinntektspostBo>

    @Captor
    private lateinit var skattegrunnlagBoCaptor: ArgumentCaptor<SkattegrunnlagBo>

    @Captor
    private lateinit var skattegrunnlagspostBoCaptor: ArgumentCaptor<SkattegrunnlagspostBo>

    @Captor
    private lateinit var utvidetBarnetrygdOgSmaabarnstilleggBoCaptor: ArgumentCaptor<UtvidetBarnetrygdOgSmaabarnstilleggBo>

    @Captor
    private lateinit var barnetilleggBoCaptor: ArgumentCaptor<BarnetilleggBo>

    @Captor
    private lateinit var relatertPersonBoCaptor: ArgumentCaptor<RelatertPersonBo>

    @Captor
    private lateinit var sivilstandBoCaptor: ArgumentCaptor<SivilstandBo>

    @Captor
    private lateinit var kontantstotteBoCaptor: ArgumentCaptor<KontantstotteBo>

    @Captor
    private lateinit var barnetilsynBoCaptor: ArgumentCaptor<BarnetilsynBo>

    @Test
    fun `Skal opprette ny grunnlagspakke`() {
        Mockito.`when`(persistenceServiceMock.opprettNyGrunnlagspakke(MockitoHelper.capture(opprettGrunnlagspakkeRequestDtoCaptor)))
            .thenReturn(byggGrunnlagspakke())
        val nyGrunnlagspakkeIdOpprettet = grunnlagspakkeService.opprettGrunnlagspakke(byggNyGrunnlagspakkeRequest())
        val grunnlagspakke = opprettGrunnlagspakkeRequestDtoCaptor.value
        Mockito.verify(
            persistenceServiceMock,
            Mockito.times(1),
        ).opprettNyGrunnlagspakke(MockitoHelper.any(OpprettGrunnlagspakkeRequestDto::class.java))
        assertAll(
            { assertThat(nyGrunnlagspakkeIdOpprettet).isNotNull() },
            // sjekk GrunnlagspakkeDto
            { assertThat(grunnlagspakke).isNotNull() },
        )
    }

    @Test
    @Suppress("NonAsciiCharacters")
    fun `Skal hente grunnlagspakke med tilhørende grunnlag`() {
        Mockito.`when`(persistenceServiceMock.opprettNyGrunnlagspakke(MockitoHelper.capture(opprettGrunnlagspakkeRequestDtoCaptor)))
            .thenReturn(byggGrunnlagspakke())
        Mockito.`when`(persistenceServiceMock.opprettAinntekt(MockitoHelper.capture(ainntektBoCaptor)))
            .thenReturn(byggAinntekt())
        Mockito.`when`(persistenceServiceMock.opprettAinntektspost(MockitoHelper.capture(ainntektspostBoCaptor)))
            .thenReturn(byggAinntektspost())
        Mockito.`when`(persistenceServiceMock.opprettSkattegrunnlag(MockitoHelper.capture(skattegrunnlagBoCaptor)))
            .thenReturn(byggSkattegrunnlagSkatt())
        Mockito.`when`(persistenceServiceMock.opprettSkattegrunnlagspost(MockitoHelper.capture(skattegrunnlagspostBoCaptor)))
            .thenReturn(byggSkattegrunnlagspost())
        Mockito.`when`(
            persistenceServiceMock.opprettUtvidetBarnetrygdOgSmaabarnstillegg(
                MockitoHelper.capture(utvidetBarnetrygdOgSmaabarnstilleggBoCaptor),
            ),
        )
            .thenReturn(byggUtvidetBarnetrygdOgSmaabarnstillegg())
        Mockito.`when`(persistenceServiceMock.opprettBarnetillegg(MockitoHelper.capture(barnetilleggBoCaptor)))
            .thenReturn(byggBarnetillegg())
        Mockito.`when`(persistenceServiceMock.opprettRelatertPerson(MockitoHelper.capture(relatertPersonBoCaptor)))
            .thenReturn(byggEgetBarnIHusstanden())
        Mockito.`when`(persistenceServiceMock.opprettSivilstand(MockitoHelper.capture(sivilstandBoCaptor)))
            .thenReturn(byggSivilstand())
        Mockito.`when`(persistenceServiceMock.opprettKontantstotte(MockitoHelper.capture(kontantstotteBoCaptor)))
            .thenReturn(byggKontantstotte())
        Mockito.`when`(persistenceServiceMock.opprettBarnetilsyn(MockitoHelper.capture(barnetilsynBoCaptor)))
            .thenReturn(byggBarnetilsyn())

        val grunnlagspakkeIdOpprettet = grunnlagspakkeService.opprettGrunnlagspakke(byggNyGrunnlagspakkeRequest())
        val nyAinntektOpprettet = persistenceServiceMock.opprettAinntekt(byggAinntektBo())
        val nyAinntektspostOpprettet = persistenceServiceMock.opprettAinntektspost(byggAinntektspostBo())
        val nyttSkattegrunnlagOpprettet = persistenceServiceMock.opprettSkattegrunnlag(byggSkattegrunnlagSkattBo())
        val nySkattegrunnlagspostOpprettet = persistenceServiceMock.opprettSkattegrunnlagspost(byggSkattegrunnlagspostBo())
        val nyUtvidetBarnetrygdOgSmaabarnstilleggOpprettet =
            persistenceServiceMock.opprettUtvidetBarnetrygdOgSmaabarnstillegg(byggUtvidetBarnetrygdOgSmaabarnstilleggBo())
        val nyttBarnetilleggOpprettet = persistenceServiceMock.opprettBarnetillegg(byggBarnetilleggBo())
        val nyRelatertPersonOpprettet = persistenceServiceMock.opprettRelatertPerson(byggRelatertPersonBo())
        val nySivilstandOpprettet = persistenceServiceMock.opprettSivilstand(byggSivilstandBo())
        val nyKontantstotteOpprettet = persistenceServiceMock.opprettKontantstotte(byggKontantstotteBo())
        val nyBarnetilsynOpprettet = persistenceServiceMock.opprettBarnetilsyn(byggBarnetilsynBo())

        val opprettGrunnlagspakkeRequestDto = opprettGrunnlagspakkeRequestDtoCaptor.value
        val ainntektBoListe = ainntektBoCaptor.allValues
        val ainntektspostBoListe = ainntektspostBoCaptor.allValues
        val skattegrunnlagBoListe = skattegrunnlagBoCaptor.allValues
        val skattegrunnlagspostBoListe = skattegrunnlagspostBoCaptor.allValues
        val ubstBoListe = utvidetBarnetrygdOgSmaabarnstilleggBoCaptor.allValues
        val barnetilleggBoListe = barnetilleggBoCaptor.allValues
        val relatertPersonBoListe = relatertPersonBoCaptor.allValues
        val sivilstandBoListe = sivilstandBoCaptor.allValues
        val barnetilleggListe = barnetilleggBoCaptor.allValues
        val kontantstotteListe = kontantstotteBoCaptor.allValues
        val barnetilsynListe = barnetilsynBoCaptor.allValues

        Mockito.verify(
            persistenceServiceMock,
            Mockito.times(1),
        ).opprettNyGrunnlagspakke(MockitoHelper.any(OpprettGrunnlagspakkeRequestDto::class.java))
        Mockito.verify(persistenceServiceMock, Mockito.times(1)).opprettAinntekt(MockitoHelper.any(AinntektBo::class.java))
        Mockito.verify(persistenceServiceMock, Mockito.times(1)).opprettAinntektspost(MockitoHelper.any(AinntektspostBo::class.java))
        Mockito.verify(persistenceServiceMock, Mockito.times(1)).opprettSkattegrunnlag(MockitoHelper.any(SkattegrunnlagBo::class.java))
        Mockito.verify(persistenceServiceMock, Mockito.times(1)).opprettSkattegrunnlagspost(MockitoHelper.any(SkattegrunnlagspostBo::class.java))
        Mockito.verify(persistenceServiceMock, Mockito.times(1))
            .opprettUtvidetBarnetrygdOgSmaabarnstillegg(MockitoHelper.any(UtvidetBarnetrygdOgSmaabarnstilleggBo::class.java))
        Mockito.verify(persistenceServiceMock, Mockito.times(1)).opprettBarnetillegg(MockitoHelper.any(BarnetilleggBo::class.java))
        Mockito.verify(persistenceServiceMock, Mockito.times(1)).opprettRelatertPerson(MockitoHelper.any(RelatertPersonBo::class.java))
        Mockito.verify(persistenceServiceMock, Mockito.times(1)).opprettSivilstand(MockitoHelper.any(SivilstandBo::class.java))
        Mockito.verify(persistenceServiceMock, Mockito.times(1)).opprettKontantstotte(MockitoHelper.any(KontantstotteBo::class.java))
        Mockito.verify(persistenceServiceMock, Mockito.times(1)).opprettBarnetilsyn(MockitoHelper.any(BarnetilsynBo::class.java))

        assertAll(
            { assertThat(grunnlagspakkeIdOpprettet).isNotNull() },
            { assertThat(grunnlagspakkeIdOpprettet).isNotNull() },

            { assertThat(nyAinntektOpprettet).isNotNull() },
            { assertThat(nyAinntektOpprettet.personId).isNotNull() },

            { assertThat(nyAinntektspostOpprettet).isNotNull() },
            { assertThat(nyAinntektspostOpprettet.inntektId).isNotNull() },

            { assertThat(nyttSkattegrunnlagOpprettet).isNotNull() },
            { assertThat(nyttSkattegrunnlagOpprettet.personId).isNotNull() },

            { assertThat(nySkattegrunnlagspostOpprettet).isNotNull() },
            { assertThat(nySkattegrunnlagspostOpprettet.skattegrunnlagId).isNotNull() },

            { assertThat(nyUtvidetBarnetrygdOgSmaabarnstilleggOpprettet).isNotNull() },
            { assertThat(nyUtvidetBarnetrygdOgSmaabarnstilleggOpprettet.grunnlagspakkeId).isNotNull() },

            { assertThat(nyttBarnetilleggOpprettet).isNotNull() },
            { assertThat(nyttBarnetilleggOpprettet.grunnlagspakkeId).isNotNull() },

            { assertThat(nyRelatertPersonOpprettet).isNotNull() },
            { assertThat(nyRelatertPersonOpprettet.grunnlagspakkeId).isNotNull() },

            { assertThat(nySivilstandOpprettet).isNotNull() },
            { assertThat(nySivilstandOpprettet.sivilstandId).isNotNull() },

            { assertThat(nyKontantstotteOpprettet).isNotNull() },
            { assertThat(nyKontantstotteOpprettet.grunnlagspakkeId).isNotNull() },

            { assertThat(nyBarnetilsynOpprettet).isNotNull() },
            { assertThat(nyBarnetilsynOpprettet.grunnlagspakkeId).isNotNull() },

            // sjekk GrunnlagspakkeDto
            { assertThat(opprettGrunnlagspakkeRequestDto).isNotNull() },
            { assertThat(opprettGrunnlagspakkeRequestDto.opprettetAv).isNotNull() },
            { assertThat(opprettGrunnlagspakkeRequestDto.opprettetAv).isEqualTo("RTV9999") },
            { assertThat(opprettGrunnlagspakkeRequestDto.formaal).isEqualTo(Formål.BIDRAG) },

            // sjekk AinntektBo
            { assertThat(ainntektBoListe[0].personId).isEqualTo("1234567") },
            { assertThat(ainntektBoListe[0].aktiv).isTrue },
            { assertThat(ainntektBoListe[0].periodeFra).isEqualTo(LocalDate.parse("2021-07-01")) },
            { assertThat(ainntektBoListe[0].periodeTil).isEqualTo(LocalDate.parse("2021-08-01")) },

            // sjekk AinntektspostBo
            { assertThat(ainntektspostBoListe.size).isEqualTo(1) },
            { assertThat(ainntektspostBoListe[0].utbetalingsperiode).isEqualTo("202108") },
            { assertThat(ainntektspostBoListe[0].opptjeningsperiodeFra).isEqualTo(LocalDate.parse("2021-07-01")) },
            { assertThat(ainntektspostBoListe[0].opptjeningsperiodeTil).isEqualTo(LocalDate.parse("2021-08-01")) },
            { assertThat(ainntektspostBoListe[0].opplysningspliktigId).isEqualTo(("123")) },
            { assertThat(ainntektspostBoListe[0].inntektType).isEqualTo(("Loenn")) },
            { assertThat(ainntektspostBoListe[0].fordelType).isEqualTo(("Kontantytelse")) },
            { assertThat(ainntektspostBoListe[0].beskrivelse).isEqualTo(("Loenn/ferieLoenn")) },
            { assertThat(ainntektspostBoListe[0].belop).isEqualTo(BigDecimal.valueOf(50000)) },

            // sjekk SkattegrunnlagBo
            { assertThat(skattegrunnlagBoListe[0].personId).isEqualTo("7654321") },
            { assertThat(skattegrunnlagBoListe[0].aktiv).isTrue },
            { assertThat(skattegrunnlagBoListe[0].periodeFra).isEqualTo(LocalDate.parse("2021-01-01")) },
            { assertThat(skattegrunnlagBoListe[0].periodeTil).isEqualTo(LocalDate.parse("2022-01-01")) },

            // sjekk SkattegrunnlagspostBo
            { assertThat(skattegrunnlagspostBoListe.size).isEqualTo(1) },
            { assertThat(skattegrunnlagspostBoListe[0].inntektType).isEqualTo(("Loenn")) },
            { assertThat(skattegrunnlagspostBoListe[0].belop).isEqualTo(BigDecimal.valueOf(171717)) },

            // sjekk UtvidetBarnetrygdOgSmaabarnstilleggdBo
            { assertThat(ubstBoListe[0].personId).isEqualTo("1234567") },
            { assertThat(ubstBoListe[0].type).isEqualTo("Utvidet barnetrygd") },
            { assertThat(ubstBoListe[0].periodeFra).isEqualTo(LocalDate.parse("2021-01-01")) },
            { assertThat(ubstBoListe[0].periodeTil).isEqualTo(LocalDate.parse("2021-07-01")) },
            { assertThat(ubstBoListe[0].belop).isEqualTo(BigDecimal.valueOf(12468.01)) },
            { assertThat(ubstBoListe[0].manueltBeregnet).isFalse },
            { assertThat(ubstBoListe[0].deltBosted).isFalse },

            // sjekk BarnetilleggBo
            { assertThat(nyttBarnetilleggOpprettet).isNotNull() },
            { assertThat(nyttBarnetilleggOpprettet.grunnlagspakkeId).isNotNull() },
            { assertThat(barnetilleggBoListe[0].partPersonId).isEqualTo("1234567") },
            { assertThat(barnetilleggBoListe[0].barnPersonId).isEqualTo("0123456") },
            { assertThat(barnetilleggBoListe[0].barnetilleggType).isEqualTo("Utvidet barnetrygd") },
            { assertThat(barnetilleggBoListe[0].periodeFra).isEqualTo(LocalDate.parse("2021-01-01")) },
            { assertThat(barnetilleggBoListe[0].periodeTil).isEqualTo(LocalDate.parse("2021-07-01")) },
            { assertThat(barnetilleggBoListe[0].belopBrutto).isEqualTo(BigDecimal.valueOf(1000)) },
            { assertThat(barnetilleggBoListe[0].barnType).isEqualTo(BarnType.FELLES.toString()) },

            // sjekk RelatertPerson
            { assertThat(relatertPersonBoListe).isNotNull() },
            { assertThat(relatertPersonBoListe[0].grunnlagspakkeId).isNotNull() },
            { assertThat(relatertPersonBoListe[0].partPersonId).isEqualTo("1234567") },
            { assertThat(relatertPersonBoListe[0].relatertPersonPersonId).isEqualTo("7654321") },
            { assertThat(relatertPersonBoListe[0].navn).isEqualTo("navn1") },
            { assertThat(relatertPersonBoListe[0].fodselsdato).isEqualTo(LocalDate.parse("1997-05-23")) },
            { assertThat(relatertPersonBoListe[0].erBarnAvBmBp).isTrue },
            { assertThat(relatertPersonBoListe[0].husstandsmedlemPeriodeFra).isEqualTo(LocalDate.parse("2021-01-01")) },
            { assertThat(relatertPersonBoListe[0].husstandsmedlemPeriodeTil).isEqualTo(LocalDate.parse("2021-07-01")) },
            { assertThat(relatertPersonBoListe[0].aktiv).isTrue },
            { assertThat(relatertPersonBoListe[0].brukFra).isNotNull() },
            { assertThat(relatertPersonBoListe[0].brukTil).isNull() },
            { assertThat(relatertPersonBoListe[0].hentetTidspunkt).isNotNull() },

            // sjekk BarnetilleggDto
            { assertThat(barnetilleggListe[0].partPersonId).isEqualTo("1234567") },
            { assertThat(barnetilleggListe[0].barnPersonId).isEqualTo("0123456") },
            { assertThat(barnetilleggListe[0].barnetilleggType).isEqualTo("Utvidet barnetrygd") },
            { assertThat(barnetilleggListe[0].periodeFra).isEqualTo(LocalDate.parse("2021-01-01")) },
            { assertThat(barnetilleggListe[0].periodeTil).isEqualTo(LocalDate.parse("2021-07-01")) },
            { assertThat(barnetilleggListe[0].belopBrutto).isEqualTo(BigDecimal.valueOf(1000)) },
            { assertThat(barnetilleggListe[0].barnType).isEqualTo(BarnType.FELLES.toString()) },

            // sjekk KontantstotteDto
            { assertThat(kontantstotteListe[0].partPersonId).isEqualTo("1234567") },
            { assertThat(kontantstotteListe[0].barnPersonId).isEqualTo("0123456") },
            { assertThat(kontantstotteListe[0].periodeFra).isEqualTo(LocalDate.parse("2021-01-01")) },
            { assertThat(kontantstotteListe[0].periodeTil).isEqualTo(LocalDate.parse("2021-07-01")) },
            { assertThat(kontantstotteListe[0].belop).isEqualTo(7500) },

            // sjekk BarnetilsynDto
            { assertThat(barnetilsynListe[0].partPersonId).isEqualTo("1234567") },
            { assertThat(barnetilsynListe[0].barnPersonId).isEqualTo("0123456") },
            { assertThat(barnetilsynListe[0].periodeFra).isEqualTo(LocalDate.parse("2021-01-01")) },
            { assertThat(barnetilsynListe[0].periodeTil).isEqualTo(LocalDate.parse("2021-07-01")) },
            { assertThat(barnetilsynListe[0].belop).isEqualTo(7500) },

            // sjekk SivilstandBo
            { assertThat(sivilstandBoListe[0].personId).isEqualTo("1234") },
            { assertThat(sivilstandBoListe[0].periodeFra).isEqualTo(LocalDate.parse("2021-01-01")) },
            { assertThat(sivilstandBoListe[0].periodeTil).isEqualTo(LocalDate.parse("2021-07-01")) },
            { assertThat(sivilstandBoListe[0].sivilstand).isEqualTo(SivilstandskodePDL.SEPARERT_PARTNER.toString()) },
            { assertThat(sivilstandBoListe[0].aktiv).isTrue },
            { assertThat(sivilstandBoListe[0].brukFra).isNotNull() },
            { assertThat(sivilstandBoListe[0].brukTil).isNull() },
            { assertThat(sivilstandBoListe[0].hentetTidspunkt).isNotNull() },
        )
    }

    object MockitoHelper {

        // use this in place of captor.capture() if you are trying to capture an argument that is not nullable
        fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()
        fun <T> any(type: Class<T>): T = Mockito.any(type)
        fun <T> any(): T = Mockito.any()
    }
}
