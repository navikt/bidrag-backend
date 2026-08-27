@file:Suppress("unused")

package no.nav.bidrag.domene.beløp

import no.nav.bidrag.domene.enums.samhandler.Valutakode
import java.math.BigDecimal

data class Beløp(
    val verdi: BigDecimal,
    val valutakode: Valutakode? = Valutakode.NOK,
) {
    constructor(verdi: Int) : this(verdi = verdi.toBigDecimal())

    fun erINorskeKroner() = valutakode == null || valutakode == Valutakode.NOK

    fun erISvenskeKroner() = valutakode != null && valutakode == Valutakode.SEK

    fun erIIslandskeKroner() = valutakode != null && valutakode == Valutakode.ISK

    fun erIEuro() = valutakode != null && valutakode == Valutakode.EUR

    fun erIUtenlandskValuta() = valutakode != null && valutakode != Valutakode.NOK
}
