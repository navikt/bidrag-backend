package no.nav.bidrag.transport.dokument

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Metadata til en respons etter journalpost med tilhørende data")
data class JournalpostResponse(
    @Schema(description = "journalposten som er etterspurt")
    val journalpost: JournalpostDto? = null,
    @Schema(description = "alle saker som journalposten er tilknyttet")
    val sakstilknytninger: List<String> = emptyList(),
)
