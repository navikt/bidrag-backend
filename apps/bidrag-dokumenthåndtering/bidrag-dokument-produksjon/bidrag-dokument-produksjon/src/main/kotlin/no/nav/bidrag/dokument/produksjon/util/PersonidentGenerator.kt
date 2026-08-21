package no.nav.bidrag.dokument.produksjon.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

private val KONTROLLSIFFER_VEKT_1 = intArrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2)
private val KONTROLLSIFFER_VEKT_2 = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)

/**
 * Genererer et tilfeldig, gyldig formatert fødselsnummer (dato + individnummer +
 * to kontrollsiffer beregnet med modulus 11). Brukes kun til å fylle inn placeholder-identer
 * i eksempeldata (se [no.nav.bidrag.dokument.produksjon.api.ProduserNotatApi]) slik at ekte
 * fødselsnumre ikke lagres i repoet.
 */
fun genererFødselsnummer(): String {
    val fødselsdato = tilfeldigFødselsdato()
    val datoDel = fødselsdato.format(DateTimeFormatter.ofPattern("ddMMyy"))
    val individnummer = Random.nextInt(0, 1000).toString().padStart(3, '0')

    val kontrollsiffer1 = beregnKontrollsiffer(datoDel + individnummer, KONTROLLSIFFER_VEKT_1) ?: return genererFødselsnummer()
    val kontrollsiffer2 =
        beregnKontrollsiffer(datoDel + individnummer + kontrollsiffer1, KONTROLLSIFFER_VEKT_2) ?: return genererFødselsnummer()

    return datoDel + individnummer + kontrollsiffer1 + kontrollsiffer2
}

private fun tilfeldigFødselsdato(): LocalDate = LocalDate.now().minusDays(Random.nextLong(0, 365L * 100))

private fun beregnKontrollsiffer(
    siffer: String,
    vekt: IntArray,
): Int? {
    val sum = siffer.indices.sumOf { siffer[it].digitToInt() * vekt[it] }
    val kontrollsiffer = 11 - sum % 11
    return when (kontrollsiffer) {
        11 -> 0
        10 -> null
        else -> kontrollsiffer
    }
}
