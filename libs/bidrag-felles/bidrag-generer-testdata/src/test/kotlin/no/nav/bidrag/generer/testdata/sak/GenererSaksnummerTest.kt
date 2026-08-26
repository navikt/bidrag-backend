package no.nav.bidrag.generer.testdata.sak

import io.kotest.matchers.shouldNotBe
import org.junit.Test
import java.time.Year

class GenererSaksnummerTest {
    @Test
    fun skalGenerereSaksnummer() {
        val saksnummer = genererSaksnummer()
        saksnummer shouldNotBe null
        println(saksnummer)
    }

    @Test
    fun skalGenerereSaksnummerSomMåPaddes() {
        val saksnummer = genererSaksnummer(Year.of(2000))
        saksnummer shouldNotBe null
        println(saksnummer)
    }
}
