package no.nav.bidrag.elin.stub.skatt.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Oppdatert status", description = "Response etter oppdateringen av en status i bidrag-elin-stub.")
data class OppdatertStatus(
    @field:Schema(
        description = "Svaret etter oppdatering av status.",
        example = "Feil ved oversending av krav slått AV",
    )
    val melding: String,
)
