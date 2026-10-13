package no.nav.bidrag.beregn.barnebidrag.api

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockkObject
import no.nav.bidrag.beregn.barnebidrag.felles.FellesTest
import no.nav.bidrag.beregn.barnebidrag.service.beregning.BeregnBarnebidragService
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider
import no.nav.bidrag.commons.web.mock.stubSjablonProvider
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BeregnetBarnebidragResultatV2
import no.nav.bidrag.transport.behandling.beregning.felles.BeregnGrunnlag
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningAndelAvBidragsevne
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningBidragTilFordeling
import no.nav.bidrag.transport.behandling.felles.grunnlag.DelberegningSumBidragTilFordeling
import no.nav.bidrag.transport.behandling.felles.grunnlag.SluttberegningBarnebidragV2
import no.nav.bidrag.transport.behandling.felles.grunnlag.filtrerOgKonverterBasertPåEgenReferanse
import no.nav.bidrag.transport.behandling.felles.grunnlag.resultatSluttberegning
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

/**
 * Beregner to søknadsbarn gjennom hele V2-kjeden. BP har evne til å betale bidraget for søknadsbarn 1 alene, men ikke for
 * begge barna fra juli 2024 (se kontroll-scenarioet). Når søknadsbarn 2 får avslag (delt bosted eller forpleining), skal det
 * verken være med i eller utløse forholdsmessig fordeling for søknadsbarn 1.
 */
internal class BeregnBarnebidragAvslagFlereBarnTest : FellesTest() {
    private val api = BeregnBarnebidragService()
    private val beregningsperiode = ÅrMånedsperiode(YearMonth.parse("2024-05"), YearMonth.parse("2025-01"))
    private val søknadsbarn1 = "Person_Søknadsbarn_01"
    private val søknadsbarn2 = "Person_Søknadsbarn_02"

    @BeforeEach
    fun initMock() {
        stubSjablonProvider()
        mockkObject(UnleashFeaturesProvider)
    }

    @Test
    fun `kontroll - BP mangler evne til begge barna når begge har bidrag`() {
        val resultat = beregn("kontroll")

        assertSoftly {
            andelerAvEvne(resultat).filterNot { it.harBPFullEvne }.shouldNotBeEmpty()
            sluttberegninger(resultat, søknadsbarn2).forEach { it.resultatBeløp shouldNotBe null }
        }
    }

    @Test
    fun `søknadsbarn med avslag for delt bosted skal ikke utløse forholdsmessig fordeling`() {
        val resultat = beregn("delt_bosted")

        assertSoftly {
            sluttberegninger(resultat, søknadsbarn2).forEach {
                it.bidragIkkeBeregnetDeltBosted shouldBe true
                it.resultatBeløp shouldBe null
            }
            resultatkoder(resultat, søknadsbarn2).forEach { it shouldBe Resultatkode.BIDRAG_IKKE_BEREGNET_DELT_BOSTED }
        }
        skalBeregneSøknadsbarn1UtenForholdsmessigFordeling(resultat)
    }

    @Test
    fun `søknadsbarn med avslag for forpleining skal ikke utløse forholdsmessig fordeling`() {
        val resultat = beregn("forpleining")

        assertSoftly {
            sluttberegninger(resultat, søknadsbarn2).forEach {
                it.forpleiningDekkerUnderholdskostnaden shouldBe true
                it.resultatBeløp shouldBe null
            }
            resultatkoder(resultat, søknadsbarn2).forEach { it shouldBe Resultatkode.FULLT_UNDERHOLDT_AV_OFFENTLIG }
        }
        skalBeregneSøknadsbarn1UtenForholdsmessigFordeling(resultat)
    }

    @Test
    fun `delt bosted med BPs andel av U nøyaktig 50 prosent skal gi bidrag 0 uten avslag og uten evnesprekk`() {
        val resultat = beregn("delt_bosted_50")

        assertSoftly {
            sluttberegninger(resultat, søknadsbarn2).forEach {
                it.bidragIkkeBeregnetDeltBosted shouldBe false
                it.resultatBeløp?.signum() shouldBe 0
            }
            resultatkoder(resultat, søknadsbarn2).forEach { it shouldBe Resultatkode.BIDRAG_JUSTERT_FOR_DELT_BOSTED }
            andelerAvEvne(resultat).forEach { it.harBPFullEvne shouldBe true }
        }
    }

    @Test
    fun `delt bosted med BPs andel av U nøyaktig 50 prosent og løpende bidrag skal beregnes uten exception`() {
        val søknadsbarn: List<BeregnGrunnlag> =
            lesFilOgByggRequestGenerisk("src/test/resources/testfiler/barnebidrag/barnebidragV2_avslag_flere_barn_delt_bosted_50.json")
        val løpendeBidrag: List<BeregnGrunnlag> =
            lesFilOgByggRequestGenerisk("src/test/resources/testfiler/barnebidrag/barnebidragV2_eksempel10A_løpende_bidrag.json")

        val resultat = api.beregnBarnebidragV2(
            beregningsperiode = beregningsperiode,
            grunnlagSøknadsbarnListe = søknadsbarn,
            grunnlagLøpendeBidragListe = løpendeBidrag,
            grunnlagPrivatAvtaleListe = emptyList(),
            grunnlagValutakursListe = emptyList(),
        )

        andelerAvEvne(resultat).forEach { it.harBPFullEvne shouldBe true }
    }

    private fun skalBeregneSøknadsbarn1UtenForholdsmessigFordeling(resultat: List<BeregnetBarnebidragResultatV2>) {
        val bidragTilFordelingSøknadsbarn1 = grunnlag(resultat)
            .filtrerOgKonverterBasertPåEgenReferanse<DelberegningBidragTilFordeling>(Grunnlagstype.DELBEREGNING_BIDRAG_TIL_FORDELING)
            .filter { it.gjelderBarnReferanse == søknadsbarn1 }

        assertSoftly {
            // Søknadsbarn 2 har ikke noe bidrag til fordeling, og har ingen andel av bidragsevnen
            grunnlag(resultat)
                .filtrerOgKonverterBasertPåEgenReferanse<DelberegningBidragTilFordeling>(Grunnlagstype.DELBEREGNING_BIDRAG_TIL_FORDELING)
                .filter { it.gjelderBarnReferanse == søknadsbarn2 }
                .shouldBeEmpty()
            andelerAvEvne(resultat, søknadsbarn2).shouldBeEmpty()

            // Summen som fordeles er bare bidraget til søknadsbarn 1
            val summer = grunnlag(resultat)
                .filtrerOgKonverterBasertPåEgenReferanse<DelberegningSumBidragTilFordeling>(Grunnlagstype.DELBEREGNING_SUM_BIDRAG_TIL_FORDELING)
            summer.shouldNotBeEmpty()
            summer.forEach { sum ->
                val bidragSøknadsbarn1 = bidragTilFordelingSøknadsbarn1.single { it.innhold.periode.inneholder(sum.innhold.periode) }
                sum.innhold.sumBidragTilFordeling.compareTo(bidragSøknadsbarn1.innhold.bidragTilFordeling) shouldBe 0
            }

            // BP har full evne til søknadsbarn 1 i alle perioder, og søknadsbarn 1 får fullt bidrag
            andelerAvEvne(resultat, søknadsbarn1) shouldHaveSize 2
            andelerAvEvne(resultat, søknadsbarn1).forEach { it.harBPFullEvne shouldBe true }
            sluttberegninger(resultat, søknadsbarn1).forEach { it.resultatBeløp?.signum() shouldBe 1 }
        }
    }

    private fun beregn(scenario: String): List<BeregnetBarnebidragResultatV2> {
        val request: List<BeregnGrunnlag> =
            lesFilOgByggRequestGenerisk("src/test/resources/testfiler/barnebidrag/barnebidragV2_avslag_flere_barn_$scenario.json")
        return api.beregnBarnebidragV2(
            beregningsperiode = beregningsperiode,
            grunnlagSøknadsbarnListe = request,
            grunnlagLøpendeBidragListe = emptyList(),
            grunnlagPrivatAvtaleListe = emptyList(),
            grunnlagValutakursListe = emptyList(),
        )
    }

    private fun grunnlag(resultat: List<BeregnetBarnebidragResultatV2>) = resultat.flatMap { it.beregnetBarnebidragResultat.grunnlagListe }.distinctBy { it.referanse }

    private fun andelerAvEvne(resultat: List<BeregnetBarnebidragResultatV2>, søknadsbarn: String? = null) = grunnlag(resultat)
        .filtrerOgKonverterBasertPåEgenReferanse<DelberegningAndelAvBidragsevne>(Grunnlagstype.DELBEREGNING_ANDEL_AV_BIDRAGSEVNE)
        .filter { søknadsbarn == null || it.gjelderBarnReferanse == søknadsbarn }
        .map { it.innhold }

    private fun sluttberegninger(resultat: List<BeregnetBarnebidragResultatV2>, søknadsbarn: String) = grunnlag(resultat)
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
