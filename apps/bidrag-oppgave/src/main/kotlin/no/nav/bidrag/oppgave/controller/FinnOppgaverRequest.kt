package no.nav.bidrag.oppgave.controller

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.AktorId
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.Enhetsnummer
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.NavIdent

@AvgrensetOppgavesøk
@Schema(description = "Minst ett søkekriterium må oppgis: saksnummer, aktør-ID, saksbehandler eller enhetsnummer.")
data class FinnOppgaverRequest(
    val saksnummer: String? = null,
    @Size(min = 13, max = 13)
    val aktoerId: AktorId? = null,
    val saksbehandler: NavIdent? = null,
    @field:Size(min = 4, max = 4)
    val enhetsnummer: Enhetsnummer? = null,
    @field:Min(1)
    val limit: Int? = STANDARD_LIMIT,
    @field:Min(0)
    val offset: Int? = STANDARD_OFFSET,

) {
    companion object {
        const val STANDARD_OFFSET = 0
        const val STANDARD_LIMIT = 100
    }
}
