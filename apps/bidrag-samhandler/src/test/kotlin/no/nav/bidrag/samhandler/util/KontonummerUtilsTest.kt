package no.nav.bidrag.samhandler.util

import no.nav.bidrag.generer.testdata.konto.genererKontonummer
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KontonummerUtilsTest {
    @Test
    fun `skal returnere true for gyldige kontonumre`() {
        val gyldigeKontonumre =
            listOf(
                genererKontonummer().norskKontonummer(true).opprett().norskKontonummer!!,
                genererKontonummer().norskKontonummer(true).opprett().norskKontonummer!!,
                genererKontonummer().norskKontonummer(true).opprett().norskKontonummer!!,
            )
        gyldigeKontonumre.forEach {
            assertTrue(KontonummerUtils.erGyldigKontonummerMod11(it), "Forventet gyldig: $it")
        }
    }

    @Test
    fun `skal returnere false for ugyldige kontonumre`() {
        val ugyldigeKontonumre =
            listOf(
                medUgyldigKontrollsiffer(genererKontonummer().norskKontonummer(true).opprett().norskKontonummer!!),
                medUgyldigKontrollsiffer(genererKontonummer().norskKontonummer(true).opprett().norskKontonummer!!),
                "abcdefghijk", // Ikke tall
                "123", // For kort
                "", // Tom streng
            )
        ugyldigeKontonumre.forEach {
            assertFalse(KontonummerUtils.erGyldigKontonummerMod11(it), "Forventet ugyldig: $it")
        }
    }

    // Endrer kontrollsifferet (siste siffer) i et gyldig, generert kontonummer slik at det blir ugyldig.
    private fun medUgyldigKontrollsiffer(gyldigKontonummer: String): String {
        val ugyldigKontrollsiffer = (gyldigKontonummer.last().digitToInt() + 1) % 10
        return gyldigKontonummer.replaceRange(gyldigKontonummer.length - 1, gyldigKontonummer.length, ugyldigKontrollsiffer.toString())
    }
}
