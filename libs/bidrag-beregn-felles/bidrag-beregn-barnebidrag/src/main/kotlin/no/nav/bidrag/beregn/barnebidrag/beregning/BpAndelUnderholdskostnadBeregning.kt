package no.nav.bidrag.beregn.barnebidrag.beregning

import no.nav.bidrag.beregn.barnebidrag.bo.BpAndelUnderholdskostnadBeregningGrunnlag
import no.nav.bidrag.beregn.barnebidrag.bo.BpAndelUnderholdskostnadBeregningResultat
import no.nav.bidrag.domene.enums.sjablon.SjablonTallNavn
import no.nav.bidrag.domene.util.avrundetMedNullDesimaler
import no.nav.bidrag.domene.util.avrundetMedTiDesimaler
import no.nav.bidrag.domene.util.avrundetMedToDesimaler
import java.math.BigDecimal
import java.math.RoundingMode

internal object BpAndelUnderholdskostnadBeregning {

    private val bigDecimalFemSjettedeler = BigDecimal.valueOf(0.833333333333)

    fun beregn(grunnlag: BpAndelUnderholdskostnadBeregningGrunnlag): BpAndelUnderholdskostnadBeregningResultat {
        // Henter sjablonverdier
        val sjablonverdier = hentSjablonverdier(grunnlag)

        var endeligAndelFaktor = BigDecimal.ZERO.setScale(10)
        var andelBeløp = BigDecimal.ZERO.setScale(2)
        var beregnetAndelFaktor = BigDecimal.ZERO.setScale(10)

        // Legger sammen inntektene
        val inntektBP = grunnlag.inntektBPBeregningGrunnlag.sumInntekt
        val inntektBM = grunnlag.inntektBMBeregningGrunnlag.sumInntekt
        val inntektSB = grunnlag.inntektSBBeregningGrunnlag.sumInntekt
        var barnEndeligInntekt = inntektSB

        // Tester om barnets inntekt er minst maksInntektBBMultiplikator (sjablon 0012) ganger sats for forhøyet forskudd. I så fall skal ikke
        // BPs andel regnes ut.
        val barnetErSelvforsørget = (inntektSB >= (sjablonverdier.forskuddssatsBeløp * sjablonverdier.maksInntektBBMultiplikator))

        if (!barnetErSelvforsørget) {
            // Barnets inntekt reduseres med inntektBBMultiplikator (sjablon 0010) ganger sats for forhøyet forskudd
            barnEndeligInntekt =
                (inntektSB - sjablonverdier.forskuddssatsBeløp * sjablonverdier.inntektBBMultiplikator).coerceAtLeast(BigDecimal.ZERO)

            val sumInntekt = inntektBP + inntektBM + barnEndeligInntekt
            beregnetAndelFaktor = if (sumInntekt.avrundetMedNullDesimaler != BigDecimal.ZERO) {
                inntektBP.divide(sumInntekt, 10, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

            endeligAndelFaktor = beregnetAndelFaktor.coerceAtMost(bigDecimalFemSjettedeler)
            andelBeløp = grunnlag.underholdskostnadBeregningGrunnlag.beløp * endeligAndelFaktor
        }

        return BpAndelUnderholdskostnadBeregningResultat(
            endeligAndelFaktor = endeligAndelFaktor.avrundetMedTiDesimaler,
            andelBeløp = andelBeløp.avrundetMedToDesimaler,
            beregnetAndelFaktor = beregnetAndelFaktor.avrundetMedTiDesimaler,
            barnEndeligInntekt = barnEndeligInntekt.avrundetMedToDesimaler,
            barnetErSelvforsørget = barnetErSelvforsørget,
            grunnlagsreferanseListe = listOf(
                grunnlag.inntektBPBeregningGrunnlag.referanse,
                grunnlag.inntektBMBeregningGrunnlag.referanse,
                grunnlag.inntektSBBeregningGrunnlag.referanse,
                grunnlag.underholdskostnadBeregningGrunnlag.referanse,
            )
                .filter { it.isNotBlank() } +
                grunnlag.sjablonSjablontallBeregningGrunnlagListe.map { it.referanse },

        )
    }

    private fun hentSjablonverdier(grunnlag: BpAndelUnderholdskostnadBeregningGrunnlag): Sjablonverdier {
        fun finnVerdi(sjablon: SjablonTallNavn) = grunnlag.sjablonSjablontallBeregningGrunnlagListe
            .firstOrNull { it.type == sjablon.navn }
            ?.verdi
            ?.toBigDecimal()

        return Sjablonverdier(
            forskuddssatsBeløp = finnVerdi(SjablonTallNavn.FORSKUDDSSATS_BELØP) ?: BigDecimal.ZERO,
            inntektBBMultiplikator = finnVerdi(SjablonTallNavn.INNTEKT_BB_MULTIPLIKATOR)
                ?: error("Mangler sjablon ${SjablonTallNavn.INNTEKT_BB_MULTIPLIKATOR.navn} i grunnlaget"),
            maksInntektBBMultiplikator = finnVerdi(SjablonTallNavn.MAKS_INNTEKT_BB_MULTIPLIKATOR)
                ?: error("Mangler sjablon ${SjablonTallNavn.MAKS_INNTEKT_BB_MULTIPLIKATOR.navn} i grunnlaget"),
        )
    }

    private data class Sjablonverdier(
        val forskuddssatsBeløp: BigDecimal,
        val inntektBBMultiplikator: BigDecimal,
        val maksInntektBBMultiplikator: BigDecimal,
    )
}
