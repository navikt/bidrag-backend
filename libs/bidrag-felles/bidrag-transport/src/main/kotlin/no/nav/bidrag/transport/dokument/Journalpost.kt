package no.nav.bidrag.transport.dokument

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Metadata til en journalpost")
data class JournalpostDto(
    @Schema(description = "Avsenders navn (med eventuelt fornavn bak komma)", deprecated = true)
    @Deprecated("bruk avsenderMottaker", ReplaceWith("avsenderMottaker.navn"))
    val avsenderNavn: String? = null,
    @Schema(description = "Informasjon om avsender eller mottaker")
    val avsenderMottaker: AvsenderMottakerDto? = avsenderNavn?.let { AvsenderMottakerDto(avsenderNavn) },
    @Schema(description = "Dokumentene som følger journalposten")
    val dokumenter: List<DokumentDto> = emptyList(),
    @Schema(description = "Dato for dokument i journalpost")
    val dokumentDato: LocalDate? = null,
    @Schema(description = "Tidspunkt for dokument i journalpost")
    val dokumentTidspunkt: LocalDateTime? = null,
    @Schema(description = "Dato dokumentene på journalposten ble sendt til bruker.")
    val ekspedertDato: LocalDate? = null,
    @Schema(description = "Fagområdet for journalposten. BID for bidrag og FAR for farskap")
    val fagomrade: String? = null,
    @Schema(description = "Ident for hvem/hva dokumente(t/ne) gjelder")
    val gjelderIdent: String? = null,
    @Schema(description = "Aktøren for hvem/hva dokumente(t/ne) gjelder")
    val gjelderAktor: AktorDto? = null,
    @Schema(description = "Kort oppsummert av journalført innhold")
    val innhold: String? = null,
    @Schema(description = "Enhetsnummer hvor journalføring ble gjort")
    val journalforendeEnhet: String? = null,
    @Schema(description = "Saksbehandler som var journalfører")
    val journalfortAv: String? = null,
    @Schema(description = "Dato dokument ble journalført")
    val journalfortDato: LocalDate? = null,
    @Schema(description = "Identifikator av journalpost i midlertidig brevlager eller fra joark på formatet [BID|JOARK]-<journalpostId>")
    val journalpostId: String? = null,
    @Schema(description = "Kanalen som er kilden til at journalposten ble registrert", deprecated = true, enumAsRef = true)
    @Deprecated("Ersatt med kanal", ReplaceWith("kanal"))
    val kilde: Kanal? = null,
    @Schema(description = "Kanalen journalposten ble mottatt i eller sendt ut på", enumAsRef = true)
    val kanal: Kanal? = null,
    @Schema(description = "Dato for når dokument er mottat, dvs. dato for journalføring eller skanning")
    val mottattDato: LocalDate? = null,
    @Schema(description = "Inngående (I), utgående (U) journalpost; (X) internt notat")
    val dokumentType: String? = null,
    @Deprecated("Bruk status istedenfor", replaceWith = ReplaceWith("status"))
    @Schema(description = "Journalpostens status, (A, D, J, M, O, R, S, T, U, KP, EJ, E)", deprecated = true)
    val journalstatus: String? = null,
    @Schema(description = "Journalpostens status", enumAsRef = true)
    val status: JournalpostStatus? = JournalpostStatus.fraKode(journalstatus),
    @Schema(description = "Om journalposten er feilført på bidragssak")
    val feilfort: Boolean? = null,
    @Schema(description = "Brevkoden til en journalpost")
    val brevkode: KodeDto? = null,
    @Schema(description = "Informasjon om returdetaljer til journalpost")
    val returDetaljer: ReturDetaljer? = null,
    @Schema(description = "Joark journalpostid for bidrag journalpost som er arkivert i Joark")
    val joarkJournalpostId: String? = null,
    @Schema(description = "Adresse som utgående journalpost var distribuert til ved sentral print")
    val distribuertTilAdresse: DistribuerTilAdresse? = null,
    @Schema(description = "Informasjon om returdetaljer til journalpost")
    val sakstilknytninger: List<String> = emptyList(),
    @Schema(description = "Språket til dokumentet i Journalposten")
    val språk: String? = null,
    @Schema(description = "Saksbehandler som opprettet journalposten")
    val opprettetAvIdent: String? = null,
    @Schema(
        description =
            "Referanse til originale kilden til journalposten. Kan være referanse til forsendelse " +
                "eller bidrag journalpost med prefiks. Feks BID_12323 eller BIF_123213",
    )
    val eksternReferanseId: String? = null,
    val ettersendingsppgave: EttersendingsppgaveDto? = null,
) {
    fun hentHoveddokument() = dokumenter.firstOrNull()

    fun hentTittel() = hentHoveddokument()?.tittel ?: innhold

    fun erFarskapUtelukket() = hentTittel()?.startsWith(FARSKAP_UTELUKKET_PREFIKS, ignoreCase = true) == true
}

data class EttersendingsppgaveDto(
    val tittel: String,
    val skjemaId: String,
    val innsendingsId: String?,
    val språk: String,
    val status: EttersendingsoppgaveStatus = EttersendingsoppgaveStatus.UKJENT,
    val opprettetDato: LocalDate?,
    val fristDato: LocalDate?,
    val slettesDato: LocalDate?,
    val vedleggsliste: List<EttersendingsoppgaveVedleggDto> = emptyList(),
)

data class EttersendingsoppgaveVedleggDto(
    val tittel: String? = null,
    val url: String? = null,
    val vedleggsnr: String,
    val status: EttersingdsoppgaveOpplastingsStatus = EttersingdsoppgaveOpplastingsStatus.UKJENT,
)

enum class EttersingdsoppgaveOpplastingsStatus {
    IKKE_VALGT,

    LASTET_OPP,

    INNSENDT,

    SEND_SENERE,

    SENDES_AV_ANDRE,

    SENDES_IKKE,

    LASTET_OPP_IKKE_RELEVANT_LENGER,

    LEVERT_DOKUMENTASJON_TIDLIGERE,

    HAR_IKKE_DOKUMENTASJONEN,

    NAV_KAN_HENTE_DOKUMENTASJON,

    UKJENT,
}

enum class EttersendingsoppgaveStatus {
    OPPRETTET,

    UTFYLT,

    INNSENDT,

    SLETTET_AV_BRUKER,

    AUTOMATISK_SLETTET,

    UKJENT,

    IKKE_OPPRETTET,
}

@Schema(
    description = """
Avsender journalposten ble sendt fra hvis utgående.
Mottaker journalposten skal sendes til hvis inngående.""",
)
data class AvsenderMottakerDto(
    @Schema(description = "Avsenders/Mottakers navn (med eventuelt fornavn bak komma). Skal ikke oppgis hvis ident er en FNR")
    val navn: String? = null,
    @Schema(description = "Person ident eller organisasjonsnummer")
    val ident: String? = null,
    @Schema(description = "Identtype", enumAsRef = true)
    val type: AvsenderMottakerDtoIdType = AvsenderMottakerDtoIdType.FNR,
    @Schema(description = "Adresse til mottaker hvis dokumentet skal sendes/er sendt gjennom sentral print")
    val adresse: MottakerAdresseTo? = null,
)

enum class AvsenderMottakerDtoIdType {
    FNR,
    SAMHANDLER,
    ORGNR,
    UTENLANDSK_ORGNR,
    UKJENT,
}

data class MottakerAdresseTo(
    val adresselinje1: String,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val bruksenhetsnummer: String? = null,
    @Schema(description = "Lankode må være i ISO 3166-1 alpha-2 format")
    val landkode: String? = null,
    @Schema(description = "Lankode må være i ISO 3166-1 alpha-3 format")
    val landkode3: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
)

@Schema(description = "Metadata for retur detaljer")
data class ReturDetaljer(
    @Schema(description = "Dato for siste retur")
    val dato: LocalDate? = null,
    @Schema(description = "Totalt antall returer")
    val antall: Int? = null,
    @Schema(description = "Liste med logg av alle registrerte returer")
    val logg: List<ReturDetaljerLog> = emptyList(),
)

@Schema(description = "Metadata for retur detaljer log")
data class ReturDetaljerLog(
    @Schema(description = "Dato for retur")
    val dato: LocalDate? = null,
    @Schema(description = "Beskrivelse av retur (eks. addresse forsøkt sendt)")
    val beskrivelse: String? = null,
    @Schema(description = "Returdetalje er låst for endring. Dette blir satt etter en ny distribusjon er bestilt")
    val locked: Boolean? = false,
)

@Schema(description = "Metadata om en aktør")
data class AktorDto(
    @Schema(description = "Identifaktor til aktøren")
    val ident: String = "",
    @Schema(description = "Hvilken identtype som skal brukes", enumAsRef = true)
    val type: IdentType? = null,
)

@Schema(description = "Identtypene til en aktør")
enum class IdentType {
    @Schema(description = "AktoerId (nav sitt løpenummer)")
    AKTOERID,

    @Schema(description = "Fødselsnummer")
    FNR,

    @Schema(description = "Organisasjonsnummer ")
    ORGNR,
}

@Schema(description = "Metadata for et dokument")
data class DokumentDto(
    @Schema(description = "Referansen til dokumentet i arkivsystemet")
    val dokumentreferanse: String? = null,
    @Schema(
        description =
            "Journalpost hvor dokumentet er arkivert. Dette brukes hvis dokumentet er arkivert i annen arkivsystem " +
                "enn det som er sendt med i forespørsel.",
    )
    val journalpostId: String? = null,
    @Schema(description = "Inngående (I), utgående (U) dokument, (X) internt notat", deprecated = true)
    val dokumentType: String? = null,
    @Schema(description = "Kort oppsummering av dokumentets innhold")
    val tittel: String? = null,
    @Schema(description = "Selve PDF dokumentet formatert som Base64")
    val dokument: String? = null,
    @Schema(
        description = "Typen dokument. Brevkoden sier noe om dokumentets innhold og oppbygning. Erstattes av dokumentmalId",
        deprecated = true,
    )
    @Deprecated("Bruk dokumentmalId", ReplaceWith("dokumentmalId"))
    val brevkode: String? = null,
    @Schema(description = "Typen dokument. Dokumentmal sier noe om dokumentets innhold og oppbygning.")
    val dokumentmalId: String? = null,
    @Schema(description = "Dokumentets status. Benyttes hvis journalposten er av typen forsendelse", enumAsRef = true)
    val status: DokumentStatusDto? = null,
    @Schema(description = "Arkivsystem hvor dokumentet er produsert og lagret", enumAsRef = true)
    val arkivSystem: DokumentArkivSystemDto? = null,
    @Schema(description = "Metadata om dokumentet")
    val metadata: Map<String, String> = emptyMap(),
) {
    override fun toString(): String =
        "(dokumentreferanse=$dokumentreferanse,journalpostId=$journalpostId, dokumentType=$dokumentType, " +
            "tittel=$tittel, status=$status, arkivSystem=$arkivSystem " +
            "dokumentmal=$dokumentmalId, metadata=$metadata"
}

@Schema(description = "Journalposten ble mottatt/sendt ut i kanal")
enum class Kanal {
    @Schema(description = "Ditt NAV")
    NAV_NO,

    @Schema(description = "Ditt NAV (Innsending bidrag)")
    NAV_NO_BID,

    @Schema(description = "Skanning Bidrag")
    SKAN_BID,

    @Schema(description = "Skanning Nets")
    SKAN_NETS,

    @Schema(description = "Skanning Iron mountain")
    SKAN_IM,

    @Schema(description = "Lokal utskrift. Gjelder utgående journalposter som er printet og sendt ut manuelt")
    LOKAL_UTSKRIFT,

    @Schema(description = "Sentral utskrift. Brukes kun for utgående journalposter")
    SENTRAL_UTSKRIFT,

    @Schema(
        description =
            "Digital postkasse til innbyggere. " +
                "Brevet er sendt via digital post til innbyggere. Brukes for utgående journalposter",
    )
    SDP,

    @Schema(description = "Ingen distribusjon av journalpost. Mottaker har ingen gyldig adresse å sende til.")
    INGEN_DISTRIBUSJON,

    @Schema(description = "Registrert av Nav-ansatt")
    INNSENDT_NAV_ANSATT,

    @Schema(description = "Nav.no uten ID-porten-pålogging")
    NAV_NO_UINNLOGGET,

    @Schema(description = "Innlogget samtale")
    NAV_NO_CHAT,
}

@Schema(description = "Metadata for kode vs dekode i et kodeobjekt")
data class KodeDto(
    @Schema(description = "Koden")
    val kode: String? = null,
    @Schema(description = "Dekode (kodebeskrivelse)")
    val dekode: String? = null,
    @Schema(description = "Om kodeobjektet inneholder en gyldig verdi")
    val erGyldig: Boolean = true,
)

enum class JournalpostType {
    INNGÅENDE,

    @Schema(description = "Bruk UTGÅENDE istedenfor", deprecated = true)
    @Deprecated("Bruk UTGÅENDE istedenfor", ReplaceWith("UTGÅENDE"))
    UTGAAENDE,
    UTGÅENDE,
    NOTAT,
}

object DokumentType {
    const val NOTAT = "X"
    const val UTGÅENDE = "U"
    const val INNGÅENDE = "I"
}

object Fagomrade {
    const val BIDRAG = "BID"
    const val FARSKAP = "FAR"
}

@Schema(
    description = """
    Mottak/Utsendingskanal som settes ved opprettelse av journalpost
    
    DIGITAL - Skal bare settes for inngående journalpost. Oversettes til NAV_NO. Dette er default for inngående
    
    SKANNING_BIDRAG - Skal settes hvis inngående journalpost er mottatt via Bidrag skanning
    
    LOKAL_UTSKRIFT - Skal settes hvis utgående journalpost er sendt via lokal utskrift. 
    Kanal for utgående journalposter blir ellers satt av dokumentdistribusjons løsningen.
    
    INGEN_DISTRIBUSJON - Skal settes hvis mottaker vil motta forsendelse via posten og ikke har noen gyldig adresse
""",
)
enum class MottakUtsendingKanal {
    DIGITALT,
    SKANNING_BIDRAG,
    LOKAL_UTSKRIFT,
    INGEN_DISTRIBUSJON,
}

val BID_JP_RANGE = (18900000L..40000000L)

val FORSENDELSE_RANGE = (1000000000L..10000000000L)

fun isBidJournalpostId(jpId: String) = jpId.isNumeric && jpId.toLong() in BID_JP_RANGE

fun isForsendelse(jpId: String) = jpId.isNumeric && jpId.toLong() in FORSENDELSE_RANGE

val String.numeric get() = this.replace("\\D".toRegex(), "").toLong()

val String.isNumeric get() = this.all { char -> char.isDigit() }

object JournalpostSystemPrefix {
    const val JOARK = "JOARK"

    const val MIDLERTIDLIG_BREVLAGER = "BID"

    const val FORSENDELSE = "BIF"

    const val UKJENT = "UKJENT"
}

typealias JournalpostSystem = String

class JournalpostId(
    val id: String?,
    private val defaultSystem: JournalpostSystem? = null,
) {
    private val system: JournalpostSystem

    val idNumerisk get() = id?.numeric

    val medSystemPrefiks get() = if (system != JournalpostSystemPrefix.UKJENT) "$system-$idNumerisk" else id

    init {
        system = parseId()
    }

    private fun parseId(): JournalpostSystem {
        if (id.isNullOrEmpty()) return JournalpostSystemPrefix.UKJENT

        val prefixSplit = id.split("-")

        return if (prefixSplit.size == 2) {
            prefixSplit[0].uppercase()
        } else {
            defaultSystem ?: if (!id.isNumeric) {
                JournalpostSystemPrefix.UKJENT
            } else if (isBidJournalpostId(id)) {
                JournalpostSystemPrefix.MIDLERTIDLIG_BREVLAGER
            } else if (isForsendelse(id)) {
                JournalpostSystemPrefix.FORSENDELSE
            } else {
                JournalpostSystemPrefix.JOARK
            }
        }
    }

    val erSystemBidrag get() = system == JournalpostSystemPrefix.MIDLERTIDLIG_BREVLAGER

    val erSystemJoark get() = system == JournalpostSystemPrefix.JOARK

    val erSystemForsendelse get() = system == JournalpostSystemPrefix.FORSENDELSE
}
