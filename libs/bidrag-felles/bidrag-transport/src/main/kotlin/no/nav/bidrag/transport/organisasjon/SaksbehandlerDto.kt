package no.nav.bidrag.organisasjon.dto

import io.swagger.v3.oas.annotations.media.Schema

data class SaksbehandlerDto(
    val ident: String,
    @Schema(description = "Saksbehandlers navn (med eventuelt fornavn bak komma)")
    val navn: String? = null,
)
