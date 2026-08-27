@file:Suppress("unused")

package no.nav.bidrag.domene.land

import no.nav.bidrag.domene.felles.Verdiobjekt

class Landkode2(
    override val verdi: String,
) : Verdiobjekt<String>() {
    override fun gyldig() = verdi.length == 2
}
