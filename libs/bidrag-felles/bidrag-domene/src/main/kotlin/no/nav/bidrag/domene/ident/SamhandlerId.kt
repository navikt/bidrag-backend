@file:Suppress("unused")

package no.nav.bidrag.domene.ident

import no.nav.bidrag.domene.felles.Verdiobjekt

class SamhandlerId(
    override val verdi: String,
) : Verdiobjekt<String>() {
    override fun gyldig(): Boolean = verdi.matches(SAMHANDLER_ID_REGEX)

    companion object {
        private val SAMHANDLER_ID_REGEX = Regex("^[89]\\d{10}$")
    }
}
