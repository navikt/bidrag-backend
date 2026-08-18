package no.nav.bidrag.sak.service

import java.time.LocalDate

object SaksnummerSerie {
    @JvmStatic
    fun hentMinimumsgrenseForAarstall(): Int = hentNaaverendeAarstallSomToSiffer() * 100000

    fun hentMaksimumsgrenseForAarstall(): Int = (hentNaaverendeAarstallSomToSiffer() + 1) * 100000

    private fun hentNaaverendeAarstallSomToSiffer(): Int = LocalDate.now().year % 100
}
