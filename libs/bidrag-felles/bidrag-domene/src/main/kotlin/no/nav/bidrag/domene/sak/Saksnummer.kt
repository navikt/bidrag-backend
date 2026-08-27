@file:Suppress("unused")

package no.nav.bidrag.domene.sak

import no.nav.bidrag.domene.felles.Verdiobjekt

class Saksnummer(
    override val verdi: String,
) : Verdiobjekt<String>() {
    override fun gyldig() = verdi.matches(SEVEN_DIGITS_REGEX)

    companion object {
        private val SEVEN_DIGITS_REGEX = Regex("^\\d{7}$")
    }
}
