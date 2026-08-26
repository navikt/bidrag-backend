package no.nav.bidrag.generer.testdata.person

import no.nav.bidrag.domene.ident.Personident
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Random

private val K1_VEKT = intArrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2)
private val K2_VEKT = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)

@Suppress("unused")
fun genererFødselsnummer(
    innsendtFodselsdato: LocalDate? = null,
    innsendtKjønn: Kjønn? = null,
): String {
    val fodselsdato = innsendtFodselsdato ?: opprettTilfeldigFodselsdato()
    val kjonn = innsendtKjønn ?: hentTilfeldigKjonn()

    var fodselsnummer = opprettIndividnummer(fodselsdato, kjonn)

    try {
        fodselsnummer = opprettKontrollsiffer(K1_VEKT, fodselsnummer)
    } catch (e: IllegalStateException) {
        return genererFødselsnummer(fodselsdato, kjonn)
    }
    try {
        fodselsnummer = opprettKontrollsiffer(K2_VEKT, fodselsnummer)
    } catch (e: IllegalStateException) {
        return genererFødselsnummer(fodselsdato, kjonn)
    }

    return fodselsnummer
}

fun genererAktørid(): String = genererPerson().opprett().aktoerid!!

@Suppress("unused")
fun genererPersonident(
    innsendtFodselsdato: LocalDate? = null,
    innsendtKjønn: Kjønn? = null,
): Personident = Personident(genererFødselsnummer(innsendtFodselsdato, innsendtKjønn))

private fun opprettIndividnummer(
    fodselsdato: LocalDate,
    kjønn: Kjønn,
): String = fodselsdato.format(DateTimeFormatter.ofPattern("ddMMyy")) + genererIndividnummer(fodselsdato.year, kjønn)

private fun opprettTilfeldigFodselsdato(): LocalDate = LocalDate.now().minus(Period.ofDays(Random().nextInt(365 * 120)))

private fun genererIndividnummer(
    fodselsAr: Int,
    kjønn: Kjønn,
): String =
    when (fodselsAr) {
        in 1940..1999 -> opprettTilfeldigIndividnummer(kjønn, 900, 999)
        in 1854..1899 -> opprettTilfeldigIndividnummer(kjønn, 500, 749)
        in 1900..1999 -> opprettTilfeldigIndividnummer(kjønn, 0, 499)
        in 2000..2039 -> opprettTilfeldigIndividnummer(kjønn, 500, 999)
        else -> throw IllegalArgumentException("Fant ikke gyldig serie for årstallet $fodselsAr")
    }

private fun opprettTilfeldigIndividnummer(
    kjønn: Kjønn,
    fraInklusiv: Int,
    tilInklusiv: Int,
): String {
    val antall = (tilInklusiv - fraInklusiv + 1) / 2
    var individNr = (Random().nextInt(antall) * 2 + fraInklusiv + if (Kjønn.MANN == kjønn) 1 else 0).toString()

    while (individNr.length < 3) {
        individNr = ("0$individNr")
    }

    return individNr
}

private fun hentTilfeldigKjonn(): Kjønn {
    val alleKjønn = Kjønn.entries.toTypedArray()
    return alleKjønn[Random().nextInt(alleKjønn.size)]
}

private fun opprettKontrollsiffer(
    vekting: IntArray,
    fodselsnummer: String,
): String {
    var sum = 0
    for (i in vekting.indices) {
        sum += fodselsnummer.substring(i, i + 1).toInt() * vekting[i]
    }
    var kontrollsiffer = 11 - sum % 11
    if (kontrollsiffer == 11) {
        kontrollsiffer = 0
    }
    check(kontrollsiffer <= 9)

    return fodselsnummer + kontrollsiffer.toString()
}
