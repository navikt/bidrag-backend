package no.nav.bidrag.transport.behandling.belopshistorikk.request

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer

@Schema(description = "Request for å hente engangsbeløp som matcher angitte parametre")
data class HentEngangsbeløpRequest(
    @Schema(description = "Engangsbeløptype")
    val type: Engangsbeløptype,
    @Schema(description = "Referanse til sak")
    val sak: Saksnummer,
    @Schema(description = "Personidenten til den som skal betale stønaden")
    val skyldner: Personident,
    @Schema(description = "Personidenten til den som krever stønaden")
    val kravhaver: Personident,
    @Schema(description = "Referanse")
    val referanse: String,
)
