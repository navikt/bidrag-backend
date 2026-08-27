package no.nav.bidrag.transport.behandling.vedtak.request

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer

@Schema(description = "Request for å hente alle endringsvedtak for en stønad (saksnr, stønadstype, skyldner, kravhaver")
data class HentVedtakForStønadRequest(
    @Schema(description = "Saksnummer")
    val sak: Saksnummer,
    @Schema(description = "Hvilken type stønad det er snakk om")
    val type: Stønadstype,
    @Schema(description = "Personen som er skyldner i stønaden")
    val skyldner: Personident,
    @Schema(description = "Personen som er kravhaver i stønaden")
    val kravhaver: Personident,
)
