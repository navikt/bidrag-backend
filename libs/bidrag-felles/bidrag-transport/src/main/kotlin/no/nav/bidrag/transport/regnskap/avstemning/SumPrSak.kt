package no.nav.bidrag.transport.regnskap.avstemning

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

data class SumPrSakResponse(
    val saker: List<SumPrSak>,
)

data class SumPrSak(
    @Schema(description = "Saksnummer.")
    val saksnummer: String,
    @Schema(description = "Antall innslag av innsendt stønadstype for saken.")
    val antall: Long,
    @Schema(description = "Samlet beløp for alle innslag i saken.")
    val beløp: BigDecimal?,
)
