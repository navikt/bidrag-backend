package no.nav.bidrag.transport.behandling.vedtak.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Respons med alle endringsvedtak for en stønad (saksnr, stønadstype, skyldner, kravhaver")
data class HentVedtakForStønadResponse(
    @Schema(description = "Liste med vedtak for stønad")
    val vedtakListe: List<VedtakForStønad> = emptyList(),
)
