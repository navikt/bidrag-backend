package no.nav.bidrag.transport.behandling.vedtak.request

import io.swagger.v3.oas.annotations.media.Schema
// import jakarta.validation.constraints.Min

@Schema
data class OpprettPeriodeGrunnlagRequestDto(
    //    @Min(0)
    @Schema(description = "Periode-id")
    val periodeId: Int,
    //    @Min(0)
    @Schema(description = "grunnlag-id")
    val grunnlagId: Int,
)
