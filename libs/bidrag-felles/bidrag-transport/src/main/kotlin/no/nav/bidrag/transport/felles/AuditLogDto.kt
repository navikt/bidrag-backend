package no.nav.bidrag.transport.felles

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AuditLogDto(
    @Schema(description = "Navnet på tabellen som ble endret.")
    val tabellNavn: String,
    @Schema(description = "Id til raden som ble endret.")
    val tabellId: Int,
    @Schema(description = "Operasjonen som ble utført.")
    val operasjon: String,
    @Schema(description = "Tidspunkt for endringen.")
    val endretTidspunkt: LocalDateTime,
    @Schema(description = "Brukeren som endret raden. Kan være et system, database-bruker eller saksbehandler.")
    val endretAv: String,
    @Schema(description = "Verdiene som raden hadde før endringen.")
    val gamleVerdier: String,
    @Schema(description = "Verdiene som raden har etter endringen.")
    val nyeVerdier: String,
)
