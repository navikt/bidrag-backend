package no.nav.bidrag.transport.behandling.vedtak.request

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer

@Schema(description = "Request for å fatte vedtak for vedtaksforslag. Kan inneholde flere stønader innenfor et vedtaksforslag")
data class FatteVedtaksforslagRequestDto(
    @Schema(description = "Liste over stønader som det skal fattes vedtak for")
    val stønadListe: List<FattVedtaksforslagStønadDto> = emptyList(),
)

data class FattVedtaksforslagStønadDto(
    @Schema(description = "Hvilken type stønad det er snakk om")
    val type: Stønadstype,
    @Schema(description = "Saksnummer")
    val sak: Saksnummer,
    @Schema(description = "Personen som er skyldner i stønaden")
    val skyldner: Personident,
    @Schema(description = "Personen som er kravhaver i stønaden")
    val kravhaver: Personident,
    @Schema(description = "Siste vedtaksid for stønaden")
    val sisteVedtaksid: Int? = null,
)
