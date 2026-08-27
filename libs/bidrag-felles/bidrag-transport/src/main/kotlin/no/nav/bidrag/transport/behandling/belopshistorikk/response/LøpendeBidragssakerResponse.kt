package no.nav.bidrag.transport.behandling.belopshistorikk.response

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import java.math.BigDecimal

@Schema(description = "Respons med alle løpende bidragssaker for angitt skyldner på angitt dato")
data class LøpendeBidragssakerResponse(
    @Schema(description = "Liste med info om skyldnerens løpende bidragssaker")
    val bidragssakerListe: List<LøpendeBidragssak> = emptyList(),
)

@Schema(description = "Objekt med relevant informasjon om skyldners bidragssaker")
data class LøpendeBidragssak(
    @Schema(description = "Saksnummer")
    val sak: Saksnummer,
    @Schema(description = "Hvilken type stønad det er snakk om")
    val type: Stønadstype,
    @Schema(description = "Personen som er kravhaver i stønaden")
    val kravhaver: Personident,
    @Schema(description = "Løpende beløp i stønaden på dato angitt i requesten")
    val løpendeBeløp: BigDecimal,
    @Schema(description = "Valutakoden tilhørende stønadsbeløpet")
    val valutakode: String = "NOK",
)
