package no.nav.bidrag.transport.dokument

import io.swagger.v3.oas.annotations.media.Schema

data class DokumentMetadata(
    @Schema(description = "Journalpostid med arkiv prefiks som skal benyttes når dokumentet hentes") val journalpostId: String? = null,
    val dokumentreferanse: String? = null,
    val tittel: String? = null,
    @Schema(description = "Hvilken format dokument er på. Dette forteller hvordan dokumentet må åpnes.", enumAsRef = true)
    val format: DokumentFormatDto,
    @Schema(description = "Status på dokumentet", enumAsRef = true) val status: DokumentStatusDto,
    @Schema(description = "Hvilken arkivsystem dokumentet er lagret på", enumAsRef = true) val arkivsystem: DokumentArkivSystemDto,
)

enum class DokumentFormatDto {
    PDF,
    MBDOK,
    HTML,
}
