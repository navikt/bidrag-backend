@file:Suppress("unused")

package no.nav.bidrag.domene.land

import no.nav.bidrag.domene.felles.Verdiobjekt

class Landkode(
    override val verdi: String,
) : Verdiobjekt<String>() {
    fun alfa2() = verdi.length == 2

    fun alfa3() = verdi.length == 3

    override fun gyldig() = verdi.length in setOf(2, 3)
}
