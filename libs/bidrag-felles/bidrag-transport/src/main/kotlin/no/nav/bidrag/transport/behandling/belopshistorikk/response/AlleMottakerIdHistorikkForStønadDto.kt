package no.nav.bidrag.transport.behandling.belopshistorikk.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema
data class AlleMottakerIdHistorikkForStonadDto(
    @Schema(description = "Alle forekomster på mottaker-id-historikk for en stønad")
    val alleMottakerIdHistorikkForStonad: List<MottakerIdHistorikkDto>?,
)

data class MottakerIdHistorikkDto(
    @Schema(description = "stonad-id")
    val stønadsid: Int,
    @Schema(description = "Utgått Mottaker-Id")
    val mottakerIdEndretFra: String,
    @Schema(description = "Ny Mottaker-Id")
    val mottakerIdEndretTil: String,
    @Schema(description = "Saksbehandler/batchjobb/annet som har oppdatert mottaker-id")
    val opprettetAv: String,
    @Schema(description = "Opprettet tidspunkt")
    val opprettetTidspunkt: LocalDateTime,
)
