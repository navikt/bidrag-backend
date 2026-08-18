package no.nav.bidrag.dokument.journalpost.dto

import no.nav.bidrag.dokument.journalpost.exception.AvvikDetaljException
import no.nav.bidrag.dokument.journalpost.exception.AvvikException
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.BEKREFTET_SENDT_SCANNING
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.ENHETSNUMMER
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.ENHETSNUMMER_GAMMELT
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.ENHETSNUMMER_NYTT
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.FAGOMRADE
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.JOARK_ARKIVERING_STATUS
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.JOARK_JOURNALPOST_ID
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.RETUR_DATO
import no.nav.bidrag.dokument.journalpost.model.Avvikstype
import no.nav.bidrag.dokument.journalpost.model.Enhet
import no.nav.bidrag.dokument.journalpost.model.HendelseData
import no.nav.bidrag.dokument.journalpost.model.JoarkArkiveringStatus
import no.nav.bidrag.dokument.journalpost.model.SaksbehandlersEnhet
import no.nav.bidrag.transport.dokument.Avvikshendelse

data class AvvikshendelseIntern(
    val avvikstype: Avvikstype,
    val beskrivelse: String? = null,
    val saksbehandlersEnhet: SaksbehandlersEnhet,
    val journalpostId: Int = -1,
    var saksnummer: String? = null,
    internal var enhet: Enhet? = null,
    internal var hendelseData: HendelseData? = null,
    private val detaljer: Map<String, String?> = HashMap(),
    private var bekreftetSendtScanning: Boolean = false,
) {
    val returDato: String get() = detaljer[RETUR_DATO] ?: throw AvvikDetaljException(RETUR_DATO)
    val enhetsnummer: String get() = hentEnhetsnummer() ?: throw AvvikDetaljException(ENHETSNUMMER)
    val enhetsnummerGammelt: String get() = detaljer[ENHETSNUMMER_GAMMELT] ?: throw AvvikDetaljException(ENHETSNUMMER_GAMMELT)
    val enhetsnummerNytt: String get() = detaljer[ENHETSNUMMER_NYTT] ?: throw AvvikDetaljException(ENHETSNUMMER_NYTT)
    val joarkJournalpostId: Int get() = detaljer[JOARK_JOURNALPOST_ID]?.toInt() ?: -1
    val nyttFagomrade: String get() = detaljer[FAGOMRADE] ?: throw AvvikDetaljException(FAGOMRADE)
    val joarkArkiveringStatus: JoarkArkiveringStatus
        get() = detaljer[JOARK_ARKIVERING_STATUS]?.let { JoarkArkiveringStatus.valueOf(it) } ?: JoarkArkiveringStatus.IKKE_STARTET

    private val avvikstypensEnhetsnummer get() = detaljer.getValue(ENHETSNUMMER) ?: throw AvvikDetaljException(ENHETSNUMMER)
    private val avvikstypensNyeEnhetsnummer get() = detaljer.getValue(ENHETSNUMMER_NYTT) ?: throw AvvikDetaljException(ENHETSNUMMER_NYTT)

    constructor(avvikshendelse: Avvikshendelse, opprettetAvEnhetsnummer: String, journalpostId: Int) : this(
        avvikstype = Avvikstype.valueOf(avvikshendelse.avvikType),
        beskrivelse = avvikshendelse.beskrivelse,
        saksbehandlersEnhet = SaksbehandlersEnhet(opprettetAvEnhetsnummer),
        journalpostId = journalpostId,
        saksnummer = avvikshendelse.saksnummer,
    ) {
        (detaljer as MutableMap).putAll(avvikshendelse.detaljer)
        bekreftetSendtScanning = avvikshendelse.detaljer[BEKREFTET_SENDT_SCANNING]?.toBoolean() ?: false
    }

    fun leggTilHendelseData(hendelseData: HendelseData) {
        this.hendelseData = hendelseData
    }

    fun erBreftetSendtScanning() = bekreftetSendtScanning

    fun erUgyldigForJournalstatus(journalstatus: String?) = !avvikstype.skalBehandleAvvikstype(journalstatus)

    fun harIkkeSaksnummer() = saksnummer == null || saksnummer!!.isBlank()

    fun harNyttFagomrade() = detaljer.keys.contains(FAGOMRADE)

    fun hentBeskrivelse() = beskrivelse ?: throw AvvikException("Mangler beskrivelse av avvikshendelsen")

    fun hentEnhetsnummer() = detaljer[ENHETSNUMMER]

    fun hentEnhetsnummerTilAvviksbehandler() = hentSaksbehandlersEnhetsnummer()

    fun hentJournalpostIdForBidrag() = "BID-$journalpostId"

    fun hentOpprettetAvEnhet() = saksbehandlersEnhet.enhetsnummer

    fun hentSaksbehandlersEnhetsnummer() = saksbehandlersEnhet.enhetsnummer

    fun lagHendelse() = avvikstype.lagHendelse()

    fun lagHendelseDetaljer(fagomrade: String): Map<String, String?> = when (avvikstype) {
        Avvikstype.BESTILL_ORIGINAL -> {
            mapOf(ENHETSNUMMER to avvikstypensEnhetsnummer, FAGOMRADE to fagomrade)
        }

        Avvikstype.OVERFOR_TIL_ANNEN_ENHET -> {
            mapOf(
                ENHETSNUMMER_GAMMELT to enhetsnummerGammelt,
                ENHETSNUMMER_NYTT to avvikstypensNyeEnhetsnummer,
                FAGOMRADE to fagomrade,
            )
        }

        else -> {
            mapOf(FAGOMRADE to fagomrade)
        }
    }
}
