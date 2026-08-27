package no.nav.bidrag.beregn.barnebidrag.service

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.beregn.barnebidrag.felles.FellesTest
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningPersonConsumer
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningSakConsumer
import no.nav.bidrag.beregn.barnebidrag.service.external.VedtakService
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.AldersjusteringOrchestrator
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.BidragsberegningOrkestrator
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.HentLøpendeBidragService
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.OmgjøringOrkestrator
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.OmgjøringOrkestratorV2
import no.nav.bidrag.beregn.barnebidrag.testdata.opprettVedtakDtoForBidragsberegning
import no.nav.bidrag.beregn.barnebidrag.testdata.opprettVedtakForStønadBidragsberegning
import no.nav.bidrag.beregn.barnebidrag.utils.OmgjøringOrkestratorHelpers
import no.nav.bidrag.beregn.barnebidrag.utils.OmgjøringOrkestratorHelpersV2
import no.nav.bidrag.beregn.core.exception.IkkeFullBidragsevneOgUfullstendigeGrunnlagException
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.commons.web.mock.stubSjablonProvider
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.sak.Stønadsid
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.beregn.indeksregulering.BeregnIndeksreguleringApi
import no.nav.bidrag.transport.behandling.belopshistorikk.request.LøpendeBidragPeriodeRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.BidragPeriode
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidrag
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidragPeriodeResponse
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidragssak
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BidragsberegningOrkestratorRequestV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.Person
import no.nav.bidrag.transport.behandling.felles.grunnlag.PrivatAvtaleGrunnlagV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
internal class BidragsberegningOrkestratorTest : FellesTest() {
    private lateinit var filnavnBeregnGrunnlag: String

    @MockK(relaxed = true)
    private lateinit var identUtils: IdentUtils

    @MockK(relaxed = true)
    private lateinit var aldersjusteringOrchestrator: AldersjusteringOrchestrator

    @MockK(relaxed = true)
    private lateinit var beregnIndeksreguleringApi: BeregnIndeksreguleringApi

    @MockK(relaxed = true)
    private lateinit var vedtakService: VedtakService

    private lateinit var barnebidragApi: BeregnBarnebidragApi
    private lateinit var omgjøringOrkestrator: OmgjøringOrkestrator
    private lateinit var omgjøringOrkestratorV2: OmgjøringOrkestratorV2
    private lateinit var bidragsberegningOrkestrator: BidragsberegningOrkestrator
    private lateinit var hentLøpendeBidragService: HentLøpendeBidragService

    @MockK(relaxed = true)
    private lateinit var personConsumer: BeregningPersonConsumer

    @MockK(relaxed = true)
    private lateinit var sakConsumer: BeregningSakConsumer

    @BeforeEach
    fun init() {
        every { identUtils.hentNyesteIdent(any()) }.answers {
            val ident = firstArg<Personident>()
            ident
        }
        barnebidragApi = BeregnBarnebidragApi()
        val omgjøringOrkestratorHelpers = OmgjøringOrkestratorHelpers(vedtakService, identUtils)
        val omgjøringOrkestratorHelpersV2 = OmgjøringOrkestratorHelpersV2(vedtakService, identUtils)
        omgjøringOrkestrator =
            OmgjøringOrkestrator(vedtakService, aldersjusteringOrchestrator, beregnIndeksreguleringApi, omgjøringOrkestratorHelpers)
        omgjøringOrkestratorV2 =
            OmgjøringOrkestratorV2(vedtakService, aldersjusteringOrchestrator, beregnIndeksreguleringApi, omgjøringOrkestratorHelpersV2)
        hentLøpendeBidragService = HentLøpendeBidragService(vedtakService = vedtakService)
        bidragsberegningOrkestrator = BidragsberegningOrkestrator(
            barnebidragApi = barnebidragApi,
            omgjøringOrkestrator = omgjøringOrkestrator,
            omgjøringOrkestratorV2 = omgjøringOrkestratorV2,
            hentLøpendeBidragService = hentLøpendeBidragService,
            personConsumer = personConsumer,
            sakConsumer = sakConsumer,
        )
        stubSjablonProvider()
    }

    @Disabled
    @Test
    fun `gi direkte avslag`() {
        filnavnBeregnGrunnlag = "src/test/resources/testfiler/bidragsberegning_orkestrator/test01_v3_direkte_avslag_bidrag_grunnlag.json"
        val beregnRequest = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(filnavnBeregnGrunnlag)

        val beregnResponse = bidragsberegningOrkestrator.utførBidragsberegningV3(beregnRequest)
        println(commonObjectmapper.writeValueAsString(beregnResponse))

        assertSoftly(beregnResponse) {
            grunnlagListe shouldHaveAtLeastSize 1
            resultat shouldHaveSize 2
            resultat.all { resultatVedtak ->
                resultatVedtak.resultatVedtakListe.all { vedtak ->
                    vedtak.periodeListe.shouldHaveSize(1)
                    vedtak.periodeListe.all { periode -> periode.resultat.beløp == null }
                }
            } shouldBe true
        }
    }

    @Test
    fun `beregn bidrag v3 - 1 BM, 2 søknadsbarn - ingen løpende stønader`() {
        filnavnBeregnGrunnlag = "src/test/resources/testfiler/bidragsberegning_orkestrator/test01_v3_beregn_bidrag_grunnlag.json"
        val beregnRequest = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(filnavnBeregnGrunnlag)

        val beregnResponse = bidragsberegningOrkestrator.utførBidragsberegningV3(beregnRequest)
        println(commonObjectmapper.writeValueAsString(beregnResponse))

        assertSoftly(beregnResponse) {
            grunnlagListe shouldHaveAtLeastSize 1
            resultat shouldHaveAtLeastSize 1
        }
    }

    @Test
    fun `beregn bidrag v3 - 1 BM, 2 søknadsbarn med samme BM - privat avtale annet barn med samme BM`() {
        filnavnBeregnGrunnlag = "src/test/resources/testfiler/bidragsberegning_orkestrator/test01_v3_beregn_bidrag_grunnlag_privat_avtale_1.json"
        val beregnRequest = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(filnavnBeregnGrunnlag)

        val beregnResponse = bidragsberegningOrkestrator.utførBidragsberegningV3(beregnRequest)
        println(commonObjectmapper.writeValueAsString(beregnResponse))

        assertSoftly(beregnResponse) {
            // Verifiser at grunnlagListe inneholder forventede grunnlagstyper
            grunnlagListe shouldHaveAtLeastSize 1
            grunnlagListe.map { it.type } shouldContainAll listOf(
                Grunnlagstype.PERSON_SØKNADSBARN,
                Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                Grunnlagstype.PERSON_BIDRAGSPLIKTIG,
            )

            // Verifiser at det er beregningsresultat for begge søknadsbarn
            resultat shouldHaveSize 2
            resultat.map { it.søknadsbarnreferanse } shouldContainAll listOf(
                "Person_Søknadsbarn_01",
                "Person_Søknadsbarn_02",
            )

            // Verifiser at alle søknadsbarn har resultatvedtak med perioder og ingen beregningsfeil
            resultat.forEach { resultatBarn ->
                resultatBarn.resultatVedtakListe shouldHaveSize 1
                resultatBarn.resultatVedtakListe.first().also { vedtak ->
                    vedtak.periodeListe shouldHaveSize 2
                    vedtak.delvedtak shouldBe false
                    vedtak.omgjøringsvedtak shouldBe false
                    vedtak.periodeListe.forEach { periode ->
                        periode.resultat.beløp shouldNotBe null
                    }
                }
            }

            // Verifiser spesifikke beløp for søknadsbarn 1
            val resultatSøknadsbarn01 = resultat.first { it.søknadsbarnreferanse == "Person_Søknadsbarn_01" }
            resultatSøknadsbarn01.resultatVedtakListe.first().periodeListe.let { perioder ->
                perioder[0].periode.fom shouldBe YearMonth.of(2024, 1)
                perioder[0].periode.til shouldBe YearMonth.of(2024, 7)
                perioder[0].resultat.beløp shouldBe BigDecimal(5920)
                perioder[1].periode.fom shouldBe YearMonth.of(2024, 7)
                perioder[1].periode.til shouldBe null
                perioder[1].resultat.beløp shouldBe BigDecimal(6070)
            }

            // Verifiser spesifikke beløp for søknadsbarn 2
            val resultatSøknadsbarn02 = resultat.first { it.søknadsbarnreferanse == "Person_Søknadsbarn_02" }
            resultatSøknadsbarn02.resultatVedtakListe.first().periodeListe.let { perioder ->
                perioder[0].periode.fom shouldBe YearMonth.of(2024, 1)
                perioder[0].periode.til shouldBe YearMonth.of(2024, 7)
                perioder[0].resultat.beløp shouldBe BigDecimal(3750)
                perioder[1].periode.fom shouldBe YearMonth.of(2024, 7)
                perioder[1].periode.til shouldBe null
                perioder[1].resultat.beløp shouldBe BigDecimal(5050)
            }

            // Verifiser at privat avtale grunnlag for annet barn med samme BM er inkludert
            grunnlagListe.any { it.type == Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG } shouldBe true
            grunnlagListe.any { it.type == Grunnlagstype.PRIVAT_AVTALE_PERIODE_GRUNNLAG } shouldBe true

            // Verifiser at privat avtale grunnlag gjelder riktig barn (Person_Barn_Bidragspliktig_01 med BM Person_Bidragsmottaker_01)
            val privatAvtaleGrunnlag = grunnlagListe.filter { it.type == Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG }
            privatAvtaleGrunnlag shouldHaveSize 1
            privatAvtaleGrunnlag.first().gjelderBarnReferanse shouldBe "Person_Barn_Bidragspliktig_01"
            privatAvtaleGrunnlag.first().gjelderReferanse shouldBe "Person_Bidragsmottaker_01"

            // Verifiser at delberegning for delberegning bidrag til fordeling privat avtale er inkludert
            val delberegningBidragTilFordelingPrivatAvtale =
                grunnlagListe.filter { it.type == Grunnlagstype.DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE }
            delberegningBidragTilFordelingPrivatAvtale shouldHaveSize 2
            delberegningBidragTilFordelingPrivatAvtale.first().gjelderBarnReferanse shouldBe "Person_Barn_Bidragspliktig_01"
        }
    }

    @Test
    fun `beregn bidrag v3 - 1 BM, 2 søknadsbarn med samme BM - privat avtale annet barn med annen BM`() {
        filnavnBeregnGrunnlag = "src/test/resources/testfiler/bidragsberegning_orkestrator/test01_v3_beregn_bidrag_grunnlag_privat_avtale_2.json"
        val beregnRequest = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(filnavnBeregnGrunnlag)

        val beregnResponse = bidragsberegningOrkestrator.utførBidragsberegningV3(beregnRequest)
        println(commonObjectmapper.writeValueAsString(beregnResponse))

        assertSoftly(beregnResponse) {
            // Verifiser at grunnlagListe inneholder forventede grunnlagstyper
            grunnlagListe shouldHaveAtLeastSize 1
            grunnlagListe.map { it.type } shouldContainAll listOf(
                Grunnlagstype.PERSON_SØKNADSBARN,
                Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                Grunnlagstype.PERSON_BIDRAGSPLIKTIG,
                Grunnlagstype.PERSON_BARN_BIDRAGSPLIKTIG,
            )

            // Verifiser at det finnes to bidragsmottakere (BM1 for søknadsbarn, BM2 for privat avtale barnet)
            val bidragsmottakere = grunnlagListe.filter { it.type == Grunnlagstype.PERSON_BIDRAGSMOTTAKER }
            bidragsmottakere shouldHaveSize 2
            bidragsmottakere.map { it.referanse } shouldContainAll listOf(
                "Person_Bidragsmottaker_01",
                "Person_Bidragsmottaker_02",
            )

            // Verifiser at det er beregningsresultat for begge søknadsbarn
            resultat shouldHaveSize 2
            resultat.map { it.søknadsbarnreferanse } shouldContainAll listOf(
                "Person_Søknadsbarn_01",
                "Person_Søknadsbarn_02",
            )

            // Verifiser at alle søknadsbarn har resultatvedtak med perioder og ingen beregningsfeil
            resultat.forEach { resultatBarn ->
                resultatBarn.resultatVedtakListe shouldHaveSize 1
                resultatBarn.resultatVedtakListe.first().also { vedtak ->
                    vedtak.periodeListe shouldHaveSize 2
                    vedtak.delvedtak shouldBe false
                    vedtak.omgjøringsvedtak shouldBe false
                    vedtak.periodeListe.forEach { periode ->
                        periode.resultat.beløp shouldNotBe null
                    }
                }
            }

            // Verifiser spesifikke beløp for søknadsbarn 1
            val resultatSøknadsbarn01 = resultat.first { it.søknadsbarnreferanse == "Person_Søknadsbarn_01" }
            resultatSøknadsbarn01.resultatVedtakListe.first().periodeListe.let { perioder ->
                perioder[0].periode.fom shouldBe YearMonth.of(2024, 1)
                perioder[0].periode.til shouldBe YearMonth.of(2024, 7)
                perioder[0].resultat.beløp shouldBe BigDecimal(5920)
                perioder[1].periode.fom shouldBe YearMonth.of(2024, 7)
                perioder[1].periode.til shouldBe null
                perioder[1].resultat.beløp shouldBe BigDecimal(6070)
            }

            // Verifiser spesifikke beløp for søknadsbarn 2
            val resultatSøknadsbarn02 = resultat.first { it.søknadsbarnreferanse == "Person_Søknadsbarn_02" }
            resultatSøknadsbarn02.resultatVedtakListe.first().periodeListe.let { perioder ->
                perioder[0].periode.fom shouldBe YearMonth.of(2024, 1)
                perioder[0].periode.til shouldBe YearMonth.of(2024, 7)
                perioder[0].resultat.beløp shouldBe BigDecimal(3750)
                perioder[1].periode.fom shouldBe YearMonth.of(2024, 7)
                perioder[1].periode.til shouldBe null
                perioder[1].resultat.beløp shouldBe BigDecimal(5050)
            }

            // Verifiser at privat avtale grunnlag for annet barn med annen BM er inkludert
            grunnlagListe.any { it.type == Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG } shouldBe true
            grunnlagListe.any { it.type == Grunnlagstype.PRIVAT_AVTALE_PERIODE_GRUNNLAG } shouldBe true

            // Verifiser at privat avtale grunnlag gjelder Person_Barn_Bidragspliktig_01 (ikke et søknadsbarn) med Person_Bidragsmottaker_02
            // (annen BM)
            val privatAvtaleGrunnlag = grunnlagListe.filter { it.type == Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG }
            privatAvtaleGrunnlag shouldHaveSize 1
            privatAvtaleGrunnlag.first().gjelderBarnReferanse shouldBe "Person_Barn_Bidragspliktig_01"
            privatAvtaleGrunnlag.first().gjelderReferanse shouldBe "Person_Bidragsmottaker_02"

            // Verifiser at delberegning bidrag til fordeling privat avtale er inkludert for Person_Barn_Bidragspliktig_01
            val delberegningBidragTilFordelingPrivatAvtale =
                grunnlagListe.filter { it.type == Grunnlagstype.DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE }
            delberegningBidragTilFordelingPrivatAvtale shouldHaveSize 2
            delberegningBidragTilFordelingPrivatAvtale.first().gjelderBarnReferanse shouldBe "Person_Barn_Bidragspliktig_01"

            // Verifiser at Person_Barn_Bidragspliktig_01 (annet barn) finnes i grunnlagslisten
            val barnBidragspliktig = grunnlagListe.filter { it.type == Grunnlagstype.PERSON_BARN_BIDRAGSPLIKTIG }
            barnBidragspliktig shouldHaveSize 1
            barnBidragspliktig.first().referanse shouldBe "Person_Barn_Bidragspliktig_01"
        }
    }

    @Test
    fun `beregn bidrag v3 - 1 BM, 2 søknadsbarn med forskjellig BM - privat avtale SB2 full overlapp`() {
        filnavnBeregnGrunnlag = "src/test/resources/testfiler/bidragsberegning_orkestrator/test01_v3_beregn_bidrag_grunnlag_privat_avtale_4.json"
        val beregnRequest = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(filnavnBeregnGrunnlag)

        val beregnResponse = bidragsberegningOrkestrator.utførBidragsberegningV3(beregnRequest)
        println(commonObjectmapper.writeValueAsString(beregnResponse))

        assertSoftly(beregnResponse) {
            // Grunnlagstyper
            grunnlagListe shouldHaveAtLeastSize 1
            grunnlagListe.map { it.type } shouldContainAll listOf(
                Grunnlagstype.PERSON_SØKNADSBARN,
                Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                Grunnlagstype.PERSON_BIDRAGSPLIKTIG,
            )

            // To bidragsmottakere
            val bidragsmottakere = grunnlagListe.filter { it.type == Grunnlagstype.PERSON_BIDRAGSMOTTAKER }
            bidragsmottakere shouldHaveSize 2
            bidragsmottakere.map { it.referanse } shouldContainAll listOf(
                "Person_Bidragsmottaker_01",
                "Person_Bidragsmottaker_02",
            )

            // Resultat for begge søknadsbarn
            resultat shouldHaveSize 2
            resultat.map { it.søknadsbarnreferanse } shouldContainAll listOf(
                "Person_Søknadsbarn_01",
                "Person_Søknadsbarn_02",
            )

            // Resultat søknadsbarn 1
            val resultatSøknadsbarn01 = resultat.first { it.søknadsbarnreferanse == "Person_Søknadsbarn_01" }
            resultatSøknadsbarn01.resultatVedtakListe.first().periodeListe.let { perioder ->
                perioder[0].periode.fom shouldBe YearMonth.of(2024, 1)
                perioder[0].periode.til shouldBe YearMonth.of(2024, 7)
                perioder[0].resultat.beløp shouldBe BigDecimal(5920)
                perioder[1].periode.fom shouldBe YearMonth.of(2024, 7)
                perioder[1].periode.til shouldBe null
                perioder[1].resultat.beløp shouldBe BigDecimal(6070)
            }

            // Resultat søknadsbarn 2
            val resultatSøknadsbarn02 = resultat.first { it.søknadsbarnreferanse == "Person_Søknadsbarn_02" }
            resultatSøknadsbarn02.resultatVedtakListe.first().periodeListe.let { perioder ->
                perioder[0].periode.fom shouldBe YearMonth.of(2024, 1)
                perioder[0].periode.til shouldBe YearMonth.of(2024, 7)
                perioder[0].resultat.beløp shouldBe BigDecimal(4140)
                perioder[1].periode.fom shouldBe YearMonth.of(2024, 7)
                perioder[1].periode.til shouldBe null
                perioder[1].resultat.beløp shouldBe BigDecimal(5590)
            }
        }
    }

    @Test
    fun `beregn bidrag v3 - 1 BM, 2 søknadsbarn med forskjellig BM - privat avtale SB2 delvis overlapp - med samværsklasse`() {
        filnavnBeregnGrunnlag = "src/test/resources/testfiler/bidragsberegning_orkestrator/test01_v3_beregn_bidrag_grunnlag_privat_avtale_5.json"
        val beregnRequest = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(filnavnBeregnGrunnlag)

        val beregnResponse = bidragsberegningOrkestrator.utførBidragsberegningV3(beregnRequest)
        println(commonObjectmapper.writeValueAsString(beregnResponse))

        // Sjekk at alle referanser er med i resultatet
        val alleReferanser = hentAlleReferanser(beregnResponse.grunnlagListe)
        val alleRefererteReferanser = hentAlleRefererteReferanser(beregnResponse.grunnlagListe)

        assertSoftly(beregnResponse) {
            // Grunnlagstyper
            grunnlagListe shouldHaveAtLeastSize 1
            grunnlagListe.map { it.type } shouldContainAll listOf(
                Grunnlagstype.PERSON_SØKNADSBARN,
                Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                Grunnlagstype.PERSON_BIDRAGSPLIKTIG,
            )

            // To bidragsmottakere
            val bidragsmottakere = grunnlagListe.filter { it.type == Grunnlagstype.PERSON_BIDRAGSMOTTAKER }
            bidragsmottakere shouldHaveSize 2
            bidragsmottakere.map { it.referanse } shouldContainAll listOf(
                "Person_Bidragsmottaker_01",
                "Person_Bidragsmottaker_02",
            )

            // Resultat for begge søknadsbarn
            resultat shouldHaveSize 2
            resultat.map { it.søknadsbarnreferanse } shouldContainAll listOf(
                "Person_Søknadsbarn_01",
                "Person_Søknadsbarn_02",
            )

            // Resultat søknadsbarn 1
            val resultatSøknadsbarn01 = resultat.first { it.søknadsbarnreferanse == "Person_Søknadsbarn_01" }
            resultatSøknadsbarn01.resultatVedtakListe.first().periodeListe.let { perioder ->
                perioder[0].periode.fom shouldBe YearMonth.of(2024, 1)
                perioder[0].periode.til shouldBe YearMonth.of(2024, 4)
                perioder[0].resultat.beløp shouldBe BigDecimal(5920)
                perioder[0].grunnlagsreferanseListe shouldHaveSize 1
                perioder[0].grunnlagsreferanseListe.any { it.contains("SLUTTBEREGNING") }
                perioder[1].periode.fom shouldBe YearMonth.of(2024, 4)
                perioder[1].periode.til shouldBe YearMonth.of(2024, 7)
                perioder[1].resultat.beløp shouldBe BigDecimal(5920)
                perioder[1].grunnlagsreferanseListe shouldHaveSize 1
                perioder[1].grunnlagsreferanseListe.any { it.contains("SLUTTBEREGNING") }
                perioder[2].periode.fom shouldBe YearMonth.of(2024, 7)
                perioder[2].periode.til shouldBe null
                perioder[2].resultat.beløp shouldBe BigDecimal(6070)
                perioder[2].grunnlagsreferanseListe shouldHaveSize 1
                perioder[2].grunnlagsreferanseListe.any { it.contains("SLUTTBEREGNING") }
            }

            // Resultat søknadsbarn 2
            val resultatSøknadsbarn02 = resultat.first { it.søknadsbarnreferanse == "Person_Søknadsbarn_02" }
            resultatSøknadsbarn02.resultatVedtakListe.first().periodeListe.let { perioder ->
                perioder[0].periode.fom shouldBe YearMonth.of(2024, 4)
                perioder[0].periode.til shouldBe YearMonth.of(2024, 7)
                perioder[0].resultat.beløp shouldBe BigDecimal(4140)
                perioder[0].grunnlagsreferanseListe shouldHaveSize 2
                perioder[0].grunnlagsreferanseListe.any { it.contains("SLUTTBEREGNING") }
                perioder[0].grunnlagsreferanseListe.any { it.contains("INDEKSREGULERING") }
                perioder[1].periode.fom shouldBe YearMonth.of(2024, 7)
                perioder[1].periode.til shouldBe null
                perioder[1].resultat.beløp shouldBe BigDecimal(5590)
                perioder[1].grunnlagsreferanseListe shouldHaveSize 2
                perioder[1].grunnlagsreferanseListe.any { it.contains("SLUTTBEREGNING") }
                perioder[1].grunnlagsreferanseListe.any { it.contains("INDEKSREGULERING") }
            }

            // Sjekk at alle referanser blir refererert og at alle refererte referanser blir definert
            alleReferanser shouldContainAll alleRefererteReferanser
            alleRefererteReferanser shouldContainAll alleReferanser
                .filterNot { it.contains("delberegning_DELBEREGNING_ENDRING_SJEKK_GRENSE_Person") }
                .filterNot { it.contains("delberegning_DELBEREGNING_ENDRING_SJEKK_GRENSE_person") }
                .filterNot { it.contains("delberegning_DELBEREGNING_ENDRING_SJEKK_GRENSE_PERSON") }
                .filterNot { it.contains("delberegning_DELBEREGNING_FATTE_VEDTAK") }
        }
    }

    @Test
    fun `beregn bidrag v3 - 1 BM, 1 søknadsbarn - 1 løpende stønad i bidrag-behandling med annen BM`() {
        filnavnBeregnGrunnlag = "src/test/resources/testfiler/bidragsberegning_orkestrator/test02_v3_beregn_bidrag_grunnlag.json"
        val beregnRequest = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(filnavnBeregnGrunnlag)

        every { vedtakService.hentSisteLøpendeStønader(any()) }.answers {
            listOf(
                LøpendeBidragssak(
                    sak = Saksnummer(SAK_LØPENDE_BIDRAG),
                    type = Stønadstype.BIDRAG,
                    kravhaver = Personident(KRAVHAVER_LØPENDE_BIDRAG),
                    løpendeBeløp = LØPENDE_BELØP,
                ),
            )
        }

        every { vedtakService.hentAlleStønaderForBidragspliktig(any<LøpendeBidragPeriodeRequest>()) } answers {
            LøpendeBidragPeriodeResponse(
                listOf(
                    LøpendeBidrag(
                        sak = Saksnummer(SAK_LØPENDE_BIDRAG),
                        type = Stønadstype.BIDRAG,
                        kravhaver = Personident(KRAVHAVER_LØPENDE_BIDRAG),
                        mottaker = Personident(MOTTAKER),
                        periodeListe = listOf(
                            BidragPeriode(
                                periode = ÅrMånedsperiode(fom = YearMonth.parse("2025-01"), til = YearMonth.parse("2025-10")),
                                løpendeBeløp = BigDecimal.valueOf(1000),
                            ),
                        ),
                    ),
                ),
            )
        }

        every { vedtakService.finnAlleManuelleVedtakForEvnevurdering(any<Stønadsid>()) }.answers {
            listOf(
                opprettVedtakForStønadBidragsberegning(
                    skyldner = SKYLDNER,
                    kravhaver = KRAVHAVER_LØPENDE_BIDRAG,
                    mottaker = MOTTAKER,
                    sak = SAK_LØPENDE_BIDRAG,
                    beregnetBeløp = LØPENDE_BELØP,
                ),
            )
        }

        every { vedtakService.hentVedtak(any()) }.answers {
            opprettVedtakDtoForBidragsberegning(
                skyldner = SKYLDNER,
                kravhaver = KRAVHAVER_LØPENDE_BIDRAG,
                mottaker = MOTTAKER,
                sak = SAK_LØPENDE_BIDRAG,
                beregnetBeløp = LØPENDE_BELØP,
            )
        }

        every { personConsumer.hentFødselsdatoForPerson(any()) }.answers {
            LocalDate.parse("2020-01-01")
        }

        val beregnResponse = bidragsberegningOrkestrator.utførBidragsberegningV3(beregnRequest)
        println(commonObjectmapper.writeValueAsString(beregnResponse))

        assertSoftly(beregnResponse) {
            grunnlagListe shouldHaveAtLeastSize 1
            resultat shouldHaveAtLeastSize 1
        }
    }

    @Test
    fun `beregn bidrag v3 - 1 BM, 2 søknadsbarn med samme BM - privat avtale utland annet barn med annen BM`() {
        filnavnBeregnGrunnlag = "src/test/resources/testfiler/bidragsberegning_orkestrator/test01_v3_beregn_bidrag_grunnlag_privat_avtale_utland.json"
        val beregnRequest = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(filnavnBeregnGrunnlag)

        val beregnResponse = bidragsberegningOrkestrator.utførBidragsberegningV3(beregnRequest)
        println(commonObjectmapper.writeValueAsString(beregnResponse))

        assertSoftly(beregnResponse) {
            // Verifiser at grunnlagListe inneholder forventede grunnlagstyper
            grunnlagListe shouldHaveAtLeastSize 1
            grunnlagListe.map { it.type } shouldContainAll listOf(
                Grunnlagstype.PERSON_SØKNADSBARN,
                Grunnlagstype.PERSON_BIDRAGSMOTTAKER,
                Grunnlagstype.PERSON_BIDRAGSPLIKTIG,
                Grunnlagstype.PERSON_BARN_BIDRAGSPLIKTIG,
            )

            // Verifiser at det finnes to bidragsmottakere (BM1 for søknadsbarn, BM2 for privat avtale barnet)
            val bidragsmottakere = grunnlagListe.filter { it.type == Grunnlagstype.PERSON_BIDRAGSMOTTAKER }
            bidragsmottakere shouldHaveSize 2
            bidragsmottakere.map { it.referanse } shouldContainAll listOf(
                "Person_Bidragsmottaker_01",
                "Person_Bidragsmottaker_02",
            )

            // Verifiser at det er beregningsresultat for begge søknadsbarn
            resultat shouldHaveSize 2
            resultat.map { it.søknadsbarnreferanse } shouldContainAll listOf(
                "Person_Søknadsbarn_01",
                "Person_Søknadsbarn_02",
            )

            // Verifiser at alle søknadsbarn har resultatvedtak med perioder og ingen beregningsfeil
            resultat.forEach { resultatBarn ->
                resultatBarn.resultatVedtakListe shouldHaveSize 1
                resultatBarn.resultatVedtakListe.first().also { vedtak ->
                    vedtak.periodeListe shouldHaveSize 2
                    vedtak.delvedtak shouldBe false
                    vedtak.omgjøringsvedtak shouldBe false
                    vedtak.periodeListe.forEach { periode ->
                        periode.resultat.beløp shouldNotBe null
                    }
                }
            }

            // Verifiser spesifikke beløp for søknadsbarn 1
            val resultatSøknadsbarn01 = resultat.first { it.søknadsbarnreferanse == "Person_Søknadsbarn_01" }
            resultatSøknadsbarn01.resultatVedtakListe.first().periodeListe.let { perioder ->
                perioder[0].periode.fom shouldBe YearMonth.of(2024, 1)
                perioder[0].periode.til shouldBe YearMonth.of(2024, 7)
                perioder[0].resultat.beløp shouldBe BigDecimal(5920)
                perioder[1].periode.fom shouldBe YearMonth.of(2024, 7)
                perioder[1].periode.til shouldBe null
                perioder[1].resultat.beløp shouldBe BigDecimal(6040)
            }

            // Verifiser spesifikke beløp for søknadsbarn 2
            val resultatSøknadsbarn02 = resultat.first { it.søknadsbarnreferanse == "Person_Søknadsbarn_02" }
            resultatSøknadsbarn02.resultatVedtakListe.first().periodeListe.let { perioder ->
                perioder[0].periode.fom shouldBe YearMonth.of(2024, 1)
                perioder[0].periode.til shouldBe YearMonth.of(2024, 7)
                perioder[0].resultat.beløp shouldBe BigDecimal(3750)
                perioder[1].periode.fom shouldBe YearMonth.of(2024, 7)
                perioder[1].periode.til shouldBe null
                perioder[1].resultat.beløp shouldBe BigDecimal(5010)
            }

            // Verifiser at privat avtale grunnlag for annet barn med annen BM er inkludert
            grunnlagListe.any { it.type == Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG } shouldBe true
            grunnlagListe.any { it.type == Grunnlagstype.PRIVAT_AVTALE_PERIODE_GRUNNLAG } shouldBe true

            // Verifiser at privat avtale grunnlag gjelder Person_Barn_Bidragspliktig_01 (ikke et søknadsbarn) med Person_Bidragsmottaker_02
            // (annen BM)
            val privatAvtaleGrunnlag = grunnlagListe.filter { it.type == Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG }
            privatAvtaleGrunnlag shouldHaveSize 1
            privatAvtaleGrunnlag.first().gjelderBarnReferanse shouldBe "Person_Barn_Bidragspliktig_01"
            privatAvtaleGrunnlag.first().gjelderReferanse shouldBe "Person_Bidragsmottaker_02"

            // Verifiser at delberegning bidrag til fordeling privat avtale er inkludert for Person_Barn_Bidragspliktig_01
            val delberegningBidragTilFordelingPrivatAvtale =
                grunnlagListe.filter { it.type == Grunnlagstype.DELBEREGNING_BIDRAG_TIL_FORDELING_PRIVAT_AVTALE }
            delberegningBidragTilFordelingPrivatAvtale shouldHaveSize 2
            delberegningBidragTilFordelingPrivatAvtale.first().gjelderBarnReferanse shouldBe "Person_Barn_Bidragspliktig_01"

            // Verifiser at Person_Barn_Bidragspliktig_01 (annet barn) finnes i grunnlagslisten
            val barnBidragspliktig = grunnlagListe.filter { it.type == Grunnlagstype.PERSON_BARN_BIDRAGSPLIKTIG }
            barnBidragspliktig shouldHaveSize 1
            barnBidragspliktig.first().referanse shouldBe "Person_Barn_Bidragspliktig_01"
        }
    }

    @Test
    fun `finn privat avtale grunnlag - privat avtale under og over 18 år for samme person - ikke søknadsbarn`() {
        filnavnBeregnGrunnlag =
            "src/test/resources/testfiler/bidragsberegning_orkestrator/test01_v3_privat_avtale_over_og_under_18_samme_person_ikke_søknadsbarn.json"
        val beregnRequest = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(filnavnBeregnGrunnlag)

        val beregnGrunnlagListe = bidragsberegningOrkestrator.finnPrivatAvtaleGrunnlag(
            grunnlagSøknadsbarnListe = with(bidragsberegningOrkestrator) { beregnRequest.tilListeBeregnGrunnlagV1() },
            request = beregnRequest,
            totalBeregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 1), YearMonth.of(2026, 3)),
        )

        assertSoftly {
            // Verifiser at resultatet ikke er tomt og har 2 elementer
            beregnGrunnlagListe shouldNotBe null
            beregnGrunnlagListe shouldHaveSize 2

            val periodeFomListe = listOf(YearMonth.of(2024, 1), YearMonth.of(2025, 5))
            val periodeTilListe = listOf(YearMonth.of(2025, 5), YearMonth.of(2026, 3))
            val stønadstypeListe = listOf(Stønadstype.BIDRAG, Stønadstype.BIDRAG18AAR)
            var indeks = 0

            beregnGrunnlagListe.forEach {
                it.periode.fom shouldBe periodeFomListe[indeks]
                it.periode.til shouldBe periodeTilListe[indeks]
                it.stønadstype shouldBe stønadstypeListe[indeks]

                // Verifiser grunnlagstyper
                it.grunnlagListe.count { grunnlag -> grunnlag.type == Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG } shouldBe 1
                it.grunnlagListe.count { grunnlag -> grunnlag.type == Grunnlagstype.PRIVAT_AVTALE_PERIODE_GRUNNLAG } shouldBe 1
                it.grunnlagListe.count { grunnlag -> grunnlag.type == Grunnlagstype.PERSON_BIDRAGSPLIKTIG } shouldBe 1
                it.grunnlagListe.count { grunnlag -> grunnlag.type == Grunnlagstype.PERSON_BARN_BIDRAGSPLIKTIG } shouldBe 1

                // Hent innhold i PRIVAT_AVTALE_GRUNNLAG
                val privatAvtaleGrunnlagInnhold = it.grunnlagListe
                    .filtrerOgKonverterBasertPåEgenReferanse<PrivatAvtaleGrunnlagV2>(
                        Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG,
                    )
                    .first()
                    .innhold

                // Verifiser at stønadstype i PRIVAT_AVTALE_GRUNNLAG samsvarer med stønadstype i BeregnGrunnlag
                privatAvtaleGrunnlagInnhold.stønadstype shouldBe it.stønadstype

                // Verifiser at PRIVAT_AVTALE_PERIODE_GRUNNLAG referanse ligger i referanselista til PRIVAT_AVTALE_GRUNNLAG
                it.grunnlagListe.first { grunnlag -> grunnlag.type == Grunnlagstype.PRIVAT_AVTALE_PERIODE_GRUNNLAG }.referanse shouldBeIn
                    it.grunnlagListe.first { grunnlag -> grunnlag.type == Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG }.grunnlagsreferanseListe

                indeks++
            }
        }
    }

    @Test
    fun `finn privat avtale grunnlag - privat avtale under og over 18 år for samme person - revurderingsbarn`() {
        filnavnBeregnGrunnlag =
            "src/test/resources/testfiler/bidragsberegning_orkestrator/test01_v3_privat_avtale_over_og_under_18_samme_person_revurderingsbarn.json"
        val beregnRequest = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(filnavnBeregnGrunnlag)

        val beregnGrunnlagListe = bidragsberegningOrkestrator.finnPrivatAvtaleGrunnlag(
            grunnlagSøknadsbarnListe = with(bidragsberegningOrkestrator) { beregnRequest.tilListeBeregnGrunnlagV1() },
            request = beregnRequest,
            totalBeregningsperiode = ÅrMånedsperiode(YearMonth.of(2024, 1), YearMonth.of(2026, 3)),
        )

        assertSoftly {
            // Verifiser at resultatet ikke er tomt og har 2 elementer
            beregnGrunnlagListe shouldNotBe null
            beregnGrunnlagListe shouldHaveSize 2

            val periodeFomListe = listOf(YearMonth.of(2024, 1), YearMonth.of(2024, 1))
            val periodeTilListe = listOf(YearMonth.of(2025, 2), YearMonth.of(2025, 9))
            val stønadstypeListe = listOf(Stønadstype.BIDRAG, Stønadstype.BIDRAG18AAR)
            var indeks = 0

            beregnGrunnlagListe.forEach {
                it.periode.fom shouldBe periodeFomListe[indeks]
                it.periode.til shouldBe periodeTilListe[indeks]
                it.stønadstype shouldBe stønadstypeListe[indeks]

                // Verifiser grunnlagstyper
                it.grunnlagListe.count { grunnlag -> grunnlag.type == Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG } shouldBe 1
                it.grunnlagListe.count { grunnlag -> grunnlag.type == Grunnlagstype.PRIVAT_AVTALE_PERIODE_GRUNNLAG } shouldBe 1
                it.grunnlagListe.count { grunnlag -> grunnlag.type == Grunnlagstype.PERSON_BIDRAGSPLIKTIG } shouldBe 1
                it.grunnlagListe.count { grunnlag -> grunnlag.type == Grunnlagstype.PERSON_BIDRAGSMOTTAKER } shouldBe 1
                it.grunnlagListe.count { grunnlag -> grunnlag.type == Grunnlagstype.PERSON_SØKNADSBARN } shouldBe 1

                // Hent innhold i PRIVAT_AVTALE_GRUNNLAG
                val privatAvtaleGrunnlagInnhold = it.grunnlagListe
                    .filtrerOgKonverterBasertPåEgenReferanse<PrivatAvtaleGrunnlagV2>(
                        Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG,
                    )
                    .first()
                    .innhold

                // Verifiser at stønadstype i PRIVAT_AVTALE_GRUNNLAG samsvarer med stønadstype i BeregnGrunnlag
                privatAvtaleGrunnlagInnhold.stønadstype shouldBe it.stønadstype

                // Verifiser at PRIVAT_AVTALE_PERIODE_GRUNNLAG referanse ligger i referanselista til PRIVAT_AVTALE_GRUNNLAG
                it.grunnlagListe.first { grunnlag -> grunnlag.type == Grunnlagstype.PRIVAT_AVTALE_PERIODE_GRUNNLAG }.referanse shouldBeIn
                    it.grunnlagListe.first { grunnlag -> grunnlag.type == Grunnlagstype.PRIVAT_AVTALE_GRUNNLAG }.grunnlagsreferanseListe

                indeks++
            }
        }
    }

    // Beregning runde 1
    // 1 søknadsbarn i requesten
    // 1 annet barn som har løpende bidrag
    // Det er evnesprekk i minst en periode
    // Det skal da kastes exception for å utløse forholdsmessig fordeling og innhente nye grunnlag for det andre barnet
    @Test
    fun `beregn bidrag v3 - beregning runde 1 - FF ikke utløst - test A`() {
        filnavnBeregnGrunnlag = "src/test/resources/testfiler/bidragsberegning_orkestrator/test03_v3_beregning_runde1_testA.json"
        val beregnRequest = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(filnavnBeregnGrunnlag)

        val mottakerLøpendeBidrag = beregnRequest.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_BIDRAGSMOTTAKER)
            .map { it.innhold.ident }
            .first()

        val skyldner = beregnRequest.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_BIDRAGSPLIKTIG)
            .map { it.innhold.ident }
            .first()

        every { vedtakService.hentSisteLøpendeStønader(any()) }.answers {
            listOf(
                LøpendeBidragssak(
                    sak = Saksnummer(SAK_LØPENDE_BIDRAG),
                    type = Stønadstype.BIDRAG,
                    kravhaver = Personident(KRAVHAVER_LØPENDE_BIDRAG),
                    løpendeBeløp = LØPENDE_BELØP,
                ),
            )
        }

        every { vedtakService.hentAlleStønaderForBidragspliktig(any<LøpendeBidragPeriodeRequest>()) } answers {
            LøpendeBidragPeriodeResponse(
                listOf(
                    LøpendeBidrag(
                        sak = Saksnummer(SAK_LØPENDE_BIDRAG),
                        type = Stønadstype.BIDRAG,
                        kravhaver = Personident(KRAVHAVER_LØPENDE_BIDRAG),
                        mottaker = mottakerLøpendeBidrag,
                        periodeListe = listOf(
                            BidragPeriode(
                                periode = ÅrMånedsperiode(fom = YearMonth.parse("2025-01"), til = YearMonth.parse("2025-10")),
                                løpendeBeløp = BigDecimal.valueOf(1000),
                            ),
                        ),
                    ),
                ),
            )
        }

        every { vedtakService.finnAlleManuelleVedtakForEvnevurdering(any<Stønadsid>()) }.answers {
            listOf(
                opprettVedtakForStønadBidragsberegning(
                    skyldner = skyldner!!.verdi,
                    kravhaver = KRAVHAVER_LØPENDE_BIDRAG,
                    mottaker = mottakerLøpendeBidrag!!.verdi,
                    sak = SAK_LØPENDE_BIDRAG,
                    beregnetBeløp = LØPENDE_BELØP,
                ),
            )
        }

        every { vedtakService.hentVedtak(any()) }.answers {
            opprettVedtakDtoForBidragsberegning(
                skyldner = skyldner!!.verdi,
                kravhaver = KRAVHAVER_LØPENDE_BIDRAG,
                mottaker = mottakerLøpendeBidrag!!.verdi,
                sak = SAK_LØPENDE_BIDRAG,
                beregnetBeløp = LØPENDE_BELØP,
            )
        }

        every { personConsumer.hentFødselsdatoForPerson(Personident(KRAVHAVER_LØPENDE_BIDRAG)) }.answers {
            LocalDate.parse("2019-06-26")
        }

        every { personConsumer.hentFødselsdatoForPerson(mottakerLøpendeBidrag!!) }.answers {
            LocalDate.parse("1982-05-05")
        }

        val exception = assertThrows<IkkeFullBidragsevneOgUfullstendigeGrunnlagException> {
            bidragsberegningOrkestrator.utførBidragsberegningV3(beregnRequest)
        }

        assertSoftly(exception) {
            melding shouldBe "Det finnes perioder med evnesprekk. Nye grunnlag må hentes inn for løpende bidrag og/eller privat avtale før vedtak kan fattes."
            data.grunnlagListe shouldHaveAtLeastSize 1
            data.resultat shouldHaveSize 1
            data.resultat[0].søknadsbarnreferanse shouldBe "Person_Søknadsbarn_01"
            data.resultat[0].resultatVedtakListe shouldHaveSize 1
            data.resultat[0].resultatVedtakListe[0].periodeListe shouldHaveSize 2

            // Sjekker at det ikke er duplikate referanser
            val alleReferanser = data.grunnlagListe.map { it.referanse }
            alleReferanser.size shouldBe alleReferanser.distinct().size

            val alleRefererteReferanser = data.grunnlagListe
                .flatMap { it.grunnlagsreferanseListe + it.gjelderBarnReferanse + it.gjelderReferanse }
                .filterNotNull()
                .distinct()
            alleReferanser shouldContainAll alleRefererteReferanser
        }
    }

    // Forholdsmessig fordeling utløst i runde 1 av beregningen
    // 2 søknadsbarn i requesten
    // Søknadsbarn 1 er en del av opprinnelig behandling
    // Søknadsbarn 2 er ikke en del av opprinnelig behandling (aka revurderingsbarn)
    // Beregning runde 2A:
    // - Revurderingsbarn fjernes fra requesten
    // - Det innhentes løpende bidrag
    // - Beregning kjøres som i runde 1
    // - Full evne for søknadsbarn 1 mot løpende bidrag for søknadsbarn 2
    // - Beregningen avsluttes
    @Test
    fun `beregn bidrag v3 - beregning runde 2 - FF utløst - test B`() {
        filnavnBeregnGrunnlag = "src/test/resources/testfiler/bidragsberegning_orkestrator/test03_v3_beregning_runde2_testB.json"
        val beregnRequest = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(filnavnBeregnGrunnlag)

        val kravhaverLøpendeBidrag = beregnRequest.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_SØKNADSBARN)
            .filter { it.innhold.delAvOpprinneligBehandling == false }
            .map { it.innhold.ident }
            .first()

        val mottakerLøpendeBidrag = beregnRequest.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_BIDRAGSMOTTAKER)
            .map { it.innhold.ident }
            .first()

        val skyldner = beregnRequest.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_BIDRAGSPLIKTIG)
            .map { it.innhold.ident }
            .first()

        every { vedtakService.hentSisteLøpendeStønader(any()) }.answers {
            listOf(
                LøpendeBidragssak(
                    sak = Saksnummer(SAK_LØPENDE_BIDRAG),
                    type = Stønadstype.BIDRAG,
                    kravhaver = kravhaverLøpendeBidrag!!,
                    løpendeBeløp = LØPENDE_BELØP,
                ),
            )
        }

        every { vedtakService.hentAlleStønaderForBidragspliktig(any<LøpendeBidragPeriodeRequest>()) } answers {
            LøpendeBidragPeriodeResponse(
                listOf(
                    LøpendeBidrag(
                        sak = Saksnummer(SAK_LØPENDE_BIDRAG),
                        type = Stønadstype.BIDRAG,
                        kravhaver = kravhaverLøpendeBidrag!!,
                        mottaker = mottakerLøpendeBidrag,
                        periodeListe = listOf(
                            BidragPeriode(
                                periode = ÅrMånedsperiode(fom = YearMonth.parse("2025-01"), til = YearMonth.parse("2025-10")),
                                løpendeBeløp = BigDecimal.valueOf(1000),
                            ),
                        ),
                    ),
                ),
            )
        }

        every { vedtakService.finnAlleManuelleVedtakForEvnevurdering(any<Stønadsid>()) }.answers {
            listOf(
                opprettVedtakForStønadBidragsberegning(
                    skyldner = skyldner!!.verdi,
                    kravhaver = kravhaverLøpendeBidrag!!.verdi,
                    mottaker = mottakerLøpendeBidrag!!.verdi,
                    sak = SAK_LØPENDE_BIDRAG,
                    beregnetBeløp = LØPENDE_BELØP,
                ),
            )
        }

        every { vedtakService.hentVedtak(any()) }.answers {
            opprettVedtakDtoForBidragsberegning(
                skyldner = skyldner!!.verdi,
                kravhaver = kravhaverLøpendeBidrag!!.verdi,
                mottaker = mottakerLøpendeBidrag!!.verdi,
                sak = SAK_LØPENDE_BIDRAG,
                beregnetBeløp = LØPENDE_BELØP,
            )
        }

        every { personConsumer.hentFødselsdatoForPerson(kravhaverLøpendeBidrag!!) }.answers {
            LocalDate.parse("2019-06-26")
        }

        every { personConsumer.hentFødselsdatoForPerson(mottakerLøpendeBidrag!!) }.answers {
            LocalDate.parse("1982-05-05")
        }

        val beregnResponse = bidragsberegningOrkestrator.utførBidragsberegningV3(beregnRequest)

        assertSoftly(beregnResponse) {
            grunnlagListe shouldHaveAtLeastSize 2
            resultat shouldHaveSize 2
            resultat[0].søknadsbarnreferanse shouldBe "Person_Søknadsbarn_01"
            resultat[0].resultatVedtakListe shouldHaveSize 1
            resultat[0].resultatVedtakListe[0].periodeListe shouldHaveSize 2
            resultat[0].fatteVedtakAnbefalt shouldBe true

            // Sjekker at det ikke er duplikate referanser
            val alleReferanser = grunnlagListe.map { it.referanse }
            alleReferanser.size shouldBe alleReferanser.distinct().size

            val alleRefererteReferanser = grunnlagListe
                .flatMap { it.grunnlagsreferanseListe + it.gjelderBarnReferanse + it.gjelderReferanse }
                .filterNotNull()
                .distinct()
            alleReferanser shouldContainAll alleRefererteReferanser
        }
    }

    // Forholdsmessig fordeling utløst i runde 1 av beregningen
    // 2 søknadsbarn i requesten
    // Søknadsbarn 1 er en del av opprinnelig behandling
    // Søknadsbarn 2 er ikke en del av opprinnelig behandling (aka revurderingsbarn)
    // Beregning runde 2A:
    // - Revurderingsbarn fjernes fra requesten
    // - Det innhentes løpende bidrag
    // - Beregning kjøres som i runde 1
    // - Ikke full evne for søknadsbarn 1 mot løpende bidrag for søknadsbarn 2
    // - Det kjøres en ny beregning (runde 2B)
    // Beregning runde 2B:
    // - Søknadsbarn 2 beregnes med nye grunnlag, som et ordinært søknadsbarn
    // - Overlappende perioder fra løpende bidrag fjernes (dvs. alle løpende bidragsperioder for søknadsbarn 2)
    // - Full evne for søknadsbarnet i alle perioder
    // - Det leveres tilbake full beregning for revurderingsbarnet med anbefaling om å ikke fatte vedtak
    @Test
    fun `beregn bidrag v3 - beregning runde 2 - FF utløst - test C`() {
        filnavnBeregnGrunnlag = "src/test/resources/testfiler/bidragsberegning_orkestrator/test03_v3_beregning_runde2_testC.json"
        val beregnRequest = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(filnavnBeregnGrunnlag)

        val kravhaverLøpendeBidrag = beregnRequest.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_SØKNADSBARN)
            .filter { it.innhold.delAvOpprinneligBehandling == false }
            .map { it.innhold.ident }
            .first()

        val mottakerLøpendeBidrag = beregnRequest.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_BIDRAGSMOTTAKER)
            .map { it.innhold.ident }
            .first()

        val skyldner = beregnRequest.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_BIDRAGSPLIKTIG)
            .map { it.innhold.ident }
            .first()

        every { vedtakService.hentSisteLøpendeStønader(any()) }.answers {
            listOf(
                LøpendeBidragssak(
                    sak = Saksnummer(SAK_LØPENDE_BIDRAG),
                    type = Stønadstype.BIDRAG,
                    kravhaver = kravhaverLøpendeBidrag!!,
                    løpendeBeløp = LØPENDE_BELØP,
                ),
            )
        }

        every { vedtakService.hentAlleStønaderForBidragspliktig(any<LøpendeBidragPeriodeRequest>()) } answers {
            LøpendeBidragPeriodeResponse(
                listOf(
                    LøpendeBidrag(
                        sak = Saksnummer(SAK_LØPENDE_BIDRAG),
                        type = Stønadstype.BIDRAG,
                        kravhaver = kravhaverLøpendeBidrag!!,
                        mottaker = mottakerLøpendeBidrag,
                        periodeListe = listOf(
                            BidragPeriode(
                                periode = ÅrMånedsperiode(fom = YearMonth.parse("2025-01"), til = YearMonth.parse("2025-04")),
                                løpendeBeløp = BigDecimal.valueOf(9000),
                            ),
                        ),
                    ),
                    LøpendeBidrag(
                        sak = Saksnummer(SAK_LØPENDE_BIDRAG),
                        type = Stønadstype.BIDRAG,
                        kravhaver = kravhaverLøpendeBidrag,
                        mottaker = mottakerLøpendeBidrag,
                        periodeListe = listOf(
                            BidragPeriode(
                                periode = ÅrMånedsperiode(fom = YearMonth.parse("2025-04"), til = YearMonth.parse("2025-10")),
                                løpendeBeløp = BigDecimal.valueOf(1000),
                            ),
                        ),
                    ),
                ),
            )
        }

        every { vedtakService.finnAlleManuelleVedtakForEvnevurdering(any<Stønadsid>()) }.answers {
            listOf(
                opprettVedtakForStønadBidragsberegning(
                    skyldner = skyldner!!.verdi,
                    kravhaver = kravhaverLøpendeBidrag!!.verdi,
                    mottaker = mottakerLøpendeBidrag!!.verdi,
                    sak = SAK_LØPENDE_BIDRAG,
                    beregnetBeløp = LØPENDE_BELØP,
                ),
            )
        }

        every { vedtakService.hentVedtak(any()) }.answers {
            opprettVedtakDtoForBidragsberegning(
                skyldner = skyldner!!.verdi,
                kravhaver = kravhaverLøpendeBidrag!!.verdi,
                mottaker = mottakerLøpendeBidrag!!.verdi,
                sak = SAK_LØPENDE_BIDRAG,
                beregnetBeløp = LØPENDE_BELØP,
            )
        }

        every { personConsumer.hentFødselsdatoForPerson(kravhaverLøpendeBidrag!!) }.answers {
            LocalDate.parse("2019-06-26")
        }

        every { personConsumer.hentFødselsdatoForPerson(mottakerLøpendeBidrag!!) }.answers {
            LocalDate.parse("1982-05-05")
        }

        val beregnResponse = bidragsberegningOrkestrator.utførBidragsberegningV3(beregnRequest)

        assertSoftly(beregnResponse) {
            grunnlagListe shouldHaveAtLeastSize 1
            resultat shouldHaveSize 2
            resultat[0].søknadsbarnreferanse shouldBe "Person_Søknadsbarn_01"
            resultat[0].resultatVedtakListe shouldHaveSize 1
            resultat[0].resultatVedtakListe[0].periodeListe shouldHaveSize 2
            resultat[0].fatteVedtakAnbefalt shouldBe true
            resultat[1].søknadsbarnreferanse shouldBe "Person_Søknadsbarn_02"
            resultat[1].resultatVedtakListe shouldHaveSize 1
            resultat[1].resultatVedtakListe[0].periodeListe shouldHaveSize 2
            resultat[1].fatteVedtakAnbefalt shouldBe false

            // Sjekker at det ikke er duplikate referanser
            val alleReferanser = grunnlagListe.map { it.referanse }
            alleReferanser.size shouldBe alleReferanser.distinct().size

            val alleRefererteReferanser = grunnlagListe
                .flatMap { it.grunnlagsreferanseListe + it.gjelderBarnReferanse + it.gjelderReferanse }
                .filterNotNull()
                .distinct()
            alleReferanser shouldContainAll alleRefererteReferanser
        }
    }

    // Forholdsmessig fordeling utløst i runde 1 av beregningen
    // 2 søknadsbarn i requesten
    // Søknadsbarn 1 er en del av opprinnelig behandling
    // Søknadsbarn 2 er ikke en del av opprinnelig behandling (aka revurderingsbarn)
    // Beregning runde 2A:
    // - Revurderingsbarn fjernes fra requesten
    // - Det innhentes løpende bidrag
    // - Beregning kjøres som i runde 1
    // - Ikke full evne for søknadsbarn 1 mot løpende bidrag for søknadsbarn 2
    // - Det kjøres en ny beregning (runde 2B)
    // Beregning runde 2B:
    // - Søknadsbarn 2 beregnes med nye grunnlag, som et ordinært søknadsbarn
    // - Overlappende perioder fra løpende bidrag fjernes (dvs. alle løpende bidragsperioder for søknadsbarn 2)
    // - Full evne for søknadsbarnet i siste periode, men ikke full evne i alle perioder
    // - Det leveres tilbake full beregning for revurderingsbarnet med anbefaling om å ikke fatte vedtak
    @Test
    fun `beregn bidrag v3 - beregning runde 2 - FF utløst - test D`() {
        filnavnBeregnGrunnlag = "src/test/resources/testfiler/bidragsberegning_orkestrator/test03_v3_beregning_runde2_testD.json"
        val beregnRequest = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(filnavnBeregnGrunnlag)

        val kravhaverLøpendeBidrag = beregnRequest.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_SØKNADSBARN)
            .filter { it.innhold.delAvOpprinneligBehandling == false }
            .map { it.innhold.ident }
            .first()

        val mottakerLøpendeBidrag = beregnRequest.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_BIDRAGSMOTTAKER)
            .map { it.innhold.ident }
            .first()

        val skyldner = beregnRequest.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_BIDRAGSPLIKTIG)
            .map { it.innhold.ident }
            .first()

        every { vedtakService.hentSisteLøpendeStønader(any()) }.answers {
            listOf(
                LøpendeBidragssak(
                    sak = Saksnummer(SAK_LØPENDE_BIDRAG),
                    type = Stønadstype.BIDRAG,
                    kravhaver = kravhaverLøpendeBidrag!!,
                    løpendeBeløp = LØPENDE_BELØP,
                ),
            )
        }

        every { vedtakService.hentAlleStønaderForBidragspliktig(any<LøpendeBidragPeriodeRequest>()) } answers {
            LøpendeBidragPeriodeResponse(
                listOf(
                    LøpendeBidrag(
                        sak = Saksnummer(SAK_LØPENDE_BIDRAG),
                        type = Stønadstype.BIDRAG,
                        kravhaver = kravhaverLøpendeBidrag!!,
                        mottaker = mottakerLøpendeBidrag,
                        periodeListe = listOf(
                            BidragPeriode(
                                periode = ÅrMånedsperiode(fom = YearMonth.parse("2025-01"), til = YearMonth.parse("2025-04")),
                                løpendeBeløp = BigDecimal.valueOf(9000),
                            ),
                        ),
                    ),
                    LøpendeBidrag(
                        sak = Saksnummer(SAK_LØPENDE_BIDRAG),
                        type = Stønadstype.BIDRAG,
                        kravhaver = kravhaverLøpendeBidrag,
                        mottaker = mottakerLøpendeBidrag,
                        periodeListe = listOf(
                            BidragPeriode(
                                periode = ÅrMånedsperiode(fom = YearMonth.parse("2025-04"), til = YearMonth.parse("2025-10")),
                                løpendeBeløp = BigDecimal.valueOf(1000),
                            ),
                        ),
                    ),
                ),
            )
        }

        every { vedtakService.finnAlleManuelleVedtakForEvnevurdering(any<Stønadsid>()) }.answers {
            listOf(
                opprettVedtakForStønadBidragsberegning(
                    skyldner = skyldner!!.verdi,
                    kravhaver = kravhaverLøpendeBidrag!!.verdi,
                    mottaker = mottakerLøpendeBidrag!!.verdi,
                    sak = SAK_LØPENDE_BIDRAG,
                    beregnetBeløp = LØPENDE_BELØP,
                ),
            )
        }

        every { vedtakService.hentVedtak(any()) }.answers {
            opprettVedtakDtoForBidragsberegning(
                skyldner = skyldner!!.verdi,
                kravhaver = kravhaverLøpendeBidrag!!.verdi,
                mottaker = mottakerLøpendeBidrag!!.verdi,
                sak = SAK_LØPENDE_BIDRAG,
                beregnetBeløp = LØPENDE_BELØP,
            )
        }

        every { personConsumer.hentFødselsdatoForPerson(kravhaverLøpendeBidrag!!) }.answers {
            LocalDate.parse("2019-06-26")
        }

        every { personConsumer.hentFødselsdatoForPerson(mottakerLøpendeBidrag!!) }.answers {
            LocalDate.parse("1982-05-05")
        }

        val beregnResponse = bidragsberegningOrkestrator.utførBidragsberegningV3(beregnRequest)

        assertSoftly(beregnResponse) {
            grunnlagListe shouldHaveAtLeastSize 1
            resultat shouldHaveSize 2
            resultat[0].søknadsbarnreferanse shouldBe "Person_Søknadsbarn_01"
            resultat[0].resultatVedtakListe shouldHaveSize 1
            resultat[0].resultatVedtakListe[0].periodeListe shouldHaveSize 3
            resultat[0].fatteVedtakAnbefalt shouldBe true
            resultat[1].søknadsbarnreferanse shouldBe "Person_Søknadsbarn_02"
            resultat[1].resultatVedtakListe shouldHaveSize 1
            resultat[1].resultatVedtakListe[0].periodeListe shouldHaveSize 3
            resultat[1].fatteVedtakAnbefalt shouldBe false

            // Sjekker at det ikke er duplikate referanser
            val alleReferanser = grunnlagListe.map { it.referanse }
            alleReferanser.size shouldBe alleReferanser.distinct().size

            val alleRefererteReferanser = grunnlagListe
                .flatMap { it.grunnlagsreferanseListe + it.gjelderBarnReferanse + it.gjelderReferanse }
                .filterNotNull()
                .distinct()
            alleReferanser shouldContainAll alleRefererteReferanser
        }
    }

    // Forholdsmessig fordeling utløst i runde 1 av beregningen
    // 2 søknadsbarn i requesten
    // Søknadsbarn 1 er en del av opprinnelig behandling
    // Søknadsbarn 2 er ikke en del av opprinnelig behandling (aka revurderingsbarn)
    // Beregning runde 2A:
    // - Revurderingsbarn fjernes fra requesten
    // - Det innhentes løpende bidrag
    // - Beregning kjøres som i runde 1
    // - Ikke full evne for søknadsbarn 1 mot løpende bidrag for søknadsbarn 2
    // - Det kjøres en ny beregning (runde 2B)
    // Beregning runde 2B:
    // - Søknadsbarn 2 beregnes med nye grunnlag, som et ordinært søknadsbarn
    // - Overlappende perioder fra løpende bidrag fjernes (dvs. alle løpende bidragsperioder for søknadsbarn 2)
    // - Ikke full evne for søknadsbarnet i siste periode basert på ny beregning av revurderingsbarnet
    // - Full evne for søknadsbarnet i siste periode basert på løpende bidrag for revurderingsbarnet (AndelAvBidragsevne fra runde 2A) - det er denne
    //   det sjekkes mot i neste punkt ifht. anbefaling om å fatte vedtak eller ikke
    // - Det leveres tilbake full beregning for revurderingsbarnet med anbefaling om å ikke fatte vedtak
    @Test
    fun `beregn bidrag v3 - beregning runde 2 - FF utløst - test E`() {
        filnavnBeregnGrunnlag = "src/test/resources/testfiler/bidragsberegning_orkestrator/test03_v3_beregning_runde2_testE.json"
        val beregnRequest = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(filnavnBeregnGrunnlag)

        val kravhaverLøpendeBidrag = beregnRequest.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_SØKNADSBARN)
            .filter { it.innhold.delAvOpprinneligBehandling == false }
            .map { it.innhold.ident }
            .first()

        val mottakerLøpendeBidrag = beregnRequest.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_BIDRAGSMOTTAKER)
            .map { it.innhold.ident }
            .first()

        val skyldner = beregnRequest.grunnlagsliste
            .filtrerOgKonverterBasertPåEgenReferanse<Person>(Grunnlagstype.PERSON_BIDRAGSPLIKTIG)
            .map { it.innhold.ident }
            .first()

        every { vedtakService.hentSisteLøpendeStønader(any()) }.answers {
            listOf(
                LøpendeBidragssak(
                    sak = Saksnummer(SAK_LØPENDE_BIDRAG),
                    type = Stønadstype.BIDRAG,
                    kravhaver = kravhaverLøpendeBidrag!!,
                    løpendeBeløp = LØPENDE_BELØP,
                ),
            )
        }

        every { vedtakService.hentAlleStønaderForBidragspliktig(any<LøpendeBidragPeriodeRequest>()) } answers {
            LøpendeBidragPeriodeResponse(
                listOf(
                    LøpendeBidrag(
                        sak = Saksnummer(SAK_LØPENDE_BIDRAG),
                        type = Stønadstype.BIDRAG,
                        kravhaver = kravhaverLøpendeBidrag!!,
                        mottaker = mottakerLøpendeBidrag,
                        periodeListe = listOf(
                            BidragPeriode(
                                periode = ÅrMånedsperiode(fom = YearMonth.parse("2025-01"), til = YearMonth.parse("2025-04")),
                                løpendeBeløp = BigDecimal.valueOf(6000),
                            ),
                        ),
                    ),
                    LøpendeBidrag(
                        sak = Saksnummer(SAK_LØPENDE_BIDRAG),
                        type = Stønadstype.BIDRAG,
                        kravhaver = kravhaverLøpendeBidrag,
                        mottaker = mottakerLøpendeBidrag,
                        periodeListe = listOf(
                            BidragPeriode(
                                periode = ÅrMånedsperiode(fom = YearMonth.parse("2025-04"), til = YearMonth.parse("2025-10")),
                                løpendeBeløp = BigDecimal.valueOf(1000),
                            ),
                        ),
                    ),
                ),
            )
        }

        every { vedtakService.finnAlleManuelleVedtakForEvnevurdering(any<Stønadsid>()) }.answers {
            listOf(
                opprettVedtakForStønadBidragsberegning(
                    skyldner = skyldner!!.verdi,
                    kravhaver = kravhaverLøpendeBidrag!!.verdi,
                    mottaker = mottakerLøpendeBidrag!!.verdi,
                    sak = SAK_LØPENDE_BIDRAG,
                    beregnetBeløp = LØPENDE_BELØP,
                ),
            )
        }

        every { vedtakService.hentVedtak(any()) }.answers {
            opprettVedtakDtoForBidragsberegning(
                skyldner = skyldner!!.verdi,
                kravhaver = kravhaverLøpendeBidrag!!.verdi,
                mottaker = mottakerLøpendeBidrag!!.verdi,
                sak = SAK_LØPENDE_BIDRAG,
                beregnetBeløp = LØPENDE_BELØP,
            )
        }

        every { personConsumer.hentFødselsdatoForPerson(kravhaverLøpendeBidrag!!) }.answers {
            LocalDate.parse("2019-06-26")
        }

        every { personConsumer.hentFødselsdatoForPerson(mottakerLøpendeBidrag!!) }.answers {
            LocalDate.parse("1982-05-05")
        }

        val beregnResponse = bidragsberegningOrkestrator.utførBidragsberegningV3(beregnRequest)

        assertSoftly(beregnResponse) {
            grunnlagListe shouldHaveAtLeastSize 1
            resultat shouldHaveSize 2
            resultat[0].søknadsbarnreferanse shouldBe "Person_Søknadsbarn_01"
            resultat[0].resultatVedtakListe shouldHaveSize 1
            resultat[0].resultatVedtakListe[0].periodeListe shouldHaveSize 2
            resultat[0].fatteVedtakAnbefalt shouldBe true
            resultat[1].søknadsbarnreferanse shouldBe "Person_Søknadsbarn_02"
            resultat[1].resultatVedtakListe shouldHaveSize 1
            resultat[1].resultatVedtakListe[0].periodeListe shouldHaveSize 2
            resultat[1].fatteVedtakAnbefalt shouldBe false

            // Sjekker at det ikke er duplikate referanser
            val alleReferanser = grunnlagListe.map { it.referanse }
            alleReferanser.size shouldBe alleReferanser.distinct().size

            val alleRefererteReferanser = grunnlagListe
                .flatMap { it.grunnlagsreferanseListe + it.gjelderBarnReferanse + it.gjelderReferanse }
                .filterNotNull()
                .distinct()
            alleReferanser shouldContainAll alleRefererteReferanser
        }
    }

    companion object {
        private val KRAVHAVER_LØPENDE_BIDRAG = genererFødselsnummer()
        private val MOTTAKER = genererFødselsnummer()
        private val SKYLDNER = genererFødselsnummer()
        private const val SAK_LØPENDE_BIDRAG = "2"
        private val LØPENDE_BELØP = BigDecimal.valueOf(5000)
    }
}
