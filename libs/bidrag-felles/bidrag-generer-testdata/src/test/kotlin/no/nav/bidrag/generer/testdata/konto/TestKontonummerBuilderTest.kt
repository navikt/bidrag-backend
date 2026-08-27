package no.nav.bidrag.generer.testdata.konto

import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class TestKontonummerBuilderTest {
    @Test
    fun skalGenerereKontonummer() {
        val kontonummer = genererKontonummer().opprett()
        kontonummer shouldNotBe null
        println(kontonummer)
    }

    @Test
    fun skalGenerereUtenlandskKontonummer() {
        val kontonummer = genererKontonummer().norskKontonummer(false).opprett()
        kontonummer shouldNotBe null
        println(kontonummer)
    }
}
