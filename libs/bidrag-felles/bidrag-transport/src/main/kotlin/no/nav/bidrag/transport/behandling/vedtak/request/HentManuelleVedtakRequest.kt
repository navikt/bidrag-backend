package no.nav.bidrag.transport.behandling.vedtak.request

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident
import java.time.YearMonth

data class HentManuelleVedtakRequest(
    @Schema(description = "Bidragspliktiges personident")
    val skyldner: Personident,
)
