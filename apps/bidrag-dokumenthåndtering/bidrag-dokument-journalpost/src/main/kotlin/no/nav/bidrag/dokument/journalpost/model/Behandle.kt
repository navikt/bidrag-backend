package no.nav.bidrag.dokument.journalpost.model

import no.nav.bidrag.dokument.journalpost.dto.AvvikshendelseIntern
import no.nav.bidrag.dokument.journalpost.dto.BestillOriginalOppgave
import no.nav.bidrag.dokument.journalpost.dto.Oppgave
import no.nav.bidrag.dokument.journalpost.dto.Saksbehandler
import no.nav.bidrag.dokument.journalpost.exception.OppgaveException
import no.nav.bidrag.transport.person.PersonDto
import java.util.Optional

sealed class Avviksbehandling(
    internal val journalpostId: Int? = null,
    internal var fagomrade: String? = null,
    val avvikstype: Avvikstype,
    internal val oppgave: Oppgave? = null,
) {
    abstract fun hentEnhetForAvviksbehandling(): Enhet?

    abstract fun hentEnhetsnummerTilAvviksbehandler(): String

    fun erGyldig() = this is GyldigAvviksbehandling

    fun skalOppretteOppgave() = oppgave != null

    fun hentOppgave() = Optional.ofNullable(oppgave)

    fun hentFagomrade() = fagomrade ?: Fagomrade.BIDRAG

    fun hentJournalpostId() = journalpostId ?: -1

    fun hentStatus() = if (erGyldig()) StatusAvviksbehandling.GYLDIG else StatusAvviksbehandling.UGYLDIG
}

internal fun unsupportedOperationFor(klasse: Class<*>) = UnsupportedOperationException(
    "Metoden mangler implementasjon i ${klasse.simpleName}",
)

data class UgyldigAvviksbehandling(
    private val type: Avvikstype,
    val forklaring: String,
) : Avviksbehandling(avvikstype = type) {
    override fun hentEnhetForAvviksbehandling() = throw unsupportedOperationFor(this::class.java)

    override fun hentEnhetsnummerTilAvviksbehandler() = throw unsupportedOperationFor(this::class.java)
}

data class GyldigAvviksbehandling(
    private val avvikshendelseIntern: AvvikshendelseIntern,
    private val avviksoppgave: Oppgave? = null,
) : Avviksbehandling(
    journalpostId = avvikshendelseIntern.journalpostId,
    avvikstype = avvikshendelseIntern.avvikstype,
    oppgave = avviksoppgave,
) {
    fun skalBerikeOppgaveMedEnhetsinformasjon() = Avvikstype.BESTILL_ORIGINAL == avvikstype

    fun skalBerikeOppgaveMedInformasjonOmSaksbehandler() = Avvikstype.BESTILL_ORIGINAL == avvikstype

    fun hentEnhetsnummer() = avvikshendelseIntern.hentEnhetsnummer() ?: hentSaksbehandlersEnhetsnummer()

    fun hentSaksbehandlersEnhetsnummer() = avvikshendelseIntern.saksbehandlersEnhet.enhetsnummer

    override fun hentEnhetForAvviksbehandling() = avvikshendelseIntern.enhet

    override fun hentEnhetsnummerTilAvviksbehandler() = avvikshendelseIntern.hentEnhetsnummerTilAvviksbehandler()

    fun berikOppgaveMedEnhet(enhet: Enhet) {
        if (oppgave == null) {
            throw OppgaveException("Ingen oppgave å berike: $this")
        }

        avvikshendelseIntern.enhet = enhet

        when (oppgave) {
            is BestillOriginalOppgave -> oppgave.berikMed(enhet)
        }
    }

    fun berikOppgaveMedAktoerId(person: PersonDto) {
        val aktoerId = person.aktørId
        if (!aktoerId.isNullOrEmpty()) {
            oppgave?.berikMedAktoerId(aktoerId)
        }
    }

    fun berikOppgaveMedSaksbehandler(saksbehandler: Saksbehandler) {
        if (oppgave == null) {
            throw OppgaveException("Ingen oppgave å berike: $this")
        }

        when (oppgave) {
            is BestillOriginalOppgave -> oppgave.berikMed(saksbehandler)
        }
    }

    fun leggTil(fagomrade: String?): GyldigAvviksbehandling {
        this.fagomrade = fagomrade
        return this
    }
}

enum class Behandlingstype {
    JOURNALFORT,
    MOTTAKSREGISTRERT,
    KLAR_TIL_PRINT,
}

fun initGyldigAvviksbehandling(avvikshendelseIntern: AvvikshendelseIntern) = GyldigAvviksbehandling(
    avvikshendelseIntern = avvikshendelseIntern,
)

fun initGyldigAvviksbehandling(
    avvikshendelseIntern: AvvikshendelseIntern,
    oppgave: Oppgave?,
) = GyldigAvviksbehandling(
    avvikshendelseIntern = avvikshendelseIntern,
    avviksoppgave = oppgave,
)
