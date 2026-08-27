package no.nav.bidrag.transport.behandling.felles.grunnlag

import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import java.math.BigDecimal

data class DelberegningIndeksreguleringPeriode(
    override val periode: ÅrMånedsperiode,
    val indeksreguleringFaktor: BigDecimal? = null,
    val valutakode: String,
    val beløp: BigDecimal,
) : Delberegning
