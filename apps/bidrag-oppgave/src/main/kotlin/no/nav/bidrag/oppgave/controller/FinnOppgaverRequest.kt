package no.nav.bidrag.oppgave.controller

import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.AktorId
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.Enhetsnummer
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.NavIdent

data class FinnOppgaverRequest(
    val saksnummer: String? = null,
    val aktoerId: AktorId? = null,
    val saksbehandler: NavIdent? = null,
    val enhetsnummer: Enhetsnummer? = null,
    val limit: Int = 100,
)
