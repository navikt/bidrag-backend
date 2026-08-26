package no.nav.bidrag.transport.dokument

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.time.LocalDateTime

enum class HendelseType {
    JOURNALFORING,
    ENDRING,
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class JournalpostHendelse(
    val journalpostId: String = "-1",
    val aktorId: String? = null,
    val fnr: String? = null,
    val behandlingstema: String? = null,
    val tittel: String? = null,
    @Deprecated("Bruk tema istedenfor", ReplaceWith("tema"))
    val fagomrade: String? = null,
    val tema: String? = fagomrade,
    val batchId: String? = null,
    val journalposttype: String? = null,
    val hendelseType: HendelseType? = null,
    val enhet: String? = null,
    @Deprecated("Bruk status istedenfor", ReplaceWith("status"))
    val journalstatus: String? = null,
    val status: JournalpostStatus? = JournalpostStatus.fraKode(journalstatus),
    val sporing: Sporingsdata? = null,
    val sakstilknytninger: List<String> = emptyList(),
    val dokumentDato: LocalDate? = null,
    val journalfortDato: LocalDate? = null,
    val distribuertTidspunkt: LocalDateTime? = null,
    val journalpostIdFagarkiv: String? = null,
) {
    fun erHendelseTypeJournalforing() = hendelseType == HendelseType.JOURNALFORING

    fun harEnhet() = enhet != null

    fun harAktorId() = aktorId != null

    fun hentJournalposttype(): JournalpostType? =
        when (journalposttype) {
            JournalpostType.UTGÅENDE.name, DokumentType.UTGÅENDE -> JournalpostType.UTGÅENDE
            JournalpostType.NOTAT.name, DokumentType.NOTAT -> JournalpostType.NOTAT
            JournalpostType.INNGÅENDE.name, DokumentType.INNGÅENDE -> JournalpostType.INNGÅENDE
            else -> null
        }

    fun harJournalpostIdPrefix() = journalpostId.contains("-")

    fun harJournalpostId() = journalpostId != "-1"

    fun hentJournalpostIdNumerisk(): Long = JournalpostId(journalpostId).idNumerisk!!

    fun erBidragJournalpost() = JournalpostId(journalpostId).erSystemBidrag

    fun erJoarkJournalpost() = JournalpostId(journalpostId).erSystemJoark

    fun erForsendelse() = JournalpostId(journalpostId).erSystemForsendelse

    fun erInngående() = hentJournalposttype() == JournalpostType.INNGÅENDE

    fun erUtgående() = hentJournalposttype() == JournalpostType.UTGÅENDE

    fun hentEndretAvEnhetsnummer() = sporing?.enhetsnummer ?: enhet

    fun hentSaksbehandlerInfo() = sporing?.lagSaksbehandlerInfo() ?: "ukjent saksbehandler"

    fun printSummary() =
        "{journalpostId=$journalpostId," +
            "tema=$tema," +
            "enhet=$enhet," +
            "saksbehandlerEnhet=${sporing?.enhetsnummer}," +
            "journalstatus=$status," +
            "saker=$sakstilknytninger," +
            "dokumentDato=$dokumentDato," +
            "journalfortDato=$journalfortDato," +
            "tittel=$tittel," +
            "behandlingstema=$behandlingstema....}"
}

data class Sporingsdata(
    val correlationId: String? = null,
    val brukerident: String? = null,
    val saksbehandlersNavn: String? = "ukjent saksbehandler",
    val enhetsnummer: String? = null,
) {
    fun lagSaksbehandlerInfo(saksbehandlerEnhet: String? = null) =
        if (brukerident == null && saksbehandlersNavn == null) {
            "ukjent saksbehandler"
        } else {
            hentBrukeridentMedSaksbehandler(
                saksbehandlerEnhet ?: enhetsnummer ?: "ukjent enhet",
            )
        }

    private fun hentBrukeridentMedSaksbehandler(enhetsnummer: String) =
        "${saksbehandlersNavn ?: "Ukjent"} ${brukerident?.let { "($it, $enhetsnummer)" } ?: "($enhetsnummer)"}"
}
