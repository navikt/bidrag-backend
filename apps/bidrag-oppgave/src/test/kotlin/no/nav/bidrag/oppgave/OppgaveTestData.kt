package no.nav.bidrag.oppgave

import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.EksternOppgaveId
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.Enhetsnummer
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.SokOppgaverResponse
import no.nav.bidrag.oppgave.dto.Beskrivelseinnslag
import no.nav.bidrag.oppgave.dto.OppgaveStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.OppgaveDto as OppgaveApiDto
import no.nav.bidrag.oppgave.dto.OppgaveDto as BidragOppgaveDto

object OppgaveTestData {
    val oppgaveDto = OppgaveApiDto(
        id = EksternOppgaveId(123456789),
        tildeltEnhetsnr = Enhetsnummer("4100"),
        tema = "OPP",
        oppgavetype = "JFR",
        versjon = 1,
        prioritet = OppgaveApiDto.Prioritet.NORM,
        status = OppgaveApiDto.Status.OPPRETTET,
        aktivDato = LocalDate.of(2026, 1, 15),
    )

    private val bidragsoppgaveDto = oppgaveDto.copy(
        id = EksternOppgaveId(123),
        tema = "BID",
        oppgavetype = "BEH_SAK",
        status = OppgaveApiDto.Status.UNDER_BEHANDLING,
        saksreferanse = "SAK-123",
        beskrivelse = "En bidragsoppgave",
        opprettetTidspunkt = OffsetDateTime.parse("2026-01-15T09:00:00Z"),
    )

    val forventetBidragOppgaveDto = BidragOppgaveDto(
        id = 123,
        tittel = "BID - BEH_SAK",
        beskrivelse = "En bidragsoppgave",
        beskrivelseshistorikk = listOf(
            Beskrivelseinnslag(
                tidspunkt = LocalDateTime.of(2026, 1, 15, 9, 0),
                kommentar = "En bidragsoppgave",
            ),
        ),
        status = OppgaveStatus.UNDER_BEHANDLING,
        opprettet = OffsetDateTime.parse("2026-01-15T09:00:00Z"),
    )

    fun oppgaveResponse(oppgaver: List<OppgaveApiDto> = listOf(bidragsoppgaveDto)): SokOppgaverResponse = SokOppgaverResponse(
        antallTreffTotalt = oppgaver.size.toLong(),
        oppgaver = oppgaver,
    )
}
