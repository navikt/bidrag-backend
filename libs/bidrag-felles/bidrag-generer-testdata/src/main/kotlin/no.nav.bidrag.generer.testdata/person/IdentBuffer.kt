package no.nav.bidrag.generer.testdata.person

import no.nav.bidrag.generer.testdata.RandomTestData
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Suppress("unused")
class IdentBuffer {
    private val buffer = StringBuffer(11)

    fun withDate(date: LocalDate): IdentBuffer {
        buffer.replace(0, 6, DateTimeFormatter.ofPattern("ddMMyy").format(date))
        return this
    }

    fun increaseDigit(
        pos: Int,
        increaseBy: Int,
    ): IdentBuffer {
        buffer.replace(pos, pos + 1, (buffer.substring(pos, pos + 1).toInt() + increaseBy).toString())
        return this
    }

    fun withPersonnummer(
        kjønn: Kjønn?,
        fodtAar: Int,
    ): IdentBuffer? {
        try {
            set(6, 3, randomPersonnummer(kjønn, fodtAar))
            return beregnKontrollsiffer()
        } catch (e: IllegalStateException) {
            return withPersonnummer(kjønn, fodtAar)
        }
    }

    fun withDodfodtNr(dodfodtNr: Int): IdentBuffer {
        set(6, 5, dodfodtNr)
        return this
    }

    private fun randomPersonnummer(
        kjønn: Kjønn?,
        fodtAar: Int,
    ): Int =
        when (fodtAar) {
            in 1940..1999 -> {
                randomPersonnummer(kjønn, 900, 999)
            }

            in 1854..1899 -> {
                randomPersonnummer(kjønn, 500, 749)
            }

            in 1900..1999 -> {
                randomPersonnummer(kjønn, 0, 499)
            }

            in 2000..2039 -> {
                randomPersonnummer(kjønn, 500, 999)
            }

            else -> {
                throw IllegalArgumentException("Fant ikke gyldig serie for årstallet $fodtAar")
            }
        }

    private fun randomPersonnummer(
        kjønn: Kjønn?,
        fraInklusiv: Int,
        tilInklusiv: Int,
    ): Int {
        val antall = (tilInklusiv - fraInklusiv + 1) / 2
        return (
            (RandomTestData.random().random.nextInt(antall) * 2) +
                fraInklusiv +
                (if (Kjønn.MANN == kjønn) 1 else 0)
        )
    }

    fun beregnKontrollsiffer(): IdentBuffer {
        set(9, 1, kontrollsiffer(K1_VEKT))
        set(10, 1, kontrollsiffer(K2_VEKT))
        return this
    }

    fun set(
        pos: Int,
        digits: Int,
        value: Int,
    ): IdentBuffer {
        buffer.replace(pos, pos + digits, leftPad(digits, value.toString()))
        return this
    }

    fun leftPad(
        length: Int,
        str: String,
    ): String {
        val padded = str.padStart(length, '0')
        return padded.substring(padded.length - length)
    }

    private fun kontrollsiffer(vekting: IntArray): Int {
        var sum = 0
        for (i in vekting.indices) {
            sum += buffer.substring(i, i + 1).toInt() * vekting[i]
        }
        var kontrollsiffer = 11 - (sum % 11)
        if (kontrollsiffer == 11) {
            kontrollsiffer = 0
        }
        check(kontrollsiffer <= 9)
        return kontrollsiffer
    }

    override fun toString(): String = buffer.toString()

    companion object {
        private val K1_VEKT = intArrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2)
        private val K2_VEKT = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)
    }
}
