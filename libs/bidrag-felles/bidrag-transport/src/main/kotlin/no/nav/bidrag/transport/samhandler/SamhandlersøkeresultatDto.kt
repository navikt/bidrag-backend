package no.nav.bidrag.transport.samhandler

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Søkeresultat etter søk på samhandler.")
data class SamhandlersøkeresultatDto(
    val samhandlere: List<SamhandlerDto>,
    @Schema(description = "True hvis det finnes flere forekomster enn det som er returnert i dette objektet.")
    val flereForekomster: Boolean,
)
