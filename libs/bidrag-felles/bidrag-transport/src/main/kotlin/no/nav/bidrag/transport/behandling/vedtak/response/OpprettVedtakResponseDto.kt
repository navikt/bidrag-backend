package no.nav.bidrag.transport.behandling.vedtak.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema
data class OpprettVedtakResponseDto(
    @Schema(description = "Unik id generert for vedtak")
    val vedtaksid: Int,
    @Schema(description = "Liste over alle referansen for engangsbeløp som inngår i vedtaket. Ligger i samme rekkefølge som i requesten")
    val engangsbeløpReferanseListe: List<String>,
)
