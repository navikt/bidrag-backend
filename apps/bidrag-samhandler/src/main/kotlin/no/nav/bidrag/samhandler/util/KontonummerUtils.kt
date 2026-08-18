package no.nav.bidrag.samhandler.util

import no.nav.bidrag.transport.dokument.isNumeric

object KontonummerUtils {
    fun erGyldigKontonummerMod11(kontonummer: String): Boolean {
        // Fjern mellomrom og punktum
        val cleanedKontonummer = kontonummer.replace("[\\s.]".toRegex(), "")

        // Valider format: 11 siffer
        if (cleanedKontonummer.length != 11 || !cleanedKontonummer.isNumeric) {
            return false
        }

        val kontrollSiffer = cleanedKontonummer.last().digitToInt()
        val nummerUtenKontrollSiffer = cleanedKontonummer.dropLast(1)

        // Beregn kontrollsiffer med MOD-11 algoritme
        val vekter = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)
        val sum =
            nummerUtenKontrollSiffer
                .mapIndexed { i, c -> c.digitToInt() * vekter[i] }
                .sum()

        val beregnetKontrollSiffer = 11 - (sum % 11)

        return when (beregnetKontrollSiffer) {
            10 -> false

            // Ugyldig kontonummer
            11 -> kontrollSiffer == 0

            else -> kontrollSiffer == beregnetKontrollSiffer
        }
    }
}
