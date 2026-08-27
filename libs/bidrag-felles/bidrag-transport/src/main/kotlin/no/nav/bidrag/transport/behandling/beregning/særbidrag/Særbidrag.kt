package no.nav.bidrag.transport.behandling.beregning.særbidrag

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.beregning.Resultatkode
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.felles.grunnlag.GrunnlagDto
import java.math.BigDecimal

// Resultat
data class BeregnetSærbidragResultat(
    @Schema(description = "Periodisert liste over resultat av særbidragsberegning")
    var beregnetSærbidragPeriodeListe: List<ResultatPeriode> = emptyList(),
    @Schema(description = "Liste over grunnlag brukt i beregning") var grunnlagListe: List<GrunnlagDto> = emptyList(),
)

@Schema(description = "Resultatet av en beregning for en gitt periode")
data class ResultatPeriode(
    @Schema(description = "Beregnet resultat periode") var periode: ÅrMånedsperiode,
    @Schema(description = "Beregnet resultat innhold") var resultat: ResultatBeregning,
    @Schema(description = "Beregnet grunnlag innhold") var grunnlagsreferanseListe: List<String>,
)

@Schema(description = "Resultatet av en beregning")
data class ResultatBeregning(
    @Schema(description = "Resultat beløp. Er null hvis resultatet er avslag") var beløp: BigDecimal?,
    @Schema(description = "Resultat kode") var resultatkode: Resultatkode,
)
