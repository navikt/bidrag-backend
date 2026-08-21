package no.nav.bidrag.oppgave.consumer.oppgaveapi.model

data class SokOppgaverResponse(
    /** Totalt antall oppgaver funnet med dette søket */
    val antallTreffTotalt: Long? = null,
    /** Liste over oppgaver */
    val oppgaver: List<OppgaveDto>? = null,
)
