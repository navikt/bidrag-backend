package no.nav.bidrag.beregn.barnebidrag.beregning

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import no.nav.bidrag.beregn.barnebidrag.bo.AndelAvBidragsevneBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.AndelAvBidragsevneDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BarnetilleggDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BidragJustertForBPBarnetilleggBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BidragTilFordelingBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BidragTilFordelingDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BidragspliktigesAndelDeltBostedDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BpAndelUnderholdskostnadDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.Evne25ProsentAvInntektDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.SamværsfradragDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.SumBidragTilFordelingDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.SøknadsbarnetBorHosBpGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.UnderholdskostnadDelberegningBeregningGrunnlag
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

class EndeligBidragBeregningV2Test {

    @Nested
    inner class AndelAvBidragsevne {

        @Test
        fun `barn med avslag skal ikke gi evnesprekk selv om andre barn har bidrag`() {
            val resultat = EndeligBidragBeregningV2.beregnAndelAvBidragsevne(
                andelAvBidragsevneGrunnlag(bidragTilFordeling = null, sumBidragTilFordeling = BigDecimal(5000), evne = BigDecimal(3000)),
            )

            resultat.harBPFullEvne shouldBe true
            resultat.bidragEtterFordeling.signum() shouldBe 0
        }

        @Test
        fun `barn med bidrag til fordeling lik 0 skal gi evnesprekk når evnen ikke dekker summen for alle barna`() {
            val resultat = EndeligBidragBeregningV2.beregnAndelAvBidragsevne(
                andelAvBidragsevneGrunnlag(bidragTilFordeling = BigDecimal.ZERO, sumBidragTilFordeling = BigDecimal(5000), evne = BigDecimal.ZERO),
            )

            resultat.harBPFullEvne shouldBe false
        }

        @Test
        fun `barn med bidrag til fordeling lik 0 skal ikke gi evnesprekk når evnen dekker summen for alle barna`() {
            val resultat = EndeligBidragBeregningV2.beregnAndelAvBidragsevne(
                andelAvBidragsevneGrunnlag(bidragTilFordeling = BigDecimal.ZERO, sumBidragTilFordeling = BigDecimal(5000), evne = BigDecimal(9000)),
            )

            resultat.harBPFullEvne shouldBe true
            resultat.bidragEtterFordeling.signum() shouldBe 0
        }

        @Test
        fun `barn med bidrag til fordeling høyere enn andel av evne skal gi evnesprekk`() {
            val resultat = EndeligBidragBeregningV2.beregnAndelAvBidragsevne(
                andelAvBidragsevneGrunnlag(bidragTilFordeling = BigDecimal(4000), sumBidragTilFordeling = BigDecimal(8000), evne = BigDecimal(6000)),
            )

            resultat.harBPFullEvne shouldBe false
        }

        @Test
        fun `barn med bidrag til fordeling innenfor andel av evne skal ikke gi evnesprekk`() {
            val resultat = EndeligBidragBeregningV2.beregnAndelAvBidragsevne(
                andelAvBidragsevneGrunnlag(bidragTilFordeling = BigDecimal(4000), sumBidragTilFordeling = BigDecimal(8000), evne = BigDecimal(9000)),
            )

            resultat.harBPFullEvne shouldBe true
        }
    }

    @Nested
    inner class BidragJustertForNettoBarnetilleggBM {

        @ParameterizedTest(name = "nettoBarnetilleggBM = {0}")
        @NullSource
        @ValueSource(strings = ["0"])
        fun `skal ikke være justert for barnetillegg BM når BM ikke har barnetillegg og U er 0`(nettoBarnetilleggBM: String?) {
            val resultat = EndeligBidragBeregningV2.beregnBidragTilFordeling(
                bidragTilFordelingGrunnlag(
                    underholdskostnad = BigDecimal.ZERO,
                    bpAndel = BigDecimal.ZERO,
                    nettoBarnetilleggBM = nettoBarnetilleggBM?.let { BigDecimal(it) },
                ),
            )

            resultat.erBidragJustertForNettoBarnetilleggBM shouldBe false
        }

        @Test
        fun `skal være justert for barnetillegg BM når U minus barnetillegg er lavere enn BPs andel`() {
            val resultat = EndeligBidragBeregningV2.beregnBidragTilFordeling(
                bidragTilFordelingGrunnlag(underholdskostnad = BigDecimal(8000), bpAndel = BigDecimal(6000), nettoBarnetilleggBM = BigDecimal(3000)),
            )

            resultat.erBidragJustertForNettoBarnetilleggBM shouldBe true
        }
    }

    @Nested
    inner class BidragJustertForNettoBarnetilleggBP {

        @Test
        fun `skal ikke være justert til barnetillegg BP når barnetillegget er 0 eller ved delt bosted`() {
            listOf(
                bidragJustertForBPBarnetilleggGrunnlag(bidragEtterFordeling = BigDecimal.ZERO, nettoBarnetilleggBP = BigDecimal.ZERO),
                bidragJustertForBPBarnetilleggGrunnlag(
                    bidragEtterFordeling = BigDecimal(2000),
                    nettoBarnetilleggBP = BigDecimal(2000),
                    erDeltBosted = true,
                ),
            ).forEach { grunnlag ->
                EndeligBidragBeregningV2.beregnBidragJustertForBPBarnetillegg(grunnlag).erBidragJustertTilNettoBarnetilleggBP shouldBe false
            }
        }

        @Test
        fun `skal være justert til barnetillegg BP når barnetillegget er høyere enn bidraget`() {
            val resultat = EndeligBidragBeregningV2.beregnBidragJustertForBPBarnetillegg(
                bidragJustertForBPBarnetilleggGrunnlag(bidragEtterFordeling = BigDecimal(1000), nettoBarnetilleggBP = BigDecimal(2000)),
            )

            resultat.erBidragJustertTilNettoBarnetilleggBP shouldBe true
            resultat.bidragJustertForNettoBarnetilleggBP.compareTo(BigDecimal(2000)) shouldBe 0
        }
    }

    @Test
    fun `delt bosted skal gi avslag bare når BPs andel av U er under 50 prosent`() {
        fun beregn(bpAndel: Int, andelDeltBosted: Int, bpAndelFaktor: String) = EndeligBidragBeregningV2.beregnBidragTilFordeling(
            bidragTilFordelingGrunnlag(
                underholdskostnad = BigDecimal(8000),
                bpAndel = BigDecimal(bpAndel),
                nettoBarnetilleggBM = null,
                andelDeltBosted = andelDeltBosted(beløp = BigDecimal(andelDeltBosted)),
                bpAndelFaktor = BigDecimal(bpAndelFaktor),
            ),
        )

        val under50Prosent = beregn(bpAndel = 3200, andelDeltBosted = 0, bpAndelFaktor = "0.4")
        val over50Prosent = beregn(bpAndel = 4800, andelDeltBosted = 800, bpAndelFaktor = "0.6")

        assertSoftly {
            under50Prosent.erAvslag shouldBe true
            over50Prosent.erAvslag shouldBe false
            over50Prosent.bidragTilFordeling.compareTo(BigDecimal(800)) shouldBe 0
        }
    }

    @Test
    fun `forpleining skal gi avslag bare når den er lik eller høyere enn U`() {
        fun erAvslag(underholdskostnad: Int, bpAndel: Int, forpleining: Int?) = EndeligBidragBeregningV2.beregnBidragTilFordeling(
            bidragTilFordelingGrunnlag(
                underholdskostnad = BigDecimal(underholdskostnad),
                bpAndel = BigDecimal(bpAndel),
                nettoBarnetilleggBM = null,
                forpleining = forpleining?.let { BigDecimal(it) },
            ),
        ).erAvslag

        assertSoftly {
            // U er 0 etter at forpleiningen er trukket fra
            erAvslag(underholdskostnad = 0, bpAndel = 0, forpleining = 9000) shouldBe true
            erAvslag(underholdskostnad = 1000, bpAndel = 600, forpleining = 8000) shouldBe false
            erAvslag(underholdskostnad = 0, bpAndel = 0, forpleining = null) shouldBe false
        }
    }
}

private fun andelDeltBosted(beløp: BigDecimal) = BidragspliktigesAndelDeltBostedDelberegningBeregningGrunnlag(
    referanse = "andelDeltBosted",
    bpAndelAvUVedDeltBostedBeløp = beløp,
)

private fun andelAvBidragsevneGrunnlag(
    bidragTilFordeling: BigDecimal?,
    sumBidragTilFordeling: BigDecimal,
    evne: BigDecimal,
) = AndelAvBidragsevneBeregningGrunnlag(
    evne25ProsentAvInntektBeregningGrunnlag = Evne25ProsentAvInntektDelberegningBeregningGrunnlag(
        referanse = "evne25ProsentAvInntekt",
        evneJustertFor25ProsentAvInntekt = evne,
    ),
    sumBidragTilFordelingBeregningGrunnlag = SumBidragTilFordelingDelberegningBeregningGrunnlag(
        referanse = "sumBidragTilFordeling",
        sumBidragTilFordeling = sumBidragTilFordeling,
        sumPrioriterteBidragTilFordeling = BigDecimal.ZERO,
    ),
    bidragTilFordelingBeregningGrunnlag = bidragTilFordeling?.let {
        BidragTilFordelingDelberegningBeregningGrunnlag(referanse = "bidragTilFordeling", bidragTilFordeling = it)
    },
)

private fun bidragTilFordelingGrunnlag(
    underholdskostnad: BigDecimal,
    bpAndel: BigDecimal,
    nettoBarnetilleggBM: BigDecimal?,
    andelDeltBosted: BidragspliktigesAndelDeltBostedDelberegningBeregningGrunnlag? = null,
    forpleining: BigDecimal? = null,
    bpAndelFaktor: BigDecimal = BigDecimal("0.5"),
) = BidragTilFordelingBeregningGrunnlag(
    underholdskostnadBeregningGrunnlag = UnderholdskostnadDelberegningBeregningGrunnlag(
        referanse = "underholdskostnad",
        beløp = underholdskostnad,
        forpleining = forpleining,
    ),
    bpAndelUnderholdskostnadBeregningGrunnlag = BpAndelUnderholdskostnadDelberegningBeregningGrunnlag(
        referanse = "bpAndelUnderholdskostnad",
        andelBeløp = bpAndel,
        andelFaktor = bpAndelFaktor,
        barnetErSelvforsørget = false,
    ),
    barnetilleggBMBeregningGrunnlag = nettoBarnetilleggBM?.let { BarnetilleggDelberegningBeregningGrunnlag(referanse = "barnetilleggBM", beløp = it) },
    samværsfradragBeregningGrunnlag = SamværsfradragDelberegningBeregningGrunnlag(referanse = "samværsfradrag", beløp = BigDecimal.ZERO),
    bidragspliktigesAndelDeltBostedBeregningGrunnlag = andelDeltBosted,
    søknadsbarnetBorHosBpGrunnlag = SøknadsbarnetBorHosBpGrunnlag(referanse = "søknadsbarnetBorHosBp", søknadsbarnetBorHosBp = false),
)

private fun bidragJustertForBPBarnetilleggGrunnlag(
    bidragEtterFordeling: BigDecimal,
    nettoBarnetilleggBP: BigDecimal?,
    erDeltBosted: Boolean = false,
) = BidragJustertForBPBarnetilleggBeregningGrunnlag(
    andelAvBidragsevneBeregningGrunnlag = AndelAvBidragsevneDelberegningBeregningGrunnlag(
        referanse = "andelAvBidragsevne",
        bidragEtterFordeling = bidragEtterFordeling,
    ),
    barnetilleggBPBeregningGrunnlag = nettoBarnetilleggBP?.let { BarnetilleggDelberegningBeregningGrunnlag(referanse = "barnetilleggBP", beløp = it) },
    bidragspliktigesAndelDeltBostedBeregningGrunnlag = if (erDeltBosted) {
        andelDeltBosted(beløp = bidragEtterFordeling)
    } else {
        null
    },
)
