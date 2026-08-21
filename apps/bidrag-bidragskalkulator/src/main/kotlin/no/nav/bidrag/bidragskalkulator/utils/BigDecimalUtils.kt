package no.nav.bidrag.bidragskalkulator.utils

import java.math.BigDecimal
import java.math.RoundingMode

fun BigDecimal.avrundeTilNærmesteHundre(): BigDecimal = this.divide(BigDecimal(100))
    .setScale(0, RoundingMode.HALF_UP)
    .multiply(BigDecimal(100))
