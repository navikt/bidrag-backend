package no.nav.bidrag.beregn.barnebidrag.beregning

import no.nav.bidrag.beregn.barnebidrag.bo.EndringSjekkGrenseBeregningResultat
import no.nav.bidrag.beregn.barnebidrag.bo.EndringSjekkGrensePeriodeDelberegningBeregningGrunnlag

internal object EndringSjekkGrenseBeregning {

    // Hvis minst en av endringene i grunnlaget er over grense settes resultatet til true.
    // Hvis alle løpende beløp er null og alle beregnede beløp er null eller 0 settes resultatet til true (antar da at beløpshistorikk mangler og
    // at det er avslag i hele beregningsperioden).
    // I alle andre tilfeller settes resultatet til false.
    fun beregn(grunnlag: List<EndringSjekkGrensePeriodeDelberegningBeregningGrunnlag>): List<EndringSjekkGrenseBeregningResultat> {
        val endringErOverGrense = grunnlag.any { it.endringErOverGrense } ||
            (
                grunnlag.all { it.løpendeBidragBeløp == null } &&
                    grunnlag.all { it.beregnetBidragBeløp == null }
                )

        return listOf(
            EndringSjekkGrenseBeregningResultat(
                endringErOverGrense = endringErOverGrense,
                grunnlagsreferanseListe =
                grunnlag.map { it.referanse },
            ),
        )
    }

    // Hvis alle løpende beløp er null og alle beregnede beløp er null eller 0 settes resultatet til true (antar da at beløpshistorikk mangler og
    // at det er avslag i hele beregningsperioden).
    // Hvis endringen i grunnlaget i første periode er over grense settes hele resultatet til true.
    // Hvis endringen i grunnlaget i alle perioder er under grense settes hele resultatet til false.
    // Hvis det finnes minst en periode hvor endringen i grunnlaget er over grense og endringen i første periode er under grense:
    // - Det lages en resultatperiode som løper fram til første periode hvor endringen er over grense hvor resultatet settes til false
    // - For resterende resultatperiode settes resultatet til true
    fun beregnV2(grunnlag: List<EndringSjekkGrensePeriodeDelberegningBeregningGrunnlag>): List<EndringSjekkGrenseBeregningResultat> {
        val alleBeløpMangler = grunnlag.all { it.løpendeBidragBeløp == null } && grunnlag.all { it.beregnetBidragBeløp == null }
        val førsteFomPeriodeOverGrense = grunnlag.filter { it.endringErOverGrense }.map { it.periode.fom }.firstOrNull()
        val førsteFomPeriode = grunnlag.first().periode.fom

        return grunnlag.map {
            val endringErOverGrense = when {
                alleBeløpMangler -> true
                førsteFomPeriodeOverGrense == førsteFomPeriode -> true
                førsteFomPeriodeOverGrense == null -> false
                else -> !it.periode.fom.isBefore(førsteFomPeriodeOverGrense)
            }

            EndringSjekkGrenseBeregningResultat(
                periode = it.periode,
                endringErOverGrense = endringErOverGrense,
                grunnlagsreferanseListe = listOf(it.referanse),
            )
        }
    }
}
