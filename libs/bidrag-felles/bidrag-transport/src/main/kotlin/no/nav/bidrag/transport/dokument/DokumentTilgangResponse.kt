package no.nav.bidrag.transport.dokument

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Metadata for en url til et fysisk dokument")
data class DokumentTilgangResponse(
    @Schema(description = "url til et fysisk dokument") val dokumentUrl: String = "",
    @Schema(description = "type system som er ansvarlig for dokumentet, eks: BREVLAGER") val type: String = "",
)
