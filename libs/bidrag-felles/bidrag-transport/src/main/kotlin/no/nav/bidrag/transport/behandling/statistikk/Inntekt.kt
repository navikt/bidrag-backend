package no.nav.bidrag.transport.behandling.statistikk

import java.math.BigDecimal

data class Inntekt(
    val type: String,
    val beløp: BigDecimal,
    val inntektstype: String? = null,
    val gjelderKravhaver: String? = null,
)
