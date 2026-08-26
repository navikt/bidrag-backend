package no.nav.bidrag.transport.dokument

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.diverse.Språk
import java.time.LocalDateTime

@Schema(description = "Bestill distribusjon av journalpost")
data class DistribuerJournalpostRequest(
    @Schema(description = "Identifiserer batch som forsendelsen inngår i. Brukes for sporing") val batchId: String? = null,
    @Schema(
        description = "Forsendelsen er skrevet ut og distribuert lokalt. Distribusjon registreres men ingen distribusjon bestilles.",
    ) val lokalUtskrift: Boolean = false,
    @Schema(description = "Adresse for hvor brev sendes ved sentral print") val adresse: DistribuerTilAdresse? = null,
    val ettersendingsoppgave: OpprettEttersendingsppgaveDto? = null,
)

@Schema(description = "Respons etter bestilt distribusjon")
data class DistribuerJournalpostResponse(
    @Schema(description = "Journalpostid for dokument som det ble bestilt distribusjon for") val journalpostId: String,
    @Schema(
        description = "Bestillingid som unikt identifiserer distribusjonsbestillingen. Vil være null hvis ingen distribusjon er bestilt.",
    ) val bestillingsId: String?,
    val ettersendingsoppgave: OpprettEttersendingsoppgaveResponseDto? = null,
)

data class OpprettEttersendingsoppgaveResponseDto(
    val innsendingsId: String,
)

data class OpprettEttersendingsppgaveDto(
    val tittel: String,
    val skjemaId: String,
    val språk: Språk,
    val innsendingsFristDager: Int = 14,
    val vedleggsliste: List<OpprettEttersendingsoppgaveVedleggDto> = emptyList(),
)

data class OpprettEttersendingsoppgaveVedleggDto(
    val tittel: String? = null,
    val url: String? = null,
    val vedleggsnr: String,
)

@Schema(description = "Adresse for hvor brev sendes ved sentral print")
data class DistribuerTilAdresse(
    val adresselinje1: String? = null,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    @Schema(description = "ISO 3166-1 alpha-2 to-bokstavers landkode")
    val land: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DistribusjonInfoDto(
    @Schema(enumAsRef = true)
    val journalstatus: JournalpostStatus,
    val kanal: String,
    @Schema(enumAsRef = true)
    val utsendingsinfo: UtsendingsInfoDto? = null,
    val distribuertDato: LocalDateTime? = null,
    val distribuertAvIdent: String? = null,
    val bestillingId: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UtsendingsInfoDto(
    @Schema(enumAsRef = true)
    val varseltype: UtsendingsInfoVarselTypeDto? = null,
    val adresse: String? = null,
    val tittel: String? = null,
    val varslingstekst: String? = null,
)

enum class UtsendingsInfoVarselTypeDto {
    EPOST,
    SMS,
    DIGIPOST,
    FYSISK_POST,
}
