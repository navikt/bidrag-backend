package no.nav.bidrag.dokument.journalpost

import no.nav.bidrag.dokument.journalpost.dto.AvvikshendelseIntern
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.BEKREFTET_SENDT_SCANNING
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.ENHETSNUMMER
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.ENHETSNUMMER_GAMMELT
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.ENHETSNUMMER_NYTT
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.FAGOMRADE
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.JOARK_ARKIVERING_STATUS
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.JOARK_JOURNALPOST_ID
import no.nav.bidrag.dokument.journalpost.model.AvvikDetaljer.RETUR_DATO
import no.nav.bidrag.dokument.journalpost.model.Avvikstype
import no.nav.bidrag.dokument.journalpost.model.JoarkArkiveringStatus
import no.nav.bidrag.transport.dokument.AvvikType
import no.nav.bidrag.transport.dokument.Avvikshendelse
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AvvikshendelseBuilder {
    private var avvikType: AvvikType? = null
    private var avvikstype: Avvikstype? = null
    private var joarkJournalpostId: Int? = null
    private var journalpostId: Int? = null
    private var joarkArkiveringStatus: JoarkArkiveringStatus? = null
    private var beskrivelse: String? = null
    private var gammeltEnhetsnummer: String? = null
    private var enhetsnummer: String? = null
    private var nyttEnhetsnummer: String? = null
    private var returDato: String? = null
    private var nyttFagomrade: String? = null
    private var opprettetAvEnhet: String? = null
    private var saksnummer: String? = null
    private var bekreftetSendtScanning = false

    fun med(avvikType: AvvikType?): AvvikshendelseBuilder {
        this.avvikType = avvikType
        return this
    }

    fun med(avvikstype: Avvikstype?): AvvikshendelseBuilder {
        this.avvikstype = avvikstype
        return this
    }

    fun medOpprettetAvEnhet(enhetsnummer: String?): AvvikshendelseBuilder {
        opprettetAvEnhet = enhetsnummer
        return this
    }

    fun med(joarkArkiveringStatus: JoarkArkiveringStatus?): AvvikshendelseBuilder {
        this.joarkArkiveringStatus = joarkArkiveringStatus
        return this
    }

    fun medSaksnummer(saksnummer: String?): AvvikshendelseBuilder {
        this.saksnummer = saksnummer ?: ""
        return this
    }

    fun medReturDato(returDato: LocalDate): AvvikshendelseBuilder {
        this.returDato = returDato.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return this
    }

    fun medBeskrivelse(beskrivelse: String?): AvvikshendelseBuilder {
        this.beskrivelse = beskrivelse
        return this
    }

    fun somErSendtScanning(): AvvikshendelseBuilder {
        bekreftetSendtScanning = true
        return this
    }

    fun somIkkeErSendtScanning(): AvvikshendelseBuilder {
        bekreftetSendtScanning = false
        return this
    }

    fun medJournalpostId(journalpostId: Int): AvvikshendelseBuilder {
        this.journalpostId = journalpostId
        return this
    }

    fun medJoarkJournalpostId(joarkJournalpostId: Int): AvvikshendelseBuilder {
        this.joarkJournalpostId = joarkJournalpostId
        return this
    }

    fun medNyttFagomrade(nyttFagomrade: String?): AvvikshendelseBuilder {
        this.nyttFagomrade = nyttFagomrade
        return this
    }

    fun medGammeltEnhetsnummer(gammeltEnhetsnummer: String?): AvvikshendelseBuilder {
        this.gammeltEnhetsnummer = gammeltEnhetsnummer
        return this
    }

    fun medNyttEnhetsnummer(nyttEnhetsnummer: String?): AvvikshendelseBuilder {
        this.nyttEnhetsnummer = nyttEnhetsnummer
        return this
    }

    fun medEnhetsnummer(enhetsnummer: String?): AvvikshendelseBuilder {
        this.enhetsnummer = enhetsnummer
        return this
    }

    fun bygg(): Avvikshendelse {
        val detaljer = HashMap<String, String>()
        if (returDato != null) {
            detaljer[RETUR_DATO] = returDato!!
        }
        if (opprettetAvEnhet != null) {
            detaljer[ENHETSNUMMER] = opprettetAvEnhet!!
        }
        if (nyttFagomrade != null) {
            detaljer[FAGOMRADE] = nyttFagomrade!!
        }
        if (bekreftetSendtScanning) {
            detaljer[BEKREFTET_SENDT_SCANNING] = "true"
        }
        if (nyttEnhetsnummer != null) {
            detaljer[ENHETSNUMMER_NYTT] = nyttEnhetsnummer!!
        }
        if (bekreftetSendtScanning) {
            detaljer[BEKREFTET_SENDT_SCANNING] = true.toString()
        }
        if (nyttFagomrade != null) {
            detaljer[FAGOMRADE] = nyttFagomrade!!
        }
        if (gammeltEnhetsnummer != null) {
            detaljer[ENHETSNUMMER_GAMMELT] = gammeltEnhetsnummer!!
        }
        if (joarkJournalpostId != null) {
            detaljer[JOARK_JOURNALPOST_ID] = joarkJournalpostId.toString()
        }
        if (joarkArkiveringStatus != null) {
            detaljer[JOARK_ARKIVERING_STATUS] = joarkArkiveringStatus!!.name
        }
        return Avvikshendelse(
            avvikType = if (avvikType != null) avvikType!!.name else avvikstype!!.name,
            beskrivelse = beskrivelse,
            saksnummer = if (saksnummer != null) saksnummer else "1771",
            detaljer = detaljer,
        )
    }

    fun byggAvvikshendelseIntern(): AvvikshendelseIntern = AvvikshendelseIntern(
        bygg(),
        (if (opprettetAvEnhet != null) opprettetAvEnhet else "123")!!,
        (if (journalpostId != null) journalpostId else -1)!!,
    )

    companion object {
        @JvmStatic
        fun enAvvikshendelse(): AvvikshendelseBuilder = AvvikshendelseBuilder()

        @JvmStatic
        fun enAvvikshendelseFor(avvikstype: Avvikstype?): AvvikshendelseBuilder = AvvikshendelseBuilder().med(avvikstype)
    }
}
