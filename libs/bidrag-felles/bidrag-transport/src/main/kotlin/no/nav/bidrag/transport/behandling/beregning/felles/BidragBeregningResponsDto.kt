package no.nav.bidrag.transport.behandling.beregning.felles

import no.nav.bidrag.domene.enums.beregning.Samværsklasse
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.math.BigDecimal
import java.time.LocalDate

data class BidragBeregningResponsDto(
    val beregningListe: List<BidragBeregning>,
) {
    data class BidragBeregning(
        val periode: ÅrMånedsperiode? = null,
        val saksnummer: String,
        val personidentBarn: Personident,
        val datoSøknad: LocalDate,
        val beregnetBeløp: BigDecimal,
        val faktiskBeløp: BigDecimal,
        val beløpSamvær: BigDecimal,
        val stønadstype: Stønadstype = Stønadstype.BIDRAG,
        val samværsklasse: Samværsklasse? = null,
        val vedtaksid: Int? = null,
        val bidragJustertForNettoBarnetilleggBP: Boolean? = false,
        val bruttoBidragEtterBarnetilleggBM: BigDecimal? = null,
        val bruttoBidragEtterBarnetilleggBP: BigDecimal? = null,
        val erVedtakKildeBBM: Boolean = true,
    )
}
