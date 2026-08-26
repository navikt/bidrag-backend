package no.nav.bidrag.transport.dokument

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Metadata for dokument som skal knyttes til journalpost")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OpprettDokumentDto(
    @Schema(description = "Dokumentets tittel")
    val tittel: String = "",
    @Schema(description = "Typen dokument. Brevkoden sier noe om dokumentets innhold og oppbygning.")
    @Deprecated("Erstatt med dokumentmalId", ReplaceWith("dokumentmalId"))
    val brevkode: String? = null,
    @Schema(description = "Typen dokument. Dokumentmal sier noe om dokumentets innhold og oppbygning.")
    val dokumentmalId: String? = brevkode,
    @Schema(description = "Referansen til dokumentet hvis det er lagret i et annet arkivsystem")
    val dokumentreferanse: String? = null,
    @Schema(description = "Selve PDF dokumentet formatert som Base64", deprecated = true)
    @Deprecated("Erstatt med fysiskDokument", ReplaceWith("fysiskDokument"))
    val dokument: String? = null,
    @Schema(description = "Selve PDF dokumentet formatert som Base64")
    val fysiskDokument: ByteArray? = dokument?.toByteArray(),
) {
    override fun toString(): String =
        "(tittel=$tittel, brevkode=$brevkode, dokumentmalId=$dokumentmalId, dokumentreferanse=$dokumentreferanse, " +
            "fysiskDokument(lengde)=${fysiskDokument?.size}"
}
