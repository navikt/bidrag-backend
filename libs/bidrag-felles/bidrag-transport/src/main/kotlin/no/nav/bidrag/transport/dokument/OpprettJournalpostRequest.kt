package no.nav.bidrag.transport.dokument

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Metadata for opprettelse av journalpost")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OpprettJournalpostRequest(
    @Schema(
        description =
            "Om journalposten skal journalføres etter opprettelse. " +
                "Journalføring betyr at journalpost låses for framtidige endringer",
    )
    val skalFerdigstilles: Boolean = false,
    @Schema(description = "Tittel på journalposten (Tittel settes til hoveddokumentes tittel for Joark journalposter)", deprecated = true)
    @Deprecated("Slutt å bruke")
    val tittel: String? = null,
    @Schema(description = "Bruker som journalposten gjelder")
    val gjelder: AktorDto? = null,
    @Schema(description = "Ident til brukeren som journalposten gjelder")
    val gjelderIdent: String? = null,
    val avsenderMottaker: AvsenderMottakerDto? = null,
    @Schema(
        description = """
    Dokumenter som skal knyttes til journalpost. 
    En journalpost må minst ha et dokument. 
    Det første dokument i meldingen blir tilknyttet som hoveddokument på journalposten.""",
        required = true,
    )
    val dokumenter: List<OpprettDokumentDto> = emptyList(),
    @Schema(description = "Saksnummer til bidragsaker som journalpost skal tilknyttes")
    val tilknyttSaker: List<String> = emptyList(),
    @Schema(description = "Behandlingstema")
    val behandlingstema: String? = null,
    @Schema(description = "Dato journalposten mottatt. Kan settes for inngående journalposter. Settes til i dag som default hvis ikke satt")
    val datoMottatt: LocalDateTime? = null,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Dato når selve dokumentet ble opprettet")
    val datoDokument: LocalDateTime? = null,
    @Schema(description = "Type kanal som benyttes ved mottak/utsending av journalpost", enumAsRef = true)
    val kanal: MottakUtsendingKanal? = null,
    @Schema(
        description = "Tema (Gyldige verdier er FAR og BID). Hvis det ikke settes opprettes journalpost med tema BID",
        defaultValue = "BID",
    )
    val tema: String = "BID",
    @Schema(description = "Journalposttype, dette kan enten være Inngående, Utgående eller Notat", required = true, enumAsRef = true)
    val journalposttype: JournalpostType,
    @Schema(
        description =
            "Referanse for journalpost. " +
                "Hvis journalpost med samme referanse finnes vil tjenesten gå videre uten å opprette journalpost. " +
                "Kan brukes for å lage løsninger idempotent",
    )
    val referanseId: String? = null,
    @Schema(description = "NAV-enheten som oppretter journalposten", deprecated = true)
    @Deprecated("Bruk journalførendeEnhet", ReplaceWith("journalførendeEnhet"))
    val journalfoerendeEnhet: String? = null,
    @Schema(description = "NAV-enheten som oppretter journalposten")
    val journalførendeEnhet: String? = null,
    @Schema(
        description =
            "Ident til saksbehandler som oppretter journalpost. " +
                "Dette vil prioriteres over ident som tilhører tokenet til kallet.",
    )
    val saksbehandlerIdent: String? = null,
    val ettersendingsoppgave: OpprettEttersendingsppgaveDto? = null,
)
