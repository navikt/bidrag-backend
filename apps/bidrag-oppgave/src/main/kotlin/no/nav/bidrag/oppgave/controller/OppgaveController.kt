package no.nav.bidrag.oppgave.controller

import no.nav.bidrag.oppgave.dto.OppgaveDto
import no.nav.bidrag.oppgave.service.OppgaveService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class OppgaveController(
    private val oppgaveService: OppgaveService,
) {

    @GetMapping("/oppgaver")
    fun hentOppgaverForSak(
        @RequestParam saksnummer: String,
    ): List<OppgaveDto> = oppgaveService.finnOppgaver(
        FinnOppgaverRequest(
            saksnummer = saksnummer,
        ),
    )

    @PostMapping("/oppgaver")
    fun finnOppgaver(
        @RequestBody request: FinnOppgaverRequest,
    ): List<OppgaveDto> = oppgaveService.finnOppgaver(request)
}
