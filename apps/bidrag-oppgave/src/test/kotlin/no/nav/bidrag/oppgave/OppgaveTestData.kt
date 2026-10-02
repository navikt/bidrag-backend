package no.nav.bidrag.oppgave

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.EksternJournalpostId
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.EksternOppgaveId
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.Enhetsnummer
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.NavIdent
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.OppgaveDto
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.SokOppgaverResponse
import no.nav.bidrag.oppgave.controller.Beskrivelseinnslag
import no.nav.bidrag.oppgave.controller.BidragOppgaveDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

object OppgaveTestData {
    val oppgaveDto = OppgaveDto(
        id = EksternOppgaveId(123456789),
        tildeltEnhetsnr = Enhetsnummer("4100"),
        tema = "BID",
        oppgavetype = "JFR",
        versjon = 1,
        prioritet = OppgaveDto.Prioritet.NORM,
        status = OppgaveDto.Status.OPPRETTET,
        aktivDato = LocalDate.of(2026, 1, 15),
        opprettetTidspunkt = OffsetDateTime.parse("2026-01-15T09:00:00Z"),
        fristFerdigstillelse = LocalDate.of(2026, 1, 25),
    )

    private val bidragsoppgaveDto = oppgaveDto.copy(
        id = EksternOppgaveId(123),
        tema = "BID",
        oppgavetype = "BEH_SAK",
        status = OppgaveDto.Status.UNDER_BEHANDLING,
        saksreferanse = "SAK-123",
        journalpostId = EksternJournalpostId("JP-123"),
        tildeltEnhetsnr = Enhetsnummer("4100"),
        tilordnetRessurs = NavIdent("Z123456"),
        beskrivelse = "En bidragsoppgave",
        bruker = OppgaveDto.Bruker(
            ident = "12345678901",
            type = OppgaveDto.Bruker.BrukerType.PERSON,
        ),
        prioritet = OppgaveDto.Prioritet.NORM,
    )

    val forventetBidragOppgaveDto = BidragOppgaveDto(
        id = EksternOppgaveId(123),
        beskrivelse = "En bidragsoppgave",
        beskrivelseListe = listOf(
            Beskrivelseinnslag(
                tidspunkt = LocalDateTime.of(2026, 1, 15, 9, 0),
                kommentar = "En bidragsoppgave",
            ),
        ),
        status = OppgaveDto.Status.UNDER_BEHANDLING,
        opprettetTidspunkt = OffsetDateTime.parse("2026-01-15T09:00:00Z"),
        tema = "BID",
        oppgavetype = "BEH_SAK",
        journalpostId = EksternJournalpostId("JP-123"),
        tildeltEnhetsnr = Enhetsnummer("4100"),
        tilordnetRessurs = NavIdent("Z123456"),
        brukerFnr = Personident("12345678901"),
        saksreferanse = "SAK-123",
        prioritet = OppgaveDto.Prioritet.NORM,
        fristFerdigstillelse = LocalDate.of(2026, 1, 25),

    )

    fun oppgaveResponse(oppgaver: List<OppgaveDto> = listOf(bidragsoppgaveDto)): SokOppgaverResponse = SokOppgaverResponse(
        antallTreffTotalt = oppgaver.size.toLong(),
        oppgaver = oppgaver,
    )
}
