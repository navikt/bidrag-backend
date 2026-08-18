package no.nav.bidrag.oppgave.service

import no.nav.bidrag.oppgave.dto.OppgaveDto
import no.nav.bidrag.oppgave.dto.OppgaveStatus
import no.nav.oppgave.OppgaveClient
import no.nav.oppgave.model.FinnOppgaverParams
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import no.nav.oppgave.model.OppgaveDto as OppgaveApiDto

@Service
class OppgaveService(
    private val oppgaveClient: OppgaveClient,
) {

    fun hentOppgaverForSak(saksnummer: String): List<OppgaveDto> = oppgaveClient
        .finnOppgaver(FinnOppgaverParams(saksreferanse = listOf(saksnummer)))
        .oppgaver
        .orEmpty()
        .map { it.tilBidragOppgave() }
}

private fun OppgaveApiDto.tilBidragOppgave(): OppgaveDto = OppgaveDto(
    id = id.verdi,
    tittel = "$tema - $oppgavetype",
    beskrivelse = beskrivelse,
    status = status.tilBidragStatus(),
    opprettet = opprettetTidspunkt ?: OffsetDateTime.now(),
)

private fun OppgaveApiDto.Status.tilBidragStatus(): OppgaveStatus = when (this) {
    OppgaveApiDto.Status.OPPRETTET, OppgaveApiDto.Status.AAPNET -> OppgaveStatus.OPPRETTET
    OppgaveApiDto.Status.UNDER_BEHANDLING -> OppgaveStatus.UNDER_BEHANDLING
    OppgaveApiDto.Status.FERDIGSTILT, OppgaveApiDto.Status.FEILREGISTRERT -> OppgaveStatus.FERDIG
}
