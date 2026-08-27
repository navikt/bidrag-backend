package no.nav.bidrag.transport.behandling.vedtak.request

import io.swagger.v3.oas.annotations.media.Schema
// import jakarta.validation.constraints.Min

@Schema
data class OpprettStønadsendringGrunnlagRequestDto(
    //    @Min(0)
    @Schema(description = "Stønadsendring-id")
    val stønadsendringId: Int,
    //    @Min(0)
    @Schema(description = "grunnlag-id")
    val grunnlagId: Int,
)
