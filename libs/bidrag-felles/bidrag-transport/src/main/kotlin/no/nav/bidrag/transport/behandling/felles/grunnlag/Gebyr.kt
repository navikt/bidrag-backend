package no.nav.bidrag.transport.behandling.felles.grunnlag

import no.nav.bidrag.domene.enums.grunnlag.Grunnlagstype
import no.nav.bidrag.domene.enums.sjablon.SjablonTallNavn
import java.math.BigDecimal

data class ManueltOverstyrtGebyr(
    val ilagtGebyr: Boolean,
    val begrunnelse: String,
) : GrunnlagInnhold

data class DelberegningInnteksbasertGebyr(
    val ileggesGebyr: Boolean,
    val sumInntekt: BigDecimal,
) : GrunnlagInnhold

data class SluttberegningGebyr(
    val ilagtGebyr: Boolean,
) : GrunnlagInnhold

val List<BaseGrunnlag>.gebyrDelberegningSumInntekt get() =
    find {
        it.type == Grunnlagstype.SLUTTBEREGNING_GEBYR
    }?.let {
        finnGrunnlagSomErReferertAv(Grunnlagstype.DELBEREGNING_SUM_INNTEKT, it)
    }?.toList()
        ?.innholdTilObjekt<DelberegningSumInntekt>()
        ?.maxByOrNull { it.barnetillegg ?: BigDecimal.ZERO }

val List<BaseGrunnlag>.sluttberegningGebyr get() =
    find {
        it.type == Grunnlagstype.SLUTTBEREGNING_GEBYR
    }?.tilInnholdMedReferanse<SluttberegningGebyr>()

val List<BaseGrunnlag>.gebyrBeløp get() =
    filter {
        it.type == Grunnlagstype.SJABLON_SJABLONTALL
    }.innholdTilObjekt<SjablonSjablontallPeriode>()
        .find { it.sjablon == SjablonTallNavn.FASTSETTELSESGEBYR_BELØP }
        ?.verdi
