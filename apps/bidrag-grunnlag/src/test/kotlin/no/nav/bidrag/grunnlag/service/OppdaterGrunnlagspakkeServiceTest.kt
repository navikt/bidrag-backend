package no.nav.bidrag.grunnlag.service

import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestStatus
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.enums.person.BarnType
import no.nav.bidrag.domene.enums.person.SivilstandskodePDL
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.bo.BarnetilleggBo
import no.nav.bidrag.grunnlag.bo.BarnetilsynBo
import no.nav.bidrag.grunnlag.bo.KontantstotteBo
import no.nav.bidrag.grunnlag.bo.RelatertPersonBo
import no.nav.bidrag.grunnlag.bo.SivilstandBo
import no.nav.bidrag.grunnlag.bo.UtvidetBarnetrygdOgSmaabarnstilleggBo
import no.nav.bidrag.grunnlag.consumer.bidragperson.BidragPersonConsumer
import no.nav.bidrag.grunnlag.consumer.bidragperson.api.HusstandsmedlemmerRequest
import no.nav.bidrag.grunnlag.consumer.familiebasak.FamilieBaSakConsumer
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.BisysStønadstype
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.FamilieBaSakRequest
import no.nav.bidrag.grunnlag.consumer.familieefsak.FamilieEfSakConsumer
import no.nav.bidrag.grunnlag.consumer.familieefsak.api.BarnetilsynRequest
import no.nav.bidrag.grunnlag.consumer.familiekssak.FamilieKsSakConsumer
import no.nav.bidrag.grunnlag.consumer.familiekssak.api.BisysDto
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.HentInntektListeRequest
import no.nav.bidrag.grunnlag.consumer.pensjon.PensjonConsumer
import no.nav.bidrag.grunnlag.consumer.pensjon.api.HentBarnetilleggPensjonRequest
import no.nav.bidrag.grunnlag.consumer.skattegrunnlag.SigrunConsumer
import no.nav.bidrag.grunnlag.consumer.skattegrunnlag.api.HentSummertSkattegrunnlagRequest
import no.nav.bidrag.grunnlag.exception.RestResponse
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("OppdaterGrunnlagspakkeServiceTest")
@ExtendWith(MockitoExtension::class)
class OppdaterGrunnlagspakkeServiceTest {

    @InjectMocks
    private lateinit var oppdaterGrunnlagspakkeService: OppdaterGrunnlagspakkeService

    @Mock
    private lateinit var persistenceServiceMock: PersistenceService

    @Mock
    private lateinit var familieBaSakConsumerMock: FamilieBaSakConsumer

    @Mock
    private lateinit var pensjonConsumerMock: PensjonConsumer

    @Mock
    private lateinit var inntektskomponentenServiceMock: InntektskomponentenService

    @Mock
    private lateinit var sigrunConsumerMock: SigrunConsumer

    @Mock
    private lateinit var bidragPersonConsumerMock: BidragPersonConsumer

    @Mock
    private lateinit var familieKsSakConsumerMock: FamilieKsSakConsumer

    @Mock
    private lateinit var familieEfSakConsumerMock: FamilieEfSakConsumer

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
    fun `Skal oppdatere grunnlagspakke med ainntekt`() {
        Mockito.`when`(
            persistenceServiceMock.hentFormaalGrunnlagspakke(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    Int::class.java,
                ),
            ),
        ).thenReturn("Bidrag")

        Mockito.`when`(
            inntektskomponentenServiceMock.hentInntekt(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    HentInntektListeRequest::class.java,
                ),
            ),
        )
            .thenReturn(TestUtil.byggHentInntektListeResponseIntern())

        val grunnlagspakkeIdOpprettet = TestUtil.byggGrunnlagspakke().grunnlagspakkeId
        val oppdatertGrunnlagspakke = oppdaterGrunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeIdOpprettet,
            TestUtil.byggOppdaterGrunnlagspakkeRequestAInntekt(),
            LocalDateTime.now(),
            emptyMap(),
        )

        Mockito.verify(persistenceServiceMock, Mockito.times(1))
            .oppdaterAinntektForGrunnlagspakke(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(Int::class.java),
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(),
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(LocalDate::class.java),
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(LocalDate::class.java),
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(),
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(LocalDateTime::class.java),
            )

        assertAll(
            {
                Assertions.assertThat(grunnlagspakkeIdOpprettet).isNotNull()
            },

            // sjekk oppdatertGrunnlagspakke
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagspakkeId).isEqualTo(grunnlagspakkeIdOpprettet) },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe.size).isEqualTo(1) },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].type).isEqualTo(
                    GrunnlagRequestType.AINNTEKT,
                )
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].personId)
                    .isEqualTo("12345678910")
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].status)
                    .isEqualTo(
                        GrunnlagRequestStatus.HENTET,
                    )
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].statusMelding)
                    .isEqualTo("Antall inntekter funnet for periode 2021-01 - 2021-12: 1")
            },
        )
    }

    @Test
    fun `Skal oppdatere grunnlagspakke med skattegrunnlag`() {
        Mockito.`when`(
            sigrunConsumerMock.hentSummertSkattegrunnlag(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    HentSummertSkattegrunnlagRequest::class.java,
                ),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggHentSkattegrunnlagResponse()))

        val grunnlagspakkeIdOpprettet = TestUtil.byggGrunnlagspakke().grunnlagspakkeId
        val oppdatertGrunnlagspakke = oppdaterGrunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeIdOpprettet,
            TestUtil.byggOppdaterGrunnlagspakkeRequestSkattegrunnlag(),
            LocalDateTime.now(),
            emptyMap(),
        )

        Mockito.verify(persistenceServiceMock, Mockito.times(1))
            .oppdaterSkattegrunnlagForGrunnlagspakke(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(Int::class.java),
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(),
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(LocalDate::class.java),
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(LocalDate::class.java),
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(),
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(LocalDateTime::class.java),
            )

        assertAll(
            { Assertions.assertThat(grunnlagspakkeIdOpprettet).isNotNull() },

            // sjekk oppdatertGrunnlagspakke
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagspakkeId).isEqualTo(grunnlagspakkeIdOpprettet) },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe.size).isEqualTo(1) },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].type).isEqualTo(GrunnlagRequestType.SKATTEGRUNNLAG) },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].personId).isEqualTo("12345678910") },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].status).isEqualTo(GrunnlagRequestStatus.HENTET) },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].statusMelding)
                    .isEqualTo("Antall skattegrunnlagsposter funnet for inntektsåret 2021: 2")
            },
        )
    }

    @Test
    fun `Skal oppdatere grunnlagspakke med utvidet barnetrygd`() {
        Mockito.`when`(
            persistenceServiceMock.opprettUtvidetBarnetrygdOgSmaabarnstillegg(
                GrunnlagspakkeServiceMockTest.MockitoHelper.capture(utvidetBarnetrygdOgSmaabarnstilleggBoCaptor),
            ),
        ).thenReturn(TestUtil.byggUtvidetBarnetrygdOgSmaabarnstillegg())
        Mockito.`when`(
            familieBaSakConsumerMock.hentFamilieBaSak(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    FamilieBaSakRequest::class.java,
                ),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggFamilieBaSakResponse()))

        val grunnlagspakkeIdOpprettet = TestUtil.byggGrunnlagspakke().grunnlagspakkeId
        val oppdatertGrunnlagspakke = oppdaterGrunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeIdOpprettet,
            TestUtil.byggOppdaterGrunnlagspakkeRequestUtvidetBarnetrygd(),
            LocalDateTime.now(),
            mapOf("12345678910" to listOf("12345678910")),
        )

        val ubstListe = utvidetBarnetrygdOgSmaabarnstilleggBoCaptor.allValues

        Mockito.verify(persistenceServiceMock, Mockito.times(2))
            .opprettUtvidetBarnetrygdOgSmaabarnstillegg(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    UtvidetBarnetrygdOgSmaabarnstilleggBo::class.java,
                ),
            )

        assertAll(
            {
                Assertions.assertThat(grunnlagspakkeIdOpprettet).isNotNull()
            },

            // sjekk UtvidetBarnetrygdOgSmaabarnstilleggDto
            { Assertions.assertThat(ubstListe).isNotNull() },
            { Assertions.assertThat(ubstListe.size).isEqualTo(2) },
            { Assertions.assertThat(ubstListe[0].personId).isEqualTo("12345678910") },
            { Assertions.assertThat(ubstListe[0].type).isEqualTo(BisysStønadstype.UTVIDET.toString()) },
            { Assertions.assertThat(ubstListe[0].periodeFra).isEqualTo(LocalDate.parse("2021-01-01")) },
            { Assertions.assertThat(ubstListe[0].periodeTil).isEqualTo(LocalDate.parse("2022-01-01")) },
            { Assertions.assertThat(ubstListe[0].belop).isEqualTo(BigDecimal.valueOf(1000.11)) },
            { Assertions.assertThat(ubstListe[0].manueltBeregnet).isFalse },
            { Assertions.assertThat(ubstListe[0].deltBosted).isFalse },

            // sjekk oppdatertGrunnlagspakke
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagspakkeId).isEqualTo(grunnlagspakkeIdOpprettet) },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe.size).isEqualTo(1) },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].type).isEqualTo(
                    GrunnlagRequestType.UTVIDET_BARNETRYGD_OG_SMÅBARNSTILLEGG,
                )
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].personId)
                    .isEqualTo("12345678910")
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].status)
                    .isEqualTo(
                        GrunnlagRequestStatus.HENTET,
                    )
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].statusMelding)
                    .isEqualTo("Antall perioder funnet: 2")
            },
        )
    }

    @Test
    fun `Skal oppdatere grunnlagspakke med barnetillegg fra Pensjon`() {
        Mockito.`when`(persistenceServiceMock.opprettBarnetillegg(GrunnlagspakkeServiceMockTest.MockitoHelper.capture(barnetilleggBoCaptor)))
            .thenReturn(
                TestUtil.byggBarnetillegg(),
            )
        Mockito.`when`(
            pensjonConsumerMock.hentBarnetilleggPensjon(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    HentBarnetilleggPensjonRequest::class.java,
                ),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggHentBarnetilleggPensjonResponse()))

        val grunnlagspakkeIdOpprettet = TestUtil.byggGrunnlagspakke().grunnlagspakkeId
        val oppdatertGrunnlagspakke = oppdaterGrunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeIdOpprettet,
            TestUtil.byggOppdaterGrunnlagspakkeRequestBarnetillegg(),
            LocalDateTime.now(),
            mapOf("12345678910" to listOf("12345678910")),
        )

        val barnetilleggListe = barnetilleggBoCaptor.allValues

        Mockito.verify(persistenceServiceMock, Mockito.times(2)).opprettBarnetillegg(
            GrunnlagspakkeServiceMockTest.MockitoHelper.any(BarnetilleggBo::class.java),
        )

        assertAll(
            { Assertions.assertThat(grunnlagspakkeIdOpprettet).isNotNull() },
            { Assertions.assertThat(grunnlagspakkeIdOpprettet).isNotNull() },

            // sjekk BarnetilleggDto
            { Assertions.assertThat(barnetilleggListe).isNotNull() },
            { Assertions.assertThat(barnetilleggListe.size).isEqualTo(2) },
            { Assertions.assertThat(barnetilleggListe[0].partPersonId).isEqualTo("12345678910") },
            { Assertions.assertThat(barnetilleggListe[0].barnPersonId).isEqualTo("barnIdent") },
            {
                Assertions.assertThat(barnetilleggListe[0].barnetilleggType)
                    .isEqualTo(Inntektstype.BARNETILLEGG_PENSJON.toString())
            },
            { Assertions.assertThat(barnetilleggListe[0].periodeFra).isEqualTo(LocalDate.parse("2021-01-01")) },
            { Assertions.assertThat(barnetilleggListe[0].periodeTil).isEqualTo(LocalDate.parse("2022-01-01")) },
            { Assertions.assertThat(barnetilleggListe[0].belopBrutto).isEqualTo(BigDecimal.valueOf(1001.45)) },
            { Assertions.assertThat(barnetilleggListe[0].barnType).isEqualTo(BarnType.FELLES.toString()) },

            { Assertions.assertThat(barnetilleggListe[1].partPersonId).isEqualTo("12345678910") },
            { Assertions.assertThat(barnetilleggListe[1].barnPersonId).isEqualTo("barnIdent") },
            {
                Assertions.assertThat(barnetilleggListe[1].barnetilleggType)
                    .isEqualTo(Inntektstype.BARNETILLEGG_PENSJON.toString())
            },
            { Assertions.assertThat(barnetilleggListe[1].periodeFra).isEqualTo(LocalDate.parse("2022-01-01")) },
            { Assertions.assertThat(barnetilleggListe[1].periodeTil).isEqualTo(LocalDate.parse("2023-01-01")) },
            { Assertions.assertThat(barnetilleggListe[1].belopBrutto).isEqualTo(BigDecimal.valueOf(2002.50)) },
            { Assertions.assertThat(barnetilleggListe[1].barnType).isEqualTo(BarnType.FELLES.toString()) },

            // sjekk oppdatertGrunnlagspakke
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagspakkeId).isEqualTo(grunnlagspakkeIdOpprettet) },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe.size).isEqualTo(1) },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].type)
                    .isEqualTo(GrunnlagRequestType.BARNETILLEGG)
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].personId)
                    .isEqualTo("12345678910")
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].status)
                    .isEqualTo(GrunnlagRequestStatus.HENTET)
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].statusMelding)
                    .isEqualTo("Antall perioder funnet: 2")
            },
        )
    }

    @Test
    fun `Skal oppdatere grunnlagspakke med egne barn i husstanden fra PDL via bidrag-person`() {
        Mockito.`when`(
            bidragPersonConsumerMock.hentForelderBarnRelasjon(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    Personident::class.java,
                ),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggHentForelderBarnRelasjonerResponse()))

        Mockito.`when`(
            bidragPersonConsumerMock.hentNavnFødselOgDød(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    Personident::class.java,
                ),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggHentNavnFoedselOgDoedResponse()))

        Mockito.`when`(
            bidragPersonConsumerMock.hentHusstandsmedlemmer(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    HusstandsmedlemmerRequest::class.java,
                ),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggHentHusstandsmedlemmerResponse()))

        Mockito.`when`(persistenceServiceMock.opprettRelatertPerson(GrunnlagspakkeServiceMockTest.MockitoHelper.capture(relatertPersonBoCaptor)))
            .thenReturn(
                TestUtil.byggEgetBarnIHusstanden(),
            )

        val grunnlagspakkeIdOpprettet = TestUtil.byggGrunnlagspakke().grunnlagspakkeId
        val oppdatertGrunnlagspakke = oppdaterGrunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeIdOpprettet,
            TestUtil.byggOppdaterGrunnlagspakkeRequestHusstandsmedlemmerOgEgneBarn(),
            LocalDateTime.now(),
            mapOf("12345678910" to listOf("12345678910")),
        )

        val relatertPersonListe = relatertPersonBoCaptor.allValues

        assertAll(
            { Assertions.assertThat(grunnlagspakkeIdOpprettet).isNotNull() },

            // sjekk GrunnlagspakkeBo
            { Assertions.assertThat(oppdatertGrunnlagspakke).isNotNull() },

            // sjekk relatertPersonBo
            { Assertions.assertThat(relatertPersonListe.size).isEqualTo(5) },
            { Assertions.assertThat(relatertPersonListe[0]?.partPersonId).isEqualTo("12345678910") },
            { Assertions.assertThat(relatertPersonListe[0]?.relatertPersonPersonId).isEqualTo("111") },
            { Assertions.assertThat(relatertPersonListe[0]?.aktiv).isTrue() },
            { Assertions.assertThat(relatertPersonListe[0]?.brukFra).isNotNull() },
            { Assertions.assertThat(relatertPersonListe[0]?.brukTil).isNull() },
            { Assertions.assertThat(relatertPersonListe[0]?.hentetTidspunkt).isNotNull() },

            // sjekk oppdatertGrunnlagspakke
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagspakkeId).isEqualTo(grunnlagspakkeIdOpprettet) },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe.size).isEqualTo(1) },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].type)
                    .isEqualTo(GrunnlagRequestType.HUSSTANDSMEDLEMMER_OG_EGNE_BARN)
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].personId)
                    .isEqualTo("12345678910")
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].status)
                    .isEqualTo(GrunnlagRequestStatus.HENTET)
            },
        )
    }

    @Test
    fun `Skal oppdatere grunnlagspakke med husstandsmedlemmer fra PDL via bidrag-person`() {
        Mockito.`when`(persistenceServiceMock.opprettRelatertPerson(GrunnlagspakkeServiceMockTest.MockitoHelper.capture(relatertPersonBoCaptor)))
            .thenReturn(
                TestUtil.byggHusstandsmedlem(),
            )

        Mockito.`when`(
            bidragPersonConsumerMock.hentForelderBarnRelasjon(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    Personident::class.java,
                ),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggHentForelderBarnRelasjonerResponse()))

        Mockito.`when`(
            bidragPersonConsumerMock.hentNavnFødselOgDød(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    Personident::class.java,
                ),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggHentNavnFoedselOgDoedResponse()))

        Mockito.`when`(
            bidragPersonConsumerMock.hentHusstandsmedlemmer(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    HusstandsmedlemmerRequest::class.java,
                ),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggHentHusstandsmedlemmerResponseTestPerioder()))

        val grunnlagspakkeIdOpprettet = TestUtil.byggGrunnlagspakke().grunnlagspakkeId
        val oppdatertGrunnlagspakke = oppdaterGrunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeIdOpprettet,
            TestUtil.byggOppdaterGrunnlagspakkeRequestHusstandsmedlemmerOgEgneBarn(),
            LocalDateTime.now(),
            mapOf("12345678910" to listOf("12345678910")),
        )

        val husstandsmedlemListe = relatertPersonBoCaptor.allValues

        assertAll(
            { Assertions.assertThat(grunnlagspakkeIdOpprettet).isNotNull() },

            // sjekk GrunnlagspakkeBo
            { Assertions.assertThat(oppdatertGrunnlagspakke).isNotNull() },

            { Assertions.assertThat(husstandsmedlemListe?.get(0)?.partPersonId).isEqualTo("12345678910") },
            { Assertions.assertThat(husstandsmedlemListe?.get(0)?.relatertPersonPersonId).isEqualTo("111") },
            { Assertions.assertThat(husstandsmedlemListe?.get(0)?.navn).isEqualTo("fornavn1 mellomnavn1 etternavn1") },
            { Assertions.assertThat(husstandsmedlemListe?.get(0)?.husstandsmedlemPeriodeFra).isEqualTo(LocalDate.parse("2011-05-17")) },
            { Assertions.assertThat(husstandsmedlemListe?.get(0)?.husstandsmedlemPeriodeTil).isEqualTo(LocalDate.parse("2021-04-17")) },
            { Assertions.assertThat(husstandsmedlemListe?.get(0)?.hentetTidspunkt).isNotNull() },

            { Assertions.assertThat(husstandsmedlemListe?.get(1)?.relatertPersonPersonId).isEqualTo("555") },
            { Assertions.assertThat(husstandsmedlemListe?.get(1)?.navn).isEqualTo("fornavn5 mellomnavn5 etternavn5") },
            { Assertions.assertThat(husstandsmedlemListe?.get(1)?.husstandsmedlemPeriodeFra).isEqualTo(LocalDate.parse("2020-01-01")) },
            { Assertions.assertThat(husstandsmedlemListe?.get(1)?.husstandsmedlemPeriodeTil).isNull() },
            { Assertions.assertThat(husstandsmedlemListe?.get(1)?.hentetTidspunkt).isNotNull() },

            // sjekk oppdatertGrunnlagspakke
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagspakkeId).isEqualTo(grunnlagspakkeIdOpprettet) },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe.size).isEqualTo(1) },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].type)
                    .isEqualTo(GrunnlagRequestType.HUSSTANDSMEDLEMMER_OG_EGNE_BARN)
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].personId)
                    .isEqualTo("12345678910")
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].status)
                    .isEqualTo(GrunnlagRequestStatus.HENTET)
            },
        )
    }

    @Test
    fun `Skal oppdatere grunnlagspakke med sivilstand fra PDL via bidrag-person`() {
        Mockito.`when`(persistenceServiceMock.opprettSivilstand(GrunnlagspakkeServiceMockTest.MockitoHelper.capture(sivilstandBoCaptor)))
            .thenReturn(
                TestUtil.byggSivilstand(),
            )
        Mockito.`when`(
            bidragPersonConsumerMock.hentSivilstand(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    Personident::class.java,
                ),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggHentSivilstandResponse()))

        val grunnlagspakkeIdOpprettet = TestUtil.byggGrunnlagspakke().grunnlagspakkeId
        val oppdatertGrunnlagspakke = oppdaterGrunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeIdOpprettet,
            TestUtil.byggOppdaterGrunnlagspakkeRequestSivilstand(),
            LocalDateTime.now(),
            mapOf("12345678910" to listOf("12345678910")),
        )

        val sivilstandListe = sivilstandBoCaptor.allValues

        assertAll(
            { Assertions.assertThat(grunnlagspakkeIdOpprettet).isNotNull() },

            // sjekk GrunnlagspakkeBo
            { Assertions.assertThat(oppdatertGrunnlagspakke).isNotNull() },

            // sjekk SivilstandBo
            { Assertions.assertThat(sivilstandListe).isNotNull() },
            { Assertions.assertThat(sivilstandListe.size).isEqualTo(3) },
            { Assertions.assertThat(sivilstandListe[0].personId).isEqualTo("12345678910") },
            { Assertions.assertThat(sivilstandListe[0].periodeFra).isNull() },
            { Assertions.assertThat(sivilstandListe[0].periodeTil).isEqualTo(LocalDate.parse("2021-02-01")) },
            { Assertions.assertThat(sivilstandListe[0].sivilstand).isEqualTo(SivilstandskodePDL.SEPARERT_PARTNER.toString()) },

            { Assertions.assertThat(sivilstandListe[1].personId).isEqualTo("12345678910") },
            { Assertions.assertThat(sivilstandListe[1].periodeFra).isEqualTo(LocalDate.parse("2021-02-01")) },
            { Assertions.assertThat(sivilstandListe[1].periodeTil).isEqualTo(LocalDate.parse("2021-09-01")) },
            { Assertions.assertThat(sivilstandListe[1].sivilstand).isEqualTo(SivilstandskodePDL.ENKE_ELLER_ENKEMANN.toString()) },

            { Assertions.assertThat(sivilstandListe[2].personId).isEqualTo("12345678910") },
            { Assertions.assertThat(sivilstandListe[2].periodeFra).isEqualTo(LocalDate.parse("2021-09-01")) },
            { Assertions.assertThat(sivilstandListe[2].periodeTil).isNull() },
            { Assertions.assertThat(sivilstandListe[2].sivilstand).isEqualTo(SivilstandskodePDL.GJENLEVENDE_PARTNER.toString()) },

            // sjekk oppdatertGrunnlagspakke
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagspakkeId).isEqualTo(grunnlagspakkeIdOpprettet) },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe.size).isEqualTo(1) },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].type).isEqualTo(GrunnlagRequestType.SIVILSTAND) },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].personId).isEqualTo("12345678910") },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].status).isEqualTo(GrunnlagRequestStatus.HENTET) },
            {
                Assertions.assertThat(
                    oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].statusMelding,
                ).isEqualTo("Antall sivilstandsforekomster funnet: 3")
            },
        )
    }

    @Test
    fun `Test på sortering av mottatt sivilstand og logikk for periodisering`() {
        Mockito.`when`(persistenceServiceMock.opprettSivilstand(GrunnlagspakkeServiceMockTest.MockitoHelper.capture(sivilstandBoCaptor)))
            .thenReturn(
                TestUtil.byggSivilstand(),
            )
        Mockito.`when`(
            bidragPersonConsumerMock.hentSivilstand(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    Personident::class.java,
                ),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggHentSivilstandResponseTestSortering()))

        val grunnlagspakkeIdOpprettet = TestUtil.byggGrunnlagspakke().grunnlagspakkeId
        val oppdatertGrunnlagspakke = oppdaterGrunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeIdOpprettet,
            TestUtil.byggOppdaterGrunnlagspakkeRequestSivilstand(),
            LocalDateTime.now(),
            mapOf("12345678910" to listOf("12345678910")),
        )

        val sivilstandListe = sivilstandBoCaptor.allValues

        assertAll(
            { Assertions.assertThat(grunnlagspakkeIdOpprettet).isNotNull() },

            // sjekk GrunnlagspakkeBo
            { Assertions.assertThat(oppdatertGrunnlagspakke).isNotNull() },

            // sjekk SivilstandBo
            { Assertions.assertThat(sivilstandListe).isNotNull() },
            { Assertions.assertThat(sivilstandListe.size).isEqualTo(5) },
            { Assertions.assertThat(sivilstandListe[0].periodeFra).isEqualTo(LocalDate.parse("2001-05-01")) },
            { Assertions.assertThat(sivilstandListe[0].periodeTil).isEqualTo(LocalDate.parse("2011-02-01")) },
            { Assertions.assertThat(sivilstandListe[0].sivilstand).isEqualTo(SivilstandskodePDL.UOPPGITT.toString()) },

            { Assertions.assertThat(sivilstandListe[1].periodeFra).isEqualTo(LocalDate.parse("2011-02-01")) },
            { Assertions.assertThat(sivilstandListe[1].periodeTil).isEqualTo(LocalDate.parse("2017-07-17")) },
            { Assertions.assertThat(sivilstandListe[1].sivilstand).isEqualTo(SivilstandskodePDL.UGIFT.toString()) },

            { Assertions.assertThat(sivilstandListe[2].periodeFra).isEqualTo(LocalDate.parse("2017-07-17")) },
            { Assertions.assertThat(sivilstandListe[2].periodeTil).isEqualTo(LocalDate.parse("2021-09-01")) },
            { Assertions.assertThat(sivilstandListe[2].sivilstand).isEqualTo(SivilstandskodePDL.GIFT.toString()) },

            { Assertions.assertThat(sivilstandListe[3].periodeFra).isEqualTo(LocalDate.parse("2021-09-01")) },
            { Assertions.assertThat(sivilstandListe[3].periodeTil).isEqualTo(LocalDate.parse("2022-03-01")) },
            { Assertions.assertThat(sivilstandListe[3].sivilstand).isEqualTo(SivilstandskodePDL.SEPARERT.toString()) },

            { Assertions.assertThat(sivilstandListe[4].periodeFra).isEqualTo(LocalDate.parse("2022-03-01")) },
            { Assertions.assertThat(sivilstandListe[4].periodeTil).isNull() },
            { Assertions.assertThat(sivilstandListe[4].sivilstand).isEqualTo(SivilstandskodePDL.SKILT.toString()) },

            // sjekk oppdatertGrunnlagspakke
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagspakkeId).isEqualTo(grunnlagspakkeIdOpprettet) },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe.size).isEqualTo(1) },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].type).isEqualTo(GrunnlagRequestType.SIVILSTAND) },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].personId).isEqualTo("12345678910") },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].status).isEqualTo(GrunnlagRequestStatus.HENTET) },
            {
                Assertions.assertThat(
                    oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].statusMelding,
                ).isEqualTo("Antall sivilstandsforekomster funnet: 5")
            },
        )
    }

    @Test
    fun `Test på sortering av mottatt sivilstand uten datoinformasjon der alle er historiske`() {
        Mockito.`when`(persistenceServiceMock.opprettSivilstand(GrunnlagspakkeServiceMockTest.MockitoHelper.capture(sivilstandBoCaptor)))
            .thenReturn(
                TestUtil.byggSivilstand(),
            )
        Mockito.`when`(
            bidragPersonConsumerMock.hentSivilstand(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    Personident::class.java,
                ),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggHentSivilstandResponseTestUtenDatoerHistoriske()))

        val grunnlagspakkeIdOpprettet = TestUtil.byggGrunnlagspakke().grunnlagspakkeId
        oppdaterGrunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeIdOpprettet,
            TestUtil.byggOppdaterGrunnlagspakkeRequestSivilstand(),
            LocalDateTime.now(),
            mapOf("12345678910" to listOf("12345678910")),
        )

        val sivilstandListe = sivilstandBoCaptor.allValues

        assertAll(
            // sjekk SivilstandBo
            { Assertions.assertThat(sivilstandListe[0].periodeFra).isNull() },
            { Assertions.assertThat(sivilstandListe[0].periodeTil).isNull() },
            { Assertions.assertThat(sivilstandListe[0].sivilstand).isEqualTo(SivilstandskodePDL.GIFT.toString()) },

            { Assertions.assertThat(sivilstandListe[1].periodeFra).isNull() },
            { Assertions.assertThat(sivilstandListe[1].periodeTil).isNull() },
            { Assertions.assertThat(sivilstandListe[1].sivilstand).isEqualTo(SivilstandskodePDL.SKILT.toString()) },
        )
    }

    @Test
    fun `Test på sortering av mottatt sivilstand uten datoinformasjon med registrert timestamp og aktiv forekomst`() {
        Mockito.`when`(persistenceServiceMock.opprettSivilstand(GrunnlagspakkeServiceMockTest.MockitoHelper.capture(sivilstandBoCaptor)))
            .thenReturn(
                TestUtil.byggSivilstand(),
            )
        Mockito.`when`(
            bidragPersonConsumerMock.hentSivilstand(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    Personident::class.java,
                ),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggHentSivilstandResponseTestUtenDatoer()))

        val grunnlagspakkeIdOpprettet = TestUtil.byggGrunnlagspakke().grunnlagspakkeId
        oppdaterGrunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeIdOpprettet,
            TestUtil.byggOppdaterGrunnlagspakkeRequestSivilstand(),
            LocalDateTime.now(),
            mapOf("12345678910" to listOf("12345678910")),
        )

        val sivilstandListe = sivilstandBoCaptor.allValues

        assertAll(
            // sjekk SivilstandBo
            { Assertions.assertThat(sivilstandListe[0].periodeFra).isNull() },
            { Assertions.assertThat(sivilstandListe[0].periodeTil).isEqualTo(LocalDate.parse("2020-05-12")) },
            { Assertions.assertThat(sivilstandListe[0].sivilstand).isEqualTo(SivilstandskodePDL.GIFT.toString()) },

            { Assertions.assertThat(sivilstandListe[1].periodeFra).isEqualTo(LocalDate.parse("2020-05-12")) },
            { Assertions.assertThat(sivilstandListe[1].periodeTil).isNull() },
            { Assertions.assertThat(sivilstandListe[1].sivilstand).isEqualTo(SivilstandskodePDL.SKILT.toString()) },
        )
    }

    @Test
    fun `Test sivilstand uten datoinformasjon med registrert timestamp kun én historisk forekomst`() {
        Mockito.`when`(persistenceServiceMock.opprettSivilstand(GrunnlagspakkeServiceMockTest.MockitoHelper.capture(sivilstandBoCaptor)))
            .thenReturn(
                TestUtil.byggSivilstand(),
            )
        Mockito.`when`(
            bidragPersonConsumerMock.hentSivilstand(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    Personident::class.java,
                ),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggHentSivilstandResponseTestUtenDatoerMedRegistrertEnForekomstHistorisk()))

        val grunnlagspakkeIdOpprettet = TestUtil.byggGrunnlagspakke().grunnlagspakkeId
        oppdaterGrunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeIdOpprettet,
            TestUtil.byggOppdaterGrunnlagspakkeRequestSivilstand(),
            LocalDateTime.now(),
            mapOf("12345678910" to listOf("12345678910")),
        )

        val sivilstandListe = sivilstandBoCaptor.allValues

        assertAll(
            // sjekk SivilstandBo
            { Assertions.assertThat(sivilstandListe[0].periodeFra).isNull() },
            { Assertions.assertThat(sivilstandListe[0].periodeTil).isNull() },
            { Assertions.assertThat(sivilstandListe[0].sivilstand).isEqualTo(SivilstandskodePDL.GIFT.toString()) },
        )
    }

    @Test
    fun `Test sivilstand uten datoinformasjon med registrert timestamp kun én aktiv forekomst`() {
        Mockito.`when`(persistenceServiceMock.opprettSivilstand(GrunnlagspakkeServiceMockTest.MockitoHelper.capture(sivilstandBoCaptor)))
            .thenReturn(
                TestUtil.byggSivilstand(),
            )
        Mockito.`when`(
            bidragPersonConsumerMock.hentSivilstand(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    Personident::class.java,
                ),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggHentSivilstandResponseTestUtenDatoerMedRegistrertEnForekomstAktiv()))

        val grunnlagspakkeIdOpprettet = TestUtil.byggGrunnlagspakke().grunnlagspakkeId
        oppdaterGrunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeIdOpprettet,
            TestUtil.byggOppdaterGrunnlagspakkeRequestSivilstand(),
            LocalDateTime.now(),
            mapOf("12345678910" to listOf("12345678910")),
        )

        val sivilstandListe = sivilstandBoCaptor.allValues

        assertAll(
            // sjekk SivilstandBo
            { Assertions.assertThat(sivilstandListe[0].periodeFra).isEqualTo(LocalDate.parse("2017-03-01")) },
            { Assertions.assertThat(sivilstandListe[0].periodeTil).isNull() },
            { Assertions.assertThat(sivilstandListe[0].sivilstand).isEqualTo(SivilstandskodePDL.GIFT.toString()) },
        )
    }

    @Test
    fun `Test sivilstand uten datoinformasjon uten registrert timestamp kun én historisk forekomst`() {
        Mockito.`when`(persistenceServiceMock.opprettSivilstand(GrunnlagspakkeServiceMockTest.MockitoHelper.capture(sivilstandBoCaptor)))
            .thenReturn(
                TestUtil.byggSivilstand(),
            )
        Mockito.`when`(
            bidragPersonConsumerMock.hentSivilstand(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(
                    Personident::class.java,
                ),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggHentSivilstandResponseTestUtenDatoerUtenRegistrertEnForekomstHistorisk()))

        val grunnlagspakkeIdOpprettet = TestUtil.byggGrunnlagspakke().grunnlagspakkeId
        oppdaterGrunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeIdOpprettet,
            TestUtil.byggOppdaterGrunnlagspakkeRequestSivilstand(),
            LocalDateTime.now(),
            mapOf("12345678910" to listOf("12345678910")),
        )

        val sivilstandListe = sivilstandBoCaptor.allValues

        assertAll(
            // sjekk SivilstandBo
            { Assertions.assertThat(sivilstandListe[0].periodeFra).isNull() },
            { Assertions.assertThat(sivilstandListe[0].periodeTil).isNull() },
            { Assertions.assertThat(sivilstandListe[0].sivilstand).isEqualTo(SivilstandskodePDL.GIFT.toString()) },
        )
    }

    @Test
    fun `skal oppdatere grunnlagspakke med kontantstotte`() {
        Mockito.`when`(persistenceServiceMock.opprettKontantstotte(GrunnlagspakkeServiceMockTest.MockitoHelper.capture(kontantstotteBoCaptor)))
            .thenReturn(
                TestUtil.byggKontantstotte(),
            )
        Mockito.`when`(
            familieKsSakConsumerMock.hentKontantstøtte(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(BisysDto::class.java),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggKontantstøtteResponse()))

        val grunnlagspakkeIdOpprettet = TestUtil.byggGrunnlagspakke().grunnlagspakkeId
        val oppdatertGrunnlagspakke = oppdaterGrunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeIdOpprettet,
            TestUtil.byggOppdaterGrunnlagspakkeRequestKontantstotte(),
            LocalDateTime.now(),
            mapOf("12345678910" to listOf("12345678910")),
        )

        val kontantstotteListe = kontantstotteBoCaptor.allValues

        Mockito.verify(persistenceServiceMock, Mockito.times(3)).opprettKontantstotte(
            GrunnlagspakkeServiceMockTest.MockitoHelper.any(KontantstotteBo::class.java),
        )

        assertAll(
            { Assertions.assertThat(grunnlagspakkeIdOpprettet).isNotNull() },

            // sjekk KontantstotteDto
            { Assertions.assertThat(kontantstotteListe.size).isEqualTo(3) },
            { Assertions.assertThat(kontantstotteListe[0].partPersonId).isEqualTo("12345678910") },
            { Assertions.assertThat(kontantstotteListe[0].barnPersonId).isEqualTo(TestUtil.BARN_PERSON_ID_KONTANTSTOTTE) },
            { Assertions.assertThat(kontantstotteListe[0].periodeFra).isEqualTo(LocalDate.parse("2022-01-01")) },
            { Assertions.assertThat(kontantstotteListe[0].periodeTil).isEqualTo(LocalDate.parse("2023-01-01")) },
            { Assertions.assertThat(kontantstotteListe[0].belop).isEqualTo(7500) },
            { Assertions.assertThat(kontantstotteListe[1].partPersonId).isEqualTo("12345678910") },
            { Assertions.assertThat(kontantstotteListe[1].barnPersonId).isEqualTo("15544332211") },
            { Assertions.assertThat(kontantstotteListe[1].periodeFra).isEqualTo(LocalDate.parse("2022-01-01")) },
            { Assertions.assertThat(kontantstotteListe[1].periodeTil).isEqualTo(LocalDate.parse("2023-01-01")) },
            { Assertions.assertThat(kontantstotteListe[1].belop).isEqualTo(7500) },
            { Assertions.assertThat(kontantstotteListe[2].partPersonId).isEqualTo("12345678910") },
            { Assertions.assertThat(kontantstotteListe[2].barnPersonId).isEqualTo(TestUtil.BARN_PERSON_ID_KONTANTSTOTTE) },
            { Assertions.assertThat(kontantstotteListe[2].periodeFra).isEqualTo(LocalDate.parse("2023-01-01")) },
            { Assertions.assertThat(kontantstotteListe[2].periodeTil).isEqualTo(LocalDate.parse("2023-07-01")) },
            { Assertions.assertThat(kontantstotteListe[2].belop).isEqualTo(5000) },

            // sjekk oppdatertGrunnlagspakke
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagspakkeId).isEqualTo(grunnlagspakkeIdOpprettet) },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe.size).isEqualTo(1) },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].type)
                    .isEqualTo(GrunnlagRequestType.KONTANTSTØTTE)
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].personId)
                    .isEqualTo("12345678910")
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].status)
                    .isEqualTo(GrunnlagRequestStatus.HENTET)
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].statusMelding)
                    .isEqualTo("Antall perioder funnet: 3")
            },
        )
    }

    @Test
    fun `skal oppdatere grunnlagspakke med barnetilsyn`() {
        Mockito.`when`(persistenceServiceMock.opprettBarnetilsyn(GrunnlagspakkeServiceMockTest.MockitoHelper.capture(barnetilsynBoCaptor)))
            .thenReturn(
                TestUtil.byggBarnetilsyn(),
            )
        Mockito.`when`(
            familieEfSakConsumerMock.hentBarnetilsyn(
                GrunnlagspakkeServiceMockTest.MockitoHelper.any(BarnetilsynRequest::class.java),
            ),
        )
            .thenReturn(RestResponse.Success(TestUtil.byggBarnetilsynResponse()))

        val grunnlagspakkeIdOpprettet = TestUtil.byggGrunnlagspakke().grunnlagspakkeId
        val oppdatertGrunnlagspakke = oppdaterGrunnlagspakkeService.oppdaterGrunnlagspakke(
            grunnlagspakkeIdOpprettet,
            TestUtil.byggOppdaterGrunnlagspakkeRequestBarnetilsyn(),
            LocalDateTime.now(),
            mapOf("12345678910" to listOf("12345678910")),
        )

        val barnetilsynListe = barnetilsynBoCaptor.allValues

        Mockito.verify(persistenceServiceMock, Mockito.times(2)).opprettBarnetilsyn(
            GrunnlagspakkeServiceMockTest.MockitoHelper.any(BarnetilsynBo::class.java),
        )

        assertAll(
            { Assertions.assertThat(grunnlagspakkeIdOpprettet).isNotNull() },

            // sjekk BarnetilsynDto
            { Assertions.assertThat(barnetilsynListe.size).isEqualTo(2) },
            { Assertions.assertThat(barnetilsynListe[0].partPersonId).isEqualTo("12345678910") },
            { Assertions.assertThat(barnetilsynListe[0].barnPersonId).isEqualTo(TestUtil.BARN_PERSON_ID_BARNETILSYN_1) },
            { Assertions.assertThat(barnetilsynListe[0].periodeFra).isEqualTo(LocalDate.parse("2021-01-01")) },
            { Assertions.assertThat(barnetilsynListe[0].periodeTil).isEqualTo(LocalDate.parse("2021-08-01")) },
            { Assertions.assertThat(barnetilsynListe[0].belop).isNull() },
            { Assertions.assertThat(barnetilsynListe[1].partPersonId).isEqualTo("12345678910") },
            { Assertions.assertThat(barnetilsynListe[1].barnPersonId).isEqualTo(TestUtil.BARN_PERSON_ID_BARNETILSYN_2) },
            { Assertions.assertThat(barnetilsynListe[1].periodeFra).isEqualTo(LocalDate.parse("2021-01-01")) },
            { Assertions.assertThat(barnetilsynListe[1].periodeTil).isEqualTo(LocalDate.parse("2021-08-01")) },
            { Assertions.assertThat(barnetilsynListe[1].belop).isNull() },

            // sjekk oppdatertGrunnlagspakke
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagspakkeId).isEqualTo(grunnlagspakkeIdOpprettet) },
            { Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe.size).isEqualTo(1) },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].type)
                    .isEqualTo(GrunnlagRequestType.BARNETILSYN)
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].personId)
                    .isEqualTo("12345678910")
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].status)
                    .isEqualTo(GrunnlagRequestStatus.HENTET)
            },
            {
                Assertions.assertThat(oppdatertGrunnlagspakke.grunnlagTypeResponsListe[0].statusMelding)
                    .isEqualTo("Antall perioder funnet: 1")
            },
        )
    }
}
