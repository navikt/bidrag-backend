package no.nav.bidrag.transport.behandling.belopshistorikk.request

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer

@Schema(description = "Request for å hente stønad som matcher angitte parametre")
data class HentStønadRequest(
    @Schema(description = "Stønadstype")
    val type: Stønadstype,
    @Schema(description = "Referanse til sak")
    val sak: Saksnummer,
    @Schema(description = "Personidenten til den som skal betale stønaden")
    val skyldner: Personident,
    @Schema(description = "Personidenten til den som krever stønaden")
    val kravhaver: Personident,
)
