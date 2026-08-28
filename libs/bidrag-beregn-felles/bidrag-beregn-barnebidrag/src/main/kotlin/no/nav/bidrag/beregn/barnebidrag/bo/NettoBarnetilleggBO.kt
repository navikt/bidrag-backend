package no.nav.bidrag.beregn.barnebidrag.bo

import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.Barnetillegg
import no.nav.bidrag.transport.behandling.felles.grunnlag.BarnetilleggPeriode
import java.math.BigDecimal

data class NettoBarnetilleggPeriodeGrunnlag(
    val beregningsperiode: ÅrMånedsperiode,
    val barnetilleggPeriodeGrunnlagListe: List<BarnetilleggPeriodeGrunnlag>,
)

data class BarnetilleggPeriodeGrunnlag(val referanse: String, val barnetilleggPeriode: BarnetilleggPeriode)

data class NettoBarnetilleggPeriodeResultat(val periode: ÅrMånedsperiode, val resultat: NettoBarnetilleggBeregningResultat)

data class NettoBarnetilleggBeregningGrunnlag(val barnetilleggBeregningGrunnlagListe: List<BarnetilleggBeregningGrunnlag>)

data class BarnetilleggBeregningGrunnlag(
    val referanse: String,
    val barnetilleggstype: Inntektstype,
    val bruttoBarnetillegg: BigDecimal,
    val skattefaktor: BigDecimal?,
)

data class NettoBarnetilleggBeregningResultat(
    val summertBruttoBarnetillegg: BigDecimal,
    val summertNettoBarnetillegg: BigDecimal,
    val barnetilleggTypeListe: List<Barnetillegg>,
    val grunnlagsreferanseListe: List<String>,
)
