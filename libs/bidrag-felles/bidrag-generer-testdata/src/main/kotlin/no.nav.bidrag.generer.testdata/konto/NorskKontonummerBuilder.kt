package no.nav.bidrag.generer.testdata.konto

import kotlin.random.Random

@Suppress("unused")
class NorskKontonummerBuilder {
    private val buffer = StringBuffer(11)

    fun bankNummer(bankNummer: Int): NorskKontonummerBuilder = set(0, 4, bankNummer)

    fun kontotype(kontotype: Int): NorskKontonummerBuilder = set(4, 2, kontotype)

    fun set(
        pos: Int,
        digits: Int,
        value: Int,
    ): NorskKontonummerBuilder {
        buffer.replace(pos, pos + digits, leftPad(digits, value.toString()))
        return this
    }

    fun kundenummer(kundenummer: Int): NorskKontonummerBuilder {
        set(6, 4, kundenummer)
        return beregnKontrollsiffer()
    }

    fun randomKundenummer(): NorskKontonummerBuilder? {
        try {
            set(6, 4, Random.nextInt(10000))
            return beregnKontrollsiffer()
        } catch (_: IllegalStateException) {
            return randomKundenummer()
        }
    }

    fun beregnKontrollsiffer(): NorskKontonummerBuilder {
        set(10, 1, kontrollsiffer())
        return this
    }

    fun leftPad(
        length: Int,
        str: String,
    ): String {
        val padded = str.padStart(length, '0')
        return padded.substring(padded.length - length)
    }

    private fun kontrollsiffer(): Int {
        var sum = 0
        for (i in VEKT.indices) {
            sum += buffer.substring(i, i + 1).toInt() * VEKT[i]
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
        private val VEKT = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2) // 5432765432
    }
}
