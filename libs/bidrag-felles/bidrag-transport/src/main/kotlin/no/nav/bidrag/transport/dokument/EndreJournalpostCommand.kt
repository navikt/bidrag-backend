package no.nav.bidrag.transport.dokument

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Metadata for endring av en journalpost")
data class EndreJournalpostCommand(
    @Schema(description = "Identifikator av journalpost")
    val journalpostId: String? = null,
    @Schema(description = "Avsenders navn (med eventuelt fornavn bak komma)", deprecated = true)
    @Deprecated("Erstatt med avsenderMottakerNavn", ReplaceWith("avsenderMottakerNavn"))
    val avsenderNavn: String? = null,
    @Schema(description = "Avsender/Mottakers navn (med eventuelt fornavn bak komma)")
    val avsenderMottakerNavn: String? = avsenderNavn,
    @Schema(description = "Avsender/Mottakers id")
    val avsenderMottakerId: String? = null,
    @Schema(description = "Kort oppsummert av journalført innhold")
    val beskrivelse: String? = null,
    @Schema(description = "Dato for dokument i journalpost")
    val dokumentDato: LocalDate? = null,
    @Schema(description = "Fnr/dnr/bostnr eller orgnr for hvem/hva dokumentet gjelder")
    val gjelder: String? = null,
    @Schema(description = "Dato dokument ble journalført")
    val journaldato: LocalDate? = null,
    @Schema(description = "Saksnummer til bidragsaker som journalpost skal tilknyttes")
    val tilknyttSaker: List<String> = emptyList(),
    @Schema(description = "En liste over endringer i dokumenter")
    val endreDokumenter: List<EndreDokument> = emptyList(),
    @Schema(description = "Behandlingstema")
    val behandlingstema: String? = null,
    @Schema(description = "Endre fagområde")
    val fagomrade: String? = null,
    @Schema(description = "Type ident for gjelder: FNR, ORGNR, AKTOERID", enumAsRef = true)
    val gjelderType: IdentType? = null,
    @Schema(description = "Tittel på journalposten")
    val tittel: String? = null,
    @Schema(description = "Skal journalposten journalføres aka. registreres")
    val skalJournalfores: Boolean = false,
    @Schema(description = "Liste med retur detaljer som skal endres")
    val endreReturDetaljer: List<EndreReturDetaljer> = listOf(),
) {
    @Suppress("unused")
    fun manglerGjelder() = gjelder == null
}

@Schema(description = "Metadata for endring av et retur detalj")
data class EndreReturDetaljer(
    @Schema(description = "Dato på retur detaljer som skal endres")
    val originalDato: LocalDate? = null,
    @Schema(description = "Ny dato på retur detaljer")
    val nyDato: LocalDate? = null,
    @Schema(description = "Beskrivelse av retur (eks. addresse forsøkt sendt)")
    val beskrivelse: String,
)

@Schema(description = "Metadata for endring av et dokument")
data class EndreDokument(
    @Schema(description = "Brevkoden til dokumentet")
    val brevkode: String? = null,
    @Schema(description = "Identifikator av dokument informasjon", deprecated = true)
    val dokId: String? = null,
    @Schema(description = "Identifikator til dokumentet")
    val dokumentreferanse: String? = dokId,
    @Schema(description = "Tittel på dokumentet")
    val tittel: String? = null,
)
