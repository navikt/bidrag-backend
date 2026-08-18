package no.nav.bidrag.dokument.journalpost.dto

import no.nav.bidrag.dokument.journalpost.model.Enhet
import no.nav.bidrag.dokument.journalpost.model.Oppgave
import no.nav.bidrag.dokument.journalpost.model.SaksbehandlersEnhet
import java.time.LocalDate

abstract class Oppgave(
    val aktivDato: String = LocalDate.now().toString(),
    val journalpostId: String,
    var aktoerId: String? = null,
    val saksreferanse: String?,
    val tema: String,
    val oppgavetype: String,
    val prioritet: String = Prioritet.HOY.name,
    val tildeltEnhetsnr: String,
    private val saksbehandlersEnhet: SaksbehandlersEnhet,
    private val gjelder: String?,
) {
    open val beskrivelse: String get() = hentBeskrivelse()
    val opprettetAvEnhetsnr: String get() = saksbehandlersEnhet.enhetsnummer

    fun berikMedAktoerId(_aktoerId: String): no.nav.bidrag.dokument.journalpost.dto.Oppgave {
        aktoerId = _aktoerId
        return this
    }

    fun hentGjelder(): String? = gjelder

    override fun toString() = asJson()

    fun asJson(): String =
        """
        {
            "journalpostId":"$journalpostId",
            "saksreferanse":"$saksreferanse",
            "aktoerId":"$aktoerId",
            "beskrivelse":"$beskrivelse",
            "tema":"$tema",
            "oppgavetype":"$oppgavetype",
            "tildeltEnhetsnr":"$tildeltEnhetsnr",
            "opprettetAvEnhetsnr":"${saksbehandlersEnhet.enhetsnummer}",
            "aktivDato":"$aktivDato",
            "prioritet":"$prioritet"
        }
        """.trimIndent()

    protected abstract fun hentBeskrivelse(): String

    override fun equals(other: Any?): Boolean {
        if (other is no.nav.bidrag.dokument.journalpost.dto.Oppgave) {
            return other.asJson() == asJson()
        }

        return false
    }

    override fun hashCode(): Int = asJson().hashCode()
}

class BestillOriginalOppgave(
    journalpostId: Int,
    saksreferanse: String?,
    private val skannetDato: LocalDate?,
    private val batchNavn: String?,
    private val gjelder: String?,
    saksbehandlersEnhet: SaksbehandlersEnhet,
) : no.nav.bidrag.dokument.journalpost.dto.Oppgave(
    journalpostId = journalpostId.toString(),
    saksreferanse = saksreferanse,
    tema = Oppgave.TEMA_POSTMOTTAK,
    oppgavetype = Oppgave.TYPE_FOR_AVVIK,
    tildeltEnhetsnr = Oppgave.ENHET_SCANNING,
    saksbehandlersEnhet = saksbehandlersEnhet,
    gjelder = gjelder,
) {
    private var enhetsinformasjon: String = "ikke beriket med enhetsiformasjon"
    private var identOgNavn: String = "ikke beriket med saksbehandler"

    fun berikMed(enhet: Enhet): no.nav.bidrag.dokument.journalpost.dto.Oppgave {
        enhetsinformasjon = enhet.hentEnhetsinformasjon()
        return this
    }

    fun berikMed(saksbehandler: Saksbehandler): no.nav.bidrag.dokument.journalpost.dto.Oppgave {
        identOgNavn = saksbehandler.hentIdentMedNavn()
        return this
    }

    override fun hentBeskrivelse() =
        """
        Originalbestilling: Vi ber om å få tilsendt papirdokumentet av vedlagte skannede dokument, se link.

        Dokumentet ble skannet $skannetDato
        med batchnavnet $batchNavn.

        Dokumentet skal sendes til $enhetsinformasjon,
        og merkes med $identOgNavn
        """.trimIndent()
}

class BestillReskanningOppgave(
    journalpostId: Int,
    saksreferanse: String?,
    private val skannetDato: LocalDate?,
    private val batchNavn: String?,
    private val reskanningBeskrivelse: String?,
    private val gjelder: String?,
    saksbehandlersEnhet: SaksbehandlersEnhet,
) : no.nav.bidrag.dokument.journalpost.dto.Oppgave(
    journalpostId = journalpostId.toString(),
    saksreferanse = saksreferanse,
    tema = Oppgave.TEMA_POSTMOTTAK,
    oppgavetype = Oppgave.TYPE_FOR_AVVIK,
    tildeltEnhetsnr = Oppgave.ENHET_SCANNING,
    saksbehandlersEnhet = saksbehandlersEnhet,
    gjelder = gjelder,
) {
    override fun hentBeskrivelse() =
        """
        Bestill reskanning:
        Vi ber om reskanning av dokument.
            
        Dokumentet ble skannet $skannetDato
        med batchnavnet $batchNavn.
        
        Beskrivelse fra saksbehandler:
        ${reskanningBeskrivelse ?: "Ingen"}
        """.trimIndent()
}

class BestillSplittingOppgave(
    journalpostId: Int?,
    saksreferanse: String?,
    private val skannetDato: LocalDate?,
    private val batchNavn: String?,
    private val filnavn: String?,
    private val beskrivSplitting: String,
    private val gjelder: String?,
    saksbehandlersEnhet: SaksbehandlersEnhet,
) : no.nav.bidrag.dokument.journalpost.dto.Oppgave(
    journalpostId = journalpostId?.toString() ?: "-1",
    saksreferanse = saksreferanse,
    tema = Oppgave.TEMA_POSTMOTTAK,
    oppgavetype = Oppgave.TYPE_FOR_AVVIK,
    tildeltEnhetsnr = Oppgave.ENHET_SCANNING,
    saksbehandlersEnhet = saksbehandlersEnhet,
    gjelder = gjelder,
) {
    override fun hentBeskrivelse() =
        """
        Bestill splitting av dokument:
        Saksbehandler ønsker ny splitting av dokument:
        "$beskrivSplitting"

        Dokumentet har filnavn "$filnavn" og ble skannet $skannetDato.
        Batchnavn: $batchNavn.
        """.trimIndent()
}

data class OpprettOppgaveResponse(
    var id: Long? = null,
    var journalpostId: String? = null,
    var saksreferanse: String? = null,
    var tildeltEnhetsnr: String? = null,
    var tema: String? = null,
    var oppgavetype: String? = null,
)

data class OppgaveData(
    var id: Long? = null,
    var tildeltEnhetsnr: String? = null,
    var endretAvEnhetsnr: String? = null,
    var opprettetAvEnhetsnr: String? = null,
    var journalpostId: String? = null,
    var journalpostkilde: String? = null,
    var behandlesAvApplikasjon: String? = null,
    var saksreferanse: String? = null,
    var bnr: String? = null,
    var samhandlernr: String? = null,
    var aktoerId: String? = null,
    var orgnr: String? = null,
    var tilordnetRessurs: String? = null,
    var beskrivelse: String? = null,
    var temagruppe: String? = null,
    var tema: String? = null,
    var behandlingstema: String? = null,
    var oppgavetype: String? = null,
    var behandlingstype: String? = null,
    var versjon: Int = -1,
    var mappeId: String? = null,
    var fristFerdigstillelse: String? = null,
    var aktivDato: String? = null,
    var opprettetTidspunkt: String? = null,
    var opprettetAv: String? = null,
    var endretAv: String? = null,
    var ferdigstiltTidspunkt: String? = null,
    var endretTidspunkt: String? = null,
    var prioritet: String? = null,
    var status: String? = null,
    var metadata: Map<String, String>? = null,
) {
    constructor(
        oppgaveId: Long,
        tildeltEnhetsnummer: String,
        fagomrade: String,
    ) : this(id = oppgaveId, tildeltEnhetsnr = tildeltEnhetsnummer, tema = fagomrade)

    private fun hentMetadata(): String {
        if (metadata == null || metadata!!.isEmpty()) {
            return "{}"
        }

        val keyValues = StringBuilder()

        metadata?.let { it.forEach { (key, value) -> keyValues.append(""""$key":"$value",""") } }

        keyValues.deleteCharAt(keyValues.length - 1) // fjerner siste komma

        return "{$keyValues}"
    }
}

enum class Prioritet {
    HOY, // , NORM, LAV
}
