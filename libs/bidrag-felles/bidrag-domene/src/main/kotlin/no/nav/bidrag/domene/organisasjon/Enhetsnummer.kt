@file:Suppress("unused")

package no.nav.bidrag.domene.organisasjon

import no.nav.bidrag.domene.felles.Verdiobjekt

class Enhetsnummer(
    override val verdi: String,
) : Verdiobjekt<String>() {
    override fun gyldig() = verdi.matches(ENHET_REGEX)

    companion object {
        private val ENHET_REGEX = Regex("^\\d{4}$")
    }
}
