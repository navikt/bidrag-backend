package no.nav.bidrag.beregn.barnebidrag.service

import com.fasterxml.jackson.databind.node.POJONode
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.bidrag.beregn.barnebidrag.BeregnBarnebidragApi
import no.nav.bidrag.beregn.barnebidrag.felles.FellesTest
import no.nav.bidrag.beregn.barnebidrag.service.beregning.BeregnBarnebidragService
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningPersonConsumer
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningSakConsumer
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.BidragsberegningOrkestrator
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.HentLøpendeBidragService
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.OmgjøringOrkestrator
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.OmgjøringOrkestratorV2
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.feilmeldingSelvforsørgetBarn
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.sperrBeregningNårBarnetErSelvforsørgetIfUnleashToggleIsOn
import no.nav.bidrag.beregn.barnebidrag.unleash.BarnebidragUnleashFeatures
import no.nav.bidrag.beregn.barnebidrag.unleash.disableUnleashFeature
import no.nav.bidrag.beregn.barnebidrag.unleash.enableUnleashFeature
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider
import no.nav.bidrag.commons.web.mock.stubSjablonProvider
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.sjablon.SjablonTallNavn
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultat
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultatV2
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BidragsberegningOrkestratorRequestV2
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.ResultatPeriode
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import no.nav.bidrag.transport.behandling.felles.grunnlag.SjablonSjablontallPeriode
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidragV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.resultatSluttberegning
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Midlertidig sperre for selvforsørget barn (§ 5): beregningen skal feile, mens avslag for full forpleining (§ 11 c) og delt bosted
 * med BPs andel av U under 50 % (§ 8) skal beregnes som før.
 */
@ExtendWith(MockKExtension::class)
internal class SperrSelvforsørgetBarnTest : FellesTest() {
    private val service = BeregnBarnebidragService()
    private val beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2025-01"))
    private val søknadsbarn2 = "Person_Søknadsbarn_02"

    @MockK(relaxed = true)
    private lateinit var omgjøringOrkestrator: OmgjøringOrkestrator

    @MockK(relaxed = true)
    private lateinit var omgjøringOrkestratorV2: OmgjøringOrkestratorV2

    @MockK(relaxed = true)
    private lateinit var hentLøpendeBidragService: HentLøpendeBidragService

    @MockK(relaxed = true)
    private lateinit var personConsumer: BeregningPersonConsumer

    @MockK(relaxed = true)
    private lateinit var sakConsumer: BeregningSakConsumer

    @BeforeEach
    fun init() {
        stubSjablonProvider()
        mockkObject(UnleashFeaturesProvider)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(UnleashFeaturesProvider)
    }

    @Test
    fun `selvforsørget barn skal gi feil`() {
        enableUnleashFeature(BarnebidragUnleashFeatures.SPERR_SELVFORSØRGET_BARN)
        val resultat = beregn("selvforsorget")
        sluttberegninger(resultat, søknadsbarn2).map { it.barnetErSelvforsørget } shouldContain true

        val feil = assertThrows<IllegalArgumentException> { sperrBeregningNårBarnetErSelvforsørgetIfUnleashToggleIsOn(resultat) }

        feil.message shouldBe feilmeldingSelvforsørgetBarn(BigDecimal.valueOf(100))
        feil.message!! shouldContain "minst 100 ganger forskuddssatsen"
    }

    @Test
    fun `feilmeldingen skal bruke multiplikatoren fra perioden der barnet er selvforsørget`() {
        enableUnleashFeature(BarnebidragUnleashFeatures.SPERR_SELVFORSØRGET_BARN)
        val ekteResultat = beregn("selvforsorget").single { it.søknadsbarnreferanse == søknadsbarn2 }
        val sluttberegning = sluttberegninger(listOf(ekteResultat), søknadsbarn2).first()
        val resultatBeregning = ekteResultat.beregnetBarnebidragResultat.beregnetBarnebidragPeriodeListe.first().resultat
        val periode1 = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2024-07"))
        val periode2 = ÅrMånedsperiode(YearMonth.parse("2024-07"), YearMonth.parse("2025-01"))

        fun sjablon(referanse: String, periode: ÅrMånedsperiode, verdi: Int) = GrunnlagDto(
            referanse = referanse,
            type = Grunnlagstype.SJABLON_SJABLONTALL,
            innhold = POJONode(SjablonSjablontallPeriode(periode, sjablon = SjablonTallNavn.MAKS_INNTEKT_BB_MULTIPLIKATOR, verdi = BigDecimal(verdi))),
        )

        fun sluttberegning(referanse: String, periode: ÅrMånedsperiode, selvforsørget: Boolean, sjablonReferanse: String) = GrunnlagDto(
            referanse = referanse,
            type = Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG,
            innhold = POJONode(sluttberegning.copy(periode = periode, barnetErSelvforsørget = selvforsørget)),
            grunnlagsreferanseListe = listOf(sjablonReferanse),
            gjelderBarnReferanse = søknadsbarn2,
        )

        val resultat = BeregnetBarnebidragResultatV2(
            søknadsbarnreferanse = søknadsbarn2,
            beregnetBarnebidragResultat = BeregnetBarnebidragResultat(
                beregnetBarnebidragPeriodeListe = listOf(
                    ResultatPeriode(periode1, resultatBeregning, listOf("sluttberegning_1")),
                    ResultatPeriode(periode2, resultatBeregning, listOf("sluttberegning_2")),
                ),
                grunnlagListe = listOf(
                    sluttberegning("sluttberegning_1", periode1, selvforsørget = false, sjablonReferanse = "sjablon_100"),
                    sluttberegning("sluttberegning_2", periode2, selvforsørget = true, sjablonReferanse = "sjablon_50"),
                    sjablon("sjablon_100", periode1, 100),
                    sjablon("sjablon_50", periode2, 50),
                ),
            ),
        )

        val feil = assertThrows<IllegalArgumentException> { sperrBeregningNårBarnetErSelvforsørgetIfUnleashToggleIsOn(listOf(resultat)) }

        feil.message shouldBe feilmeldingSelvforsørgetBarn(BigDecimal.valueOf(50))
    }

    @Test
    fun `sperren skal være på som standard når Unleash ikke kjenner bryteren`() {
        BarnebidragUnleashFeatures.SPERR_SELVFORSØRGET_BARN.isEnabled shouldBe true
        assertThrows<IllegalArgumentException> { sperrBeregningNårBarnetErSelvforsørgetIfUnleashToggleIsOn(beregn("selvforsorget")) }
    }

    @Test
    fun `selvforsørget barn skal ikke gi feil når bryteren er av`() {
        disableUnleashFeature(BarnebidragUnleashFeatures.SPERR_SELVFORSØRGET_BARN)
        val resultat = beregn("selvforsorget")

        assertDoesNotThrow { sperrBeregningNårBarnetErSelvforsørgetIfUnleashToggleIsOn(resultat) }
        resultatkoder(resultat, søknadsbarn2) shouldContain Resultatkode.BARNET_ER_SELVFORSØRGET
    }

    @Test
    fun `full forpleining (paragraf 11 c) skal ikke gi feil`() {
        enableUnleashFeature(BarnebidragUnleashFeatures.SPERR_SELVFORSØRGET_BARN)
        val resultat = beregn("forpleining")

        assertDoesNotThrow { sperrBeregningNårBarnetErSelvforsørgetIfUnleashToggleIsOn(resultat) }
        resultatkoder(resultat, søknadsbarn2).forEach { it shouldBe Resultatkode.FULLT_UNDERHOLDT_AV_OFFENTLIG }
    }

    @Test
    fun `delt bosted med BPs andel av U under 50 prosent (paragraf 8) skal ikke gi feil`() {
        enableUnleashFeature(BarnebidragUnleashFeatures.SPERR_SELVFORSØRGET_BARN)
        val resultat = beregn("delt_bosted")

        assertDoesNotThrow { sperrBeregningNårBarnetErSelvforsørgetIfUnleashToggleIsOn(resultat) }
        resultatkoder(resultat, søknadsbarn2).forEach { it shouldBe Resultatkode.BIDRAG_IKKE_BEREGNET_DELT_BOSTED }
    }

    @Test
    fun `orkestrator skal gi beregningsfeil med teknisk feil for alle søknadsbarn når et barn er selvforsørget`() {
        enableUnleashFeature(BarnebidragUnleashFeatures.SPERR_SELVFORSØRGET_BARN)
        val request = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(
            "src/test/resources/testfiler/bidragsberegning_orkestrator/test01_v3_beregn_bidrag_selvforsorget_grunnlag.json",
        )

        val respons = orkestrator().utførBidragsberegningV3(request)

        assertSoftly(respons) {
            resultat shouldHaveSize 2
            resultat.forEach {
                it.beregningsfeil shouldNotBe null
                it.beregningsfeil!!.message!! shouldContain "teknisk feil"
                it.beregningsfeil!!.message!! shouldContain feilmeldingSelvforsørgetBarn(BigDecimal.valueOf(100))
            }
        }
    }

    @Test
    fun `orkestrator skal beregne som før når ingen barn er selvforsørget`() {
        enableUnleashFeature(BarnebidragUnleashFeatures.SPERR_SELVFORSØRGET_BARN)
        val request = lesFilOgByggRequestGenerisk<BidragsberegningOrkestratorRequestV2>(
            "src/test/resources/testfiler/bidragsberegning_orkestrator/test01_v3_beregn_bidrag_grunnlag.json",
        )

        val respons = orkestrator().utførBidragsberegningV3(request)

        respons.resultat.forEach { it.beregningsfeil shouldBe null }
    }

    private fun orkestrator() = BidragsberegningOrkestrator(
        barnebidragApi = BeregnBarnebidragApi(),
        omgjøringOrkestrator = omgjøringOrkestrator,
        omgjøringOrkestratorV2 = omgjøringOrkestratorV2,
        hentLøpendeBidragService = hentLøpendeBidragService,
        personConsumer = personConsumer,
        sakConsumer = sakConsumer,
    )

    private fun beregn(scenario: String): List<BeregnetBarnebidragResultatV2> {
        val request: List<BeregnGrunnlag> =
            lesFilOgByggRequestGenerisk("src/test/resources/testfiler/barnebidrag/barnebidragV2_avslag_flere_barn_$scenario.json")
        return service.beregnBarnebidragV2(
            beregningsperiode = beregningsperiode,
            grunnlagSøknadsbarnListe = request,
        )
    }

    private fun sluttberegninger(resultat: List<BeregnetBarnebidragResultatV2>, søknadsbarn: String) = resultat
        .flatMap { it.beregnetBarnebidragResultat.grunnlagListe }
        .filtrerOgKonverterBasertPåEgenReferanse<SluttberegningBarnebidragV2>(Grunnlagstype.SLUTTBEREGNING_BARNEBIDRAG)
        .filter { it.gjelderBarnReferanse == søknadsbarn }
        .map { it.innhold }
        .also { it.shouldNotBeEmpty() }

    private fun resultatkoder(resultat: List<BeregnetBarnebidragResultatV2>, søknadsbarn: String): List<Resultatkode?> {
        val beregning = resultat.single { it.søknadsbarnreferanse == søknadsbarn }.beregnetBarnebidragResultat
        return beregning.beregnetBarnebidragPeriodeListe
            .map { beregning.grunnlagListe.resultatSluttberegning(it.grunnlagsreferanseListe) }
            .also { it.shouldNotBeEmpty() }
    }
}
