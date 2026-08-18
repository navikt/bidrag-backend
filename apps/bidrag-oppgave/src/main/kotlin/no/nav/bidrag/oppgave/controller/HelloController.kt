package no.nav.bidrag.oppgave.controller

import no.nav.bidrag.oppgave.dto.OppgaveDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class HelloController {

    @GetMapping("/hello")
    fun hello(): OppgaveDto = OppgaveDto(
        id = 1,
        tittel = "Hello, Bidrag!",
        beskrivelse = "Første oppgave fra bidrag-oppgave",
    )
}
