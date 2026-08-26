package no.nav.bidrag.transport.dokument

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Metadata til en respons etter journalpost ble opprettet")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OpprettJournalpostResponse(
    @Schema(description = "Journalpostid på journalpost som ble opprettet")
    val journalpostId: String? = null,
    @Schema(description = "Liste med dokumenter som er knyttet til journalposten")
    val dokumenter: List<OpprettDokumentDto> = emptyList(),
)
