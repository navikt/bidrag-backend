package no.nav.bidrag.transport.behandling.felles.grunnlag

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.inntekt.Inntektstype
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.math.BigDecimal

@Schema(description = "Barnetillegg for person")
data class BarnetilleggPeriode(
    override val periode: ÅrMånedsperiode,
    val type: Inntektstype,
    val beløp: BigDecimal,
    val skattefaktor: BigDecimal? = null,
    override val manueltRegistrert: Boolean,
) : GrunnlagPeriodeInnhold {
    constructor(
        periode: ÅrMånedsperiode,
        type: Inntektstype,
        beløp: BigDecimal,
        manueltRegistrert: Boolean,
    ) : this(periode, type, beløp, null, manueltRegistrert)
}

data class DelberegningBarnetilleggSkattesats(
    override val periode: ÅrMånedsperiode,
    val skattFaktor: BigDecimal,
    val minstefradrag: BigDecimal,
    val skattAlminneligInntekt: BigDecimal,
    val trygdeavgift: BigDecimal,
    val trinnskatt: BigDecimal,
    val sumSkatt: BigDecimal,
    val sumInntekt: BigDecimal,
) : Delberegning

data class DelberegningNettoBarnetillegg(
    override val periode: ÅrMånedsperiode,
    val summertBruttoBarnetillegg: BigDecimal,
    val summertNettoBarnetillegg: BigDecimal,
    val barnetilleggTypeListe: List<Barnetillegg>,
) : Delberegning

data class Barnetillegg(
    val barnetilleggType: Inntektstype,
    val bruttoBarnetillegg: BigDecimal,
    val nettoBarnetillegg: BigDecimal,
    val skattefaktor: BigDecimal? = null,
) {
    constructor(
        barnetilleggType: Inntektstype,
        bruttoBarnetillegg: BigDecimal,
        nettoBarnetillegg: BigDecimal,
    ) : this(barnetilleggType, bruttoBarnetillegg, nettoBarnetillegg, null)
}
