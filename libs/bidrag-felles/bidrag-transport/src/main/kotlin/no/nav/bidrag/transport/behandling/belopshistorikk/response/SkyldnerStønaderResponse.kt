package no.nav.bidrag.transport.behandling.belopshistorikk.response

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer

@Schema(description = "Respons med alle stønader for angitt skyldner")
data class SkyldnerStønaderResponse(
    @Schema(description = "Liste med info om skyldnerens stønader")
    val stønader: List<SkyldnerStønad> = emptyList(),
)

@Schema(description = "Objekt med relevant informasjon om skyldners stønader")
data class SkyldnerStønad(
    @Schema(description = "Saksnummer")
    val sak: Saksnummer,
    @Schema(description = "Hvilken type stønad det er snakk om")
    val type: Stønadstype,
    @Schema(description = "Personen som er kravhaver i stønaden")
    val kravhaver: Personident,
)
