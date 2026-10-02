package no.nav.bidrag.oppgave.controller

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.EksternJournalpostId
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.EksternOppgaveId
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.Enhetsnummer
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.NavIdent
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.OppgaveDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

data class BidragOppgaveDto(
    val id: EksternOppgaveId,
    val beskrivelse: String? = null,
    /**
     * Strukturert tolkning av [beskrivelse]. Rekkefølgen er som i kilden (nyeste innslag først).
     *
     * `null` betyr at vi ikke klarte å utlede noen historikk. Tom liste betyr at tolkningen lyktes,
     * men at det ikke fantes noen innslag.
     */
    val beskrivelseListe: List<Beskrivelseinnslag>? = null,
    val status: OppgaveDto.Status,
    val opprettetTidspunkt: OffsetDateTime? = null,
    val tema: String,
    val oppgavetype: String,
    val journalpostId: EksternJournalpostId?,
    val tildeltEnhetsnr: Enhetsnummer,
    val tilordnetRessurs: NavIdent?,
    val brukerFnr: Personident?,
    val saksreferanse: String?,
    val prioritet: OppgaveDto.Prioritet,
    val fristFerdigstillelse: LocalDate?,
)

data class Beskrivelseinnslag(
    val tidspunkt: LocalDateTime? = null,
    val saksbehandlerNavn: String? = null,
    val saksbehandlerId: String? = null,
    val enhetsnr: String? = null,
    val kommentar: String? = null,
    val endringer: List<String> = emptyList(),
)
