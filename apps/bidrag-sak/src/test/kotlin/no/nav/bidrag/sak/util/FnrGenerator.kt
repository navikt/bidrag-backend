package no.nav.bidrag.sak.util

/**
 * Fødselsnummergenerator. Genererer tilfeldige fødselsnumre med mulighet for å spesifisere år, måned, dato og D-nummer.
 */
object FnrGenerator {
    private val k1Vekting = intArrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2)
    private val k2Vekting = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)

    fun generer(
        år: Int = (1854..2039).random(),
        måned: Int = (1..12).random(),
        dag: Int = (1..28).random(),
        type: FnrType = FnrType.Vanlig,
    ): String {
        if (år > 2039 || år < 1854) {
            error("Ugyldig årstall. Lovlige verdier er mellom 1854 og 2039")
        }

        val datoString = formater(år, måned, dag, type)

        while (true) {
            val fødselsnummer = lagFødselsnummer(år, datoString)
            if (fødselsnummer.length == 11) {
                return fødselsnummer
            }
        }
    }

    private fun lagFødselsnummer(
        år: Int,
        datoString: String,
    ): String {
        val personnummerUtenSjekksiffer =
            when (år / 100) {
                19 -> (0..499).random().toString().padStart(3, '0')
                18 -> (500..749).random().toString().padStart(3, '0')
                20 -> (500..999).random().toString().padStart(3, '0')
                else -> error("Ugylidg århundre")
            }
        val verdi = datoString + personnummerUtenSjekksiffer

        val siffer = verdi.chunked(1).map { it.toInt() }

        val kontrollMod1 = 11 - (0..8).sumOf { k1Vekting[it] * siffer[it] } % 11
        val kontrollsiffer1 = kontrollSiffer(kontrollMod1)

        val sifferMedEttKontrollsiffer = siffer + kontrollsiffer1
        val kontrollMod2 = 11 - (0..9).sumOf { k2Vekting[it] * sifferMedEttKontrollsiffer[it] } % 11
        val kontrollsiffer2 = kontrollSiffer(kontrollMod2)

        return verdi + kontrollsiffer1 + kontrollsiffer2
    }

    private fun formater(
        år: Int,
        måned: Int,
        dag: Int,
        type: FnrType,
    ): String {
        val dagString = if (type == FnrType.DNummer) pad2(dag + 40) else pad2(dag)
        val månedString =
            when (type) {
                FnrType.NAVSyntetisk -> (måned + 40).toString()
                FnrType.SkattSyntetisk -> (måned + 80).toString()
                else -> pad2(måned)
            }
        val årString = pad2(år % 100)
        return dagString + månedString + årString
    }

    private fun pad2(i: Int) = i.toString().padStart(2, '0')

    private fun kontrollSiffer(kontrollMod: Int): Int {
        if (kontrollMod == 11) {
            return 0
        }
        return kontrollMod
    }

    enum class FnrType {
        Vanlig,
        DNummer,
        NAVSyntetisk,
        SkattSyntetisk,
    }
}
