package no.nav.bidrag.oppgave.controller

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.AktorId
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.Enhetsnummer
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.NavIdent

@AvgrensetOppgavesøk
@Schema(description = "Minst ett søkekriterium må oppgis: saksnummer, aktør-ID, saksbehandler eller enhetsnummer.")
data class FinnOppgaverRequest(
    val saksnummer: String? = null,
    val aktoerId: AktorId? = null,
    val saksbehandler: NavIdent? = null,
    val enhetsnummer: Enhetsnummer? = null,
    @field:Schema(description = "Antall oppgaver (1 til 100). Hvis feltet mangler eller er null, brukes 100.", defaultValue = "100")
    @field:Min(1)
    @field:Max(100)
    val limit: Int? = 100,
)
