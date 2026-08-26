@file:Suppress("unused")

package no.nav.bidrag.transport.dokument

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "En avvikshendelse som kan utføres på en journalpost")
data class Avvikshendelse(
    @Schema(description = "Type avvik")
    val avvikType: String,
    @Schema(description = "Manuell beskrivelse av avvik")
    val beskrivelse: String? = null,
    @Schema(description = "Eventuelle detaljer som skal følge avviket")
    val detaljer: Map<String, String> = HashMap(),
    @Schema(description = "Saksnummer til sak når journalpost er journalført")
    val saksnummer: String? = null,
    @Schema(
        description =
            "Addresse som skal brukes ved bestilling av ny distribusjon av utgående journalpost. " +
                "Benyttes ved avvik BESTILL_NY_DISTRIBUSJON",
    )
    val adresse: DistribuerTilAdresse? = null,
    @Schema(description = "Dokumenter som brukes ved kopiering ny journalpost. Benyttes ved avvik KOPIER_FRA_ANNEN_FAGOMRADE")
    val dokumenter: List<DokumentDto>? = emptyList(),
) {
    constructor(avvikType: String, enhetsnummer: String) :
        this(avvikType, detaljer = mapOf(Pair("enhetsnummer", enhetsnummer)), beskrivelse = null, saksnummer = null, adresse = null)
    constructor(avvikType: String, enhetsnummer: String, saksnummer: String?) :
        this(avvikType, detaljer = mapOf(Pair("enhetsnummer", enhetsnummer)), beskrivelse = null, saksnummer = saksnummer, adresse = null)

    constructor(
        avvikType: AvvikType,
        beskrivelse: String? = null,
        detaljer: Map<String, String> = mapOf(),
        saksnummer: String? = null,
        adresse: DistribuerTilAdresse? = null,
        dokumenter: List<DokumentDto> = listOf(),
    ) : this(avvikType.name, beskrivelse, detaljer, saksnummer, adresse, dokumenter)

    fun hent(): AvvikType? = AvvikType.values().firstOrNull { it.name == avvikType }
}

@Schema(description = "Responsen til en avvikshendelse")
data class BehandleAvvikshendelseResponse(
    @Schema(description = "Type avvik")
    val avvikType: String,
    @Schema(description = "Oppgave id for oppgaven som ble opprettet på bakgrunn av avviket")
    val oppgaveId: Long? = null,
    @Schema(description = "Enhetsnummer til enheten som oppgaven er tildelt") val tildeltEnhetsnr: String? = null,
    @Schema(description = "Oppgavens tema") val tema: String? = null,
    @Schema(description = "Oppgavens type") val oppgavetype: String? = null,
) {
    constructor() : this("avvik ikke angitt")
    constructor(avvikType: AvvikType) : this(avvikType.name)
}

enum class AvvikType {
    ARKIVERE_JOURNALPOST,
    BESTILL_ORIGINAL,
    BESTILL_RESKANNING,
    BESTILL_SPLITTING,
    ENDRE_FAGOMRADE,

    @Deprecated("Bruk ENDRE_FAGOMRADE", ReplaceWith("ENDRE_FAGOMRADE"))
    SEND_TIL_FAGOMRADE,
    KOPIER_FRA_ANNEN_FAGOMRADE,
    SEND_KOPI_TIL_FAGOMRADE,
    FEILFORE_SAK,
    INNG_TIL_UTG_DOKUMENT,
    OVERFOR_TIL_ANNEN_ENHET,
    SLETT_JOURNALPOST,
    TREKK_JOURNALPOST,
    REGISTRER_RETUR,
    MANGLER_ADRESSE,
    BESTILL_NY_DISTRIBUSJON,
    FARSKAP_UTELUKKET,
}
