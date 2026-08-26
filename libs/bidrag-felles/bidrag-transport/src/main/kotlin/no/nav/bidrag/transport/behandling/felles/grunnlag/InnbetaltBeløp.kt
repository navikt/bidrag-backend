package no.nav.bidrag.transport.behandling.felles.grunnlag

import no.nav.bidrag.domene.ident.Personident
import java.math.BigDecimal

data class InnbetaltBeløp(
    val beløp: BigDecimal,
    val søknadsbarn: Personident,
)
