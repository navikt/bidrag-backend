package no.nav.bidrag.oppgave.controller

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.AktorId

@AvgrensetOppgavesøk
@Schema(description = "Minst ett søkekriterium må oppgis: saksnummer eller aktør-ID.")
data class FinnOppgaverRequest(
    val saksnummer: String? = null,
    @Size(min = 13, max = 13)
    val aktoerId: AktorId? = null,
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
