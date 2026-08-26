@file:Suppress("unused")

package no.nav.bidrag.domene.ident

import no.nav.bidrag.domene.felles.Verdiobjekt

class Organisasjonsnummer(
    override val verdi: String,
) : Verdiobjekt<String>() {
    override fun gyldig(): Boolean {
        if (verdi.length != 9 || verdi.toLongOrNull() == null) {
            return false
        }

        val siffer = verdi.chunked(1).map { it.toInt() }
        val vekting = intArrayOf(3, 2, 7, 6, 5, 4, 3, 2)

        val kontrollMod = 11 - (0..7).sumOf { vekting[it] * siffer[it] } % 11
        val kontrollsiffer = siffer[8]

        return gyldigKontrollSiffer(kontrollMod, kontrollsiffer)
    }

    private fun gyldigKontrollSiffer(
        kontrollMod: Int,
        kontrollsiffer: Int,
    ): Boolean {
        if (kontrollMod == kontrollsiffer) {
            return true
        }
        if (kontrollMod == 11 && kontrollsiffer == 0) {
            return true
        }
        return false
    }
}
