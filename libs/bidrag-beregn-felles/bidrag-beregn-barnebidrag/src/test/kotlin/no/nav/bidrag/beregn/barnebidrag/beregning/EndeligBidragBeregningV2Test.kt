package no.nav.bidrag.beregn.barnebidrag.beregning

import io.kotest.matchers.shouldBe
import no.nav.bidrag.beregn.barnebidrag.bo.AndelAvBidragsevneBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.AndelAvBidragsevneDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BarnetilleggDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BidragJustertForBPBarnetilleggBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BidragJustertForBPBarnetilleggDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BidragTilFordelingBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BidragTilFordelingDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BidragspliktigesAndelDeltBostedDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BpAndelUnderholdskostnadDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.Evne25ProsentAvInntektDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.SamværsfradragDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.SluttberegningBarnebidragV2BeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.SumBidragTilFordelingDelberegningBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.SøknadsbarnetBorHosBpGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.UnderholdskostnadDelberegningBeregningGrunnlag
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
        fun `barn med bidrag til fordeling lik 0 skal fortsatt gi evnesprekk når andre barn har bidrag`() {
            val resultat = EndeligBidragBeregningV2.beregnAndelAvBidragsevne(
                andelAvBidragsevneGrunnlag(bidragTilFordeling = BigDecimal.ZERO, sumBidragTilFordeling = BigDecimal(5000), evne = BigDecimal.ZERO),
            )

            resultat.harBPFullEvne shouldBe false
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

        @Test
        fun `skal ikke være justert for barnetillegg BM når BM ikke har barnetillegg og U er 0`() {
            val resultat = EndeligBidragBeregningV2.beregnBidragTilFordeling(
                bidragTilFordelingGrunnlag(underholdskostnad = BigDecimal.ZERO, bpAndel = BigDecimal.ZERO, nettoBarnetilleggBM = null),
            )

            resultat.erBidragJustertForNettoBarnetilleggBM shouldBe false
        }

        @Test
        fun `skal ikke være justert for barnetillegg BM når barnetillegget er 0`() {
            val resultat = EndeligBidragBeregningV2.beregnBidragTilFordeling(
                bidragTilFordelingGrunnlag(underholdskostnad = BigDecimal.ZERO, bpAndel = BigDecimal.ZERO, nettoBarnetilleggBM = BigDecimal.ZERO),
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
        fun `skal ikke være justert til barnetillegg BP når barnetillegget og bidraget er 0`() {
            val resultat = EndeligBidragBeregningV2.beregnBidragJustertForBPBarnetillegg(
                bidragJustertForBPBarnetilleggGrunnlag(bidragEtterFordeling = BigDecimal.ZERO, nettoBarnetilleggBP = BigDecimal.ZERO),
            )

            resultat.erBidragJustertTilNettoBarnetilleggBP shouldBe false
        }

        @Test
        fun `skal ikke være justert til barnetillegg BP ved delt bosted`() {
            val resultat = EndeligBidragBeregningV2.beregnBidragJustertForBPBarnetillegg(
                bidragJustertForBPBarnetilleggGrunnlag(
                    bidragEtterFordeling = BigDecimal(2000),
                    nettoBarnetilleggBP = BigDecimal(2000),
                    erDeltBosted = true,
                ),
            )

            resultat.erBidragJustertTilNettoBarnetilleggBP shouldBe false
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

    @Nested
    inner class DeltBosted {

        @Test
        fun `skal gi avslag når BPs andel av U er under 50 prosent`() {
            val resultat = EndeligBidragBeregningV2.beregnBidragTilFordeling(
                bidragTilFordelingGrunnlag(
                    underholdskostnad = BigDecimal(8000),
                    bpAndel = BigDecimal(3200),
                    nettoBarnetilleggBM = null,
                    andelDeltBosted = andelDeltBosted(faktor = BigDecimal.ZERO, beløp = BigDecimal.ZERO),
                ),
            )

            resultat.erAvslag shouldBe true
        }

        @Test
        fun `skal ikke gi avslag når BPs andel av U er over 50 prosent`() {
            val resultat = EndeligBidragBeregningV2.beregnBidragTilFordeling(
                bidragTilFordelingGrunnlag(
                    underholdskostnad = BigDecimal(8000),
                    bpAndel = BigDecimal(4800),
                    nettoBarnetilleggBM = null,
                    andelDeltBosted = andelDeltBosted(faktor = BigDecimal("0.1"), beløp = BigDecimal(800)),
                ),
            )

            resultat.erAvslag shouldBe false
            resultat.bidragTilFordeling.compareTo(BigDecimal(800)) shouldBe 0
        }

        @Test
        fun `sluttberegning skal gi avslag uten beløp når BPs andel av U er 50 prosent eller lavere`() {
            val resultat = EndeligBidragBeregningV2.beregnSluttberegningBarnebidrag(
                sluttberegningGrunnlag(andelDeltBosted = andelDeltBosted(faktor = BigDecimal.ZERO, beløp = BigDecimal.ZERO)),
            )

            resultat.bidragIkkeBeregnetDeltBosted shouldBe true
            resultat.beregnetBeløp shouldBe null
            resultat.resultatBeløp shouldBe null
        }

        @Test
        fun `sluttberegning skal beregne bidrag når BPs andel av U er over 50 prosent`() {
            val resultat = EndeligBidragBeregningV2.beregnSluttberegningBarnebidrag(
                sluttberegningGrunnlag(andelDeltBosted = andelDeltBosted(faktor = BigDecimal("0.1"), beløp = BigDecimal(800))),
            )

            resultat.bidragIkkeBeregnetDeltBosted shouldBe false
            resultat.resultatBeløp?.compareTo(BigDecimal(800)) shouldBe 0
        }
    }
}

private fun andelDeltBosted(faktor: BigDecimal, beløp: BigDecimal) = BidragspliktigesAndelDeltBostedDelberegningBeregningGrunnlag(
    referanse = "andelDeltBosted",
    bpAndelAvUVedDeltBostedFaktor = faktor,
    bpAndelAvUVedDeltBostedBeløp = beløp,
)

private fun sluttberegningGrunnlag(andelDeltBosted: BidragspliktigesAndelDeltBostedDelberegningBeregningGrunnlag?) = SluttberegningBarnebidragV2BeregningGrunnlag(
    bidragJustertForBPBarnetilleggBeregningGrunnlag = BidragJustertForBPBarnetilleggDelberegningBeregningGrunnlag(
        referanse = "bidragJustertForBPBarnetillegg",
        bidragJustertForNettoBarnetilleggBP = andelDeltBosted?.bpAndelAvUVedDeltBostedBeløp ?: BigDecimal.ZERO,
    ),
    samværsfradragBeregningGrunnlag = SamværsfradragDelberegningBeregningGrunnlag(referanse = "samværsfradrag", beløp = BigDecimal.ZERO),
    bpAndelUnderholdskostnadBeregningGrunnlag = BpAndelUnderholdskostnadDelberegningBeregningGrunnlag(
        referanse = "bpAndelUnderholdskostnad",
        andelBeløp = BigDecimal(4800),
        andelFaktor = BigDecimal("0.6"),
        barnetErSelvforsørget = false,
    ),
    søknadsbarnetBorHosBpGrunnlag = SøknadsbarnetBorHosBpGrunnlag(referanse = "søknadsbarnetBorHosBp", søknadsbarnetBorHosBp = false),
    bidragspliktigesAndelDeltBostedBeregningGrunnlag = andelDeltBosted,
)

private fun andelAvBidragsevneGrunnlag(bidragTilFordeling: BigDecimal?, sumBidragTilFordeling: BigDecimal, evne: BigDecimal) = AndelAvBidragsevneBeregningGrunnlag(
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
) = BidragTilFordelingBeregningGrunnlag(
    underholdskostnadBeregningGrunnlag = UnderholdskostnadDelberegningBeregningGrunnlag(referanse = "underholdskostnad", beløp = underholdskostnad),
    bpAndelUnderholdskostnadBeregningGrunnlag = BpAndelUnderholdskostnadDelberegningBeregningGrunnlag(
        referanse = "bpAndelUnderholdskostnad",
        andelBeløp = bpAndel,
        andelFaktor = BigDecimal("0.5"),
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
        andelDeltBosted(faktor = BigDecimal("0.1"), beløp = bidragEtterFordeling)
    } else {
        null
    },
)
