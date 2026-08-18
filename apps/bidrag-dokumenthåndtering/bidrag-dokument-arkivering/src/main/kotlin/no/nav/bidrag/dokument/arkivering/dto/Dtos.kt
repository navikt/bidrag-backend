package no.nav.bidrag.dokument.arkivering.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.bidrag.transport.dokument.Avvikshendelse
import no.nav.bidrag.transport.dokument.JournalpostResponse

class ArkivereJournalpostResponse private constructor(
    val jpIdBidrag: String? = null,
    val jpIdJoark: String? = null,
    val journalstatus: String? = null,
    val melding: String? = null,
    val journalpostFerdigstilt: Boolean? = null,
    val dokumentInfo: List<DokumentInfo>? = emptyList(),
) {
    constructor() : this(null)
    constructor(jpIdBidrag: String, jpResponse: JournalpostResponse) : this(
        jpIdBidrag = jpIdBidrag,
        jpIdJoark = jpResponse.journalpost?.joarkJournalpostId,
        journalpostFerdigstilt = true,
    )
    constructor(opprettJournalpostResponse: OpprettJournalpostResponse, jpIdBidrag: String?) : this(
        jpIdBidrag = jpIdBidrag,
        jpIdJoark = opprettJournalpostResponse.journalpostId,
        journalstatus = opprettJournalpostResponse.journalstatus,
        melding = opprettJournalpostResponse.melding,
        journalpostFerdigstilt = opprettJournalpostResponse.journalpostferdigstilt,
        dokumentInfo = opprettJournalpostResponse.dokumenter,
    )

    data class Builder(
        var jpIdBidrag: String? = null,
        var jpIdJoark: String? = null,
        var journalstatus: String? = null,
        var melding: String? = null,
        var journalpostFerdigstilt: Boolean? = null,
        var dokumentInfo: List<DokumentInfo> = emptyList(),
    ) {
        fun jpIdBidrag(jpIdBidrag: String?) = apply { this.jpIdBidrag = jpIdBidrag }

        fun jpIdJoark(jpIdJoark: String?) = apply { this.jpIdJoark = jpIdJoark }

        fun journalstatus(journalstatus: String?) = apply { this.journalstatus = journalstatus }

        fun melding(melding: String?) = apply { this.melding = melding }

        fun journalpostFerdigstilt(journalpostFerdigstilt: Boolean?) = apply {
            this.journalpostFerdigstilt = journalpostFerdigstilt
        }

        fun dokumentInfo(dokumentInfo: List<DokumentInfo> = emptyList()) = apply {
            this.dokumentInfo = dokumentInfo
        }

        fun build() = ArkivereJournalpostResponse(
            jpIdBidrag,
            jpIdJoark,
            journalstatus,
            melding,
            journalpostFerdigstilt,
            dokumentInfo,
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class SecurityTokenServiceResponse(
    @JsonProperty("access_token")
    val idToken: String? = null,
    @JsonProperty("token_type")
    val tokenType: String? = null,
    @JsonProperty("expires_in")
    val expiresIn: String? = null,
) {
    constructor() : this(null)
}

data class BrevklientCredentials(
    val brevklientUsername: String? = null,
    val brevklientPassword: String? = null,
)

data class AvvikHendelseIntern(
    val joarkJournalpostId: String? = null,
    val saksnummer: String,
) {
    enum class JournalStatusIntern {
        FULLFORT,
        FEILET,
        STARTET,
    }

    private fun createInitialAvvikHendelse(): Avvikshendelse = Avvikshendelse(
        saksnummer = saksnummer,
        avvikType = Companion.AVVIK_TYPE_ARKIVER,
    )

    fun toAvvikHendelseSettStatusEkspedert(): Avvikshendelse {
        val details = mutableMapOf<String, String>()
        details[AVVIK_DETAIL_UTSENDINGSKANAL] = "L"
        details[AVVIK_DETAIL_SETT_STATUS_EKSPEDERT] = "true"

        return createInitialAvvikHendelse()
            .copy(
                avvikType = Companion.AVVIK_TYPE_OPPDATER_DISTRIBUSJONSINFO,
                detaljer = details,
            )
    }

    fun toAvvikHendelseArkiverStartet(): Avvikshendelse {
        val details = mutableMapOf<String, String>()
        details[AVVIK_DETAIL_JOARK_STATUS] = JournalStatusIntern.STARTET.name
        return createInitialAvvikHendelse().copy(
            detaljer = details,
        )
    }

    fun toAvvikHendelseArkiverFullfort(): Avvikshendelse {
        val details = mutableMapOf<String, String>()
        details[AVVIK_DETAIL_JOARK_JP_ID] = joarkJournalpostId!!
        details[AVVIK_DETAIL_JOARK_STATUS] = JournalStatusIntern.FULLFORT.name
        return createInitialAvvikHendelse().copy(
            detaljer = details,
        )
    }

    fun toAvvikHendelseArkiverFeilet(): Avvikshendelse {
        val details = HashMap<String, String>()
        details[AVVIK_DETAIL_JOARK_STATUS] = JournalStatusIntern.FEILET.name
        return createInitialAvvikHendelse().copy(
            detaljer = details,
        )
    }

    companion object {
        const val AVVIK_DETAIL_JOARK_STATUS = "joarkArkiveringStatus"
        const val AVVIK_DETAIL_JOARK_JP_ID = "joarkJournalpostId"
        const val AVVIK_DETAIL_UTSENDINGSKANAL = "utsendingsKanal"
        const val AVVIK_DETAIL_SETT_STATUS_EKSPEDERT = "settStatusEkspedert"
        private const val AVVIK_TYPE_ARKIVER = "ARKIVERE_JOURNALPOST"
        private const val AVVIK_TYPE_OPPDATER_DISTRIBUSJONSINFO = "OPPDATER_DISTRIBUSJONSINFO"
    }
}

data class ArkiverDecision(
    var kanArkivere: Boolean,
    var reason: String? = null,
)

object JournalpostStatus {
    const val EKSPEDERT = "E"
    const val EKSPEDERT_JOARK = "EJ"
    const val KLAR_TIL_PRINT = "KP"
}
