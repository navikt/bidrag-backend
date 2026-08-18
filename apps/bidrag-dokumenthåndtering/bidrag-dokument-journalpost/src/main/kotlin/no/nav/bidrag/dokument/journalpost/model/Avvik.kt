package no.nav.bidrag.dokument.journalpost.model

import no.nav.bidrag.dokument.journalpost.dto.AvvikshendelseIntern
import no.nav.bidrag.dokument.journalpost.dto.OpprettOppgaveResponse
import no.nav.bidrag.dokument.journalpost.entity.Journalpost
import no.nav.bidrag.dokument.journalpost.exception.CharacterOverflowException
import no.nav.bidrag.dokument.journalpost.model.Journalstatus.MOTTAKSREGISTRERT
import no.nav.bidrag.dokument.journalpost.model.Journalstatus.UNDER_PRODUKSJON
import no.nav.bidrag.transport.dokument.AvvikType
import no.nav.bidrag.transport.dokument.Avvikshendelse
import no.nav.bidrag.transport.dokument.BehandleAvvikshendelseResponse

enum class Avvikstype(
    private val behandle: Behandle,
) {
    FARSKAP_UTELUKKET(Behandle.INGEN_STRENG_JOURNALSTATUS_BEGRENSNING),
    ARKIVERE_JOURNALPOST(Behandle.INGEN_STRENG_JOURNALSTATUS_BEGRENSNING),
    BESTILL_ORIGINAL(Behandle.INGEN_STRENG_JOURNALSTATUS_BEGRENSNING),
    BESTILL_RESKANNING(Behandle.INGEN_STRENG_JOURNALSTATUS_BEGRENSNING),
    BESTILL_SPLITTING(Behandle.INGEN_STRENG_JOURNALSTATUS_BEGRENSNING),
    ENDRE_FAGOMRADE(Behandle.INGEN_STRENG_JOURNALSTATUS_BEGRENSNING),
    FEILFORE_SAK(Behandle.INGEN_STRENG_JOURNALSTATUS_BEGRENSNING),
    INNG_TIL_UTG_DOKUMENT(Behandle.INGEN_STRENG_JOURNALSTATUS_BEGRENSNING),
    OVERFOR_TIL_ANNEN_ENHET(Behandle.BARE_JOURNALSTATUS_MOTTAKSREGISTRERT),
    SLETT_JOURNALPOST(Behandle.BARE_JOURNASTATUS_UNDER_PRODUKSJON),
    TREKK_JOURNALPOST(Behandle.BARE_JOURNALSTATUS_MOTTAKSREGISTRERT),
    REGISTRER_RETUR(Behandle.INGEN_STRENG_JOURNALSTATUS_BEGRENSNING),
    SEND_TIL_FAGOMRADE(Behandle.INGEN_STRENG_JOURNALSTATUS_BEGRENSNING),
    ;

    fun lagHendelse() = "AVVIK_$name"

    fun skalBehandleAvvikstype(journalstatus: String?) = when (behandle) {
        Behandle.BARE_JOURNALSTATUS_MOTTAKSREGISTRERT -> journalstatus == MOTTAKSREGISTRERT
        Behandle.BARE_JOURNASTATUS_UNDER_PRODUKSJON -> journalstatus == UNDER_PRODUKSJON
        Behandle.INGEN_STRENG_JOURNALSTATUS_BEGRENSNING -> true
    }

    private enum class Behandle {
        INGEN_STRENG_JOURNALSTATUS_BEGRENSNING,
        BARE_JOURNALSTATUS_MOTTAKSREGISTRERT,
        BARE_JOURNASTATUS_UNDER_PRODUKSJON,
    }
}

data class BehandleAvvikRequest(
    val avvikshendelseIntern: AvvikshendelseIntern,
) {
    constructor(avvikshendelse: Avvikshendelse, opprettetAvEnhetsnummer: String, journalpostId: Int) : this(
        AvvikshendelseIntern(avvikshendelse, opprettetAvEnhetsnummer, journalpostId),
    )

    fun harSaksnummer() = avvikshendelseIntern.saksnummer != null

    fun hentJournalpostId() = avvikshendelseIntern.journalpostId

    fun erForArkiveringAvJournalpost() = avvikshendelseIntern.avvikstype == Avvikstype.ARKIVERE_JOURNALPOST
}

enum class JoarkArkiveringStatus {
    IKKE_STARTET,
    STARTET,
    FEILET,
    FULLFORT,
}

data class BehandleAvvikResponse(
    var avvikstype: String,
    var opprettOppgaveResponse: OpprettOppgaveResponse?,
    var statusAvviksbehandling: StatusAvviksbehandling,
    var joarkArkiveringStatus: JoarkArkiveringStatus?,
    var ugyldigForklaring: String? = null,
) {
    constructor(statusAvviksbehandling: StatusAvviksbehandling) :
        this("", null, statusAvviksbehandling, JoarkArkiveringStatus.IKKE_STARTET)

    constructor(avviksbehandling: Avviksbehandling) :
        this(avviksbehandling.avvikstype.name, null, avviksbehandling.hentStatus(), JoarkArkiveringStatus.IKKE_STARTET) {
        ugyldigForklaring = if (avviksbehandling is UgyldigAvviksbehandling) avviksbehandling.forklaring else null
    }

    @Suppress("unused") // brukes fra java
    constructor(avviksbehandling: Avviksbehandling, statusAvviksbehandling: StatusAvviksbehandling) :
        this(avviksbehandling.avvikstype.name, null, statusAvviksbehandling, JoarkArkiveringStatus.IKKE_STARTET) {
        ugyldigForklaring = if (avviksbehandling is UgyldigAvviksbehandling) avviksbehandling.forklaring else null
    }

    constructor(avvikstype: Avvikstype, opprettOppgaveResponse: OpprettOppgaveResponse?, statusAvviksbehandling: StatusAvviksbehandling) :
        this(avvikstype.name, opprettOppgaveResponse, statusAvviksbehandling, JoarkArkiveringStatus.IKKE_STARTET)

    fun tilBehandleAvvikshendelseResponse(): BehandleAvvikshendelseResponse = BehandleAvvikshendelseResponse(
        avvikstype,
        opprettOppgaveResponse?.id,
        opprettOppgaveResponse?.tildeltEnhetsnr,
        opprettOppgaveResponse?.tema,
        opprettOppgaveResponse?.oppgavetype,
    )

    fun erUgyldig() = statusAvviksbehandling.erUgyldig()

    fun erStatus(statusAvviksbehandling: StatusAvviksbehandling) = statusAvviksbehandling == this.statusAvviksbehandling

    fun harUgyldigForklaring() = ugyldigForklaring != null

    fun hentOppgaveId() = opprettOppgaveResponse?.id
}

data class FinnAvvik(
    val journalpost: Journalpost? = null,
    val saksnummer: String? = null,
) {
    constructor(journalpost: Journalpost) : this(journalpost, null)

    fun hentListeMedAvvik(): List<AvvikType> {
        if (journalpost == null) {
            return emptyList()
        }

        if (saksnummer != null) {
            return journalpost.finnAvvikForSaksnummer(saksnummer).map { avvikstype -> AvvikType.valueOf(avvikstype.name) }
        }

        return journalpost.finnAvvik().map { avvikstype -> AvvikType.valueOf(avvikstype.name) }
    }
}

enum class StatusAvviksbehandling {
    GYLDIG,
    UGYLDIG,
    OPPRETT_OPPGAVE_FEILET,
    ER_IKKE_MOTTAKSREGISTRERT,
    ER_IKKE_KLAR_FOR_ARKIVERING,
    ;

    fun erUgyldig() = this != GYLDIG
}

data class JournalHendelseForAvvik(
    val avvikshendelseIntern: AvvikshendelseIntern,
    val hendelseData: HendelseData,
    val brukerident: String?,
    val enhet: Enhet?,
) {
    fun hentJournalpostId() = hendelseData.journalpostId

    fun hentOpprettetAvEnhet() = avvikshendelseIntern.hentOpprettetAvEnhet()

    fun lagHendelse() = avvikshendelseIntern.lagHendelse()

    fun lagBeskrivelse(): String? {
        val saksbehandlersBeskrivelse =
            if (avvikshendelseIntern.beskrivelse == null) {
                ""
            } else {
                ": ${avvikshendelseIntern.beskrivelse}"
            }

        return when (avvikshendelseIntern.avvikstype) {
            Avvikstype.ENDRE_FAGOMRADE -> {
                val gammelFagomrade = hendelseData.fagomrade
                val nyttFagomrade = avvikshendelseIntern.nyttFagomrade

                sjekkAtTekstIkkeOverskriderMaxAntallTegn("Endret fra $gammelFagomrade til $nyttFagomrade$saksbehandlersBeskrivelse")
            }

            Avvikstype.BESTILL_ORIGINAL -> {
                val enhhetsinformasjon =
                    enhet?.hentEnhetsinformasjon()
                        ?: "enhet ${avvikshendelseIntern.hentEnhetsnummer()}"

                sjekkAtTekstIkkeOverskriderMaxAntallTegn("Originaldokumentet er bestilt til $enhhetsinformasjon$saksbehandlersBeskrivelse")
            }

            else -> {
                avvikshendelseIntern.beskrivelse?.let { sjekkAtTekstIkkeOverskriderMaxAntallTegn(it) }
            }
        }
    }

    private fun sjekkAtTekstIkkeOverskriderMaxAntallTegn(beskrivelse: String): String {
        if (MAX_LENGDE_JORNALHENDELSE_BESKRIVELSE < beskrivelse.length) {
            throw CharacterOverflowException("Beskrivelse kan max være $MAX_LENGDE_JORNALHENDELSE_BESKRIVELSE tegn!")
        }

        return beskrivelse
    }
}

data class HendelseData(
    val journalpostId: Int,
    val fagomrade: String?,
    val detaljer: Map<String, String>,
)
