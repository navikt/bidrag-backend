package no.nav.bidrag.transport.behandling.belopshistorikk.request

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident

@Schema(description = "Request for å hente alle stønader for angitt skyldner")
data class SkyldnerStønaderRequest(
    @Schema(description = "Personen som er skyldner")
    val skyldner: Personident,
)
