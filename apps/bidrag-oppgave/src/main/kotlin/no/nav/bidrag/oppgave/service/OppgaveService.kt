package no.nav.bidrag.oppgave.service

import no.nav.bidrag.oppgave.consumer.oppgaveapi.OppgaveClient
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.FellesKodeverkTema
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.FinnOppgaverParams
import no.nav.bidrag.oppgave.dto.OppgaveDto
import no.nav.bidrag.oppgave.dto.OppgaveStatus
import org.springframework.stereotype.Service
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.OppgaveDto as OppgaveApiDto

@Service
class OppgaveService(
    private val oppgaveClient: OppgaveClient,
) {

    fun hentOppgaverForSak(saksnummer: String): List<OppgaveDto> = oppgaveClient
        .finnOppgaver(
            FinnOppgaverParams(
                saksreferanse = listOf(saksnummer),
                tema = listOf(FellesKodeverkTema.BID),
                statuser = statuserViSokerEtter,
            ),
        )
        .oppgaver
        .orEmpty()
        .map { it.tilBidragOppgave() }
}

private val statuserViSokerEtter = listOf(
    OppgaveApiDto.Status.OPPRETTET,
    OppgaveApiDto.Status.AAPNET,
    OppgaveApiDto.Status.UNDER_BEHANDLING,
    OppgaveApiDto.Status.FERDIGSTILT,
    OppgaveApiDto.Status.FEILREGISTRERT,
)

private fun OppgaveApiDto.tilBidragOppgave(): OppgaveDto = OppgaveDto(
    id = id.verdi,
    tittel = "$tema - $oppgavetype",
    beskrivelse = beskrivelse,
    beskrivelseshistorikk = OppgaveBeskrivelseParser.parse(
        beskrivelse = beskrivelse,
        sistEndretTidspunkt = endretTidspunkt ?: opprettetTidspunkt,
        sistEndretAv = endretAv ?: opprettetAv,
        sistEndretEnhetsnr = endretAvEnhetsnr ?: opprettetAvEnhetsnr,
        oppgaveId = id.verdi,
    ),
    status = status.tilBidragStatus(),
    opprettet = opprettetTidspunkt,
)

private fun OppgaveApiDto.Status.tilBidragStatus(): OppgaveStatus = when (this) {
    OppgaveApiDto.Status.OPPRETTET, OppgaveApiDto.Status.AAPNET -> OppgaveStatus.OPPRETTET
    OppgaveApiDto.Status.UNDER_BEHANDLING -> OppgaveStatus.UNDER_BEHANDLING
    OppgaveApiDto.Status.FERDIGSTILT, OppgaveApiDto.Status.FEILREGISTRERT -> OppgaveStatus.FERDIG
}
