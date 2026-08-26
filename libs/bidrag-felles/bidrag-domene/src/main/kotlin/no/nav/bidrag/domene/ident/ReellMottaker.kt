@file:Suppress("unused")

package no.nav.bidrag.domene.ident

import no.nav.bidrag.domene.felles.Verdiobjekt

class ReellMottaker(
    override val verdi: String,
) : Verdiobjekt<String>() {
    override fun gyldig(): Boolean = Personident(verdi).gyldig() || SamhandlerId(verdi).gyldig()

    fun erPersonIdent() = Personident(verdi).gyldig()

    fun erSamhandlerId() = SamhandlerId(verdi).gyldig()

    fun personIdent() = if (erPersonIdent()) Personident(verdi) else null

    fun samhandlerId() = if (erSamhandlerId()) SamhandlerId(verdi) else null
}
