package no.nav.bidrag.transport.behandling.grunnlag.response

import io.swagger.v3.oas.annotations.media.Schema

data class TilleggsstønadGrunnlagDto(
    @Schema(description = "Id til personen som mottar tilleggsstønaden")
    val partPersonId: String,
    @Schema(description = "Indikator som viser om en person mottar tilleggsstønad")
    val harInnvilgetVedtak: Boolean,
)
