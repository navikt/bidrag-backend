package no.nav.bidrag.transport.behandling.belopshistorikk.response

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.math.BigDecimal

@Schema(description = "Respons med alle løpende bidragssaker for angitt skyldner i angitt periode")
data class LøpendeBidragPeriodeResponse(
    @param:Schema(description = "Liste med skyldners løpende bidragssaker")
    val bidragListe: List<LøpendeBidrag> = emptyList(),
)

@Schema(description = "Objekt med relevant informasjon om skyldners bidragssaker")
data class LøpendeBidrag(
    @param:Schema(description = "Saksnummer")
    val sak: Saksnummer,
    @param:Schema(description = "Hvilken type stønad det er snakk om")
    val type: Stønadstype,
    @param:Schema(description = "Personen som er kravhaver i stønaden")
    val kravhaver: Personident,
    @param:Schema(description = "Personen som er mottaker i stønaden")
    val mottaker: Personident? = null,
    @param:Schema(description = "Liste med perioder og beløp")
    val periodeListe: List<BidragPeriode>,
)

data class BidragPeriode(
    @param:Schema(description = "Periode for bidrag")
    val periode: ÅrMånedsperiode,
    @param:Schema(description = "Løpende beløp i stønaden for periode")
    val løpendeBeløp: BigDecimal,
    @param:Schema(description = "Valutakode")
    val valutakode: String = "NOK",
)
