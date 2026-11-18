package no.nav.bidrag.commons.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LogSanitizerTest {

    @Test
    fun `uendret verdi uten kontrolltegn returneres som samme streng`() {
        "vanlig-verdi-123".sanitizeForLog() shouldBe "vanlig-verdi-123"
    }

    @Test
    fun `linjeskift med linefeed erstattes med understrek`() {
        "linje1\nlinje2".sanitizeForLog() shouldBe "linje1_linje2"
    }

    @Test
    fun `linjeskift med carriage return erstattes med understrek`() {
        "linje1\rlinje2".sanitizeForLog() shouldBe "linje1_linje2"
    }

    @Test
    fun `crlf-linjeskift erstattes med to understreker`() {
        "linje1\r\nlinje2".sanitizeForLog() shouldBe "linje1__linje2"
    }

    @Test
    fun `tab erstattes med understrek`() {
        "verdi\tmed-tab".sanitizeForLog() shouldBe "verdi_med-tab"
    }

    @Test
    fun `andre kontrolltegn i området 0000 til 001F erstattes med understrek`() {
        "verdi${'\u0007'}med-bell".sanitizeForLog() shouldBe "verdi_med-bell"
        "verdi${'\u001F'}med-unitseparator".sanitizeForLog() shouldBe "verdi_med-unitseparator"
        "verdi${'\u0000'}med-null".sanitizeForLog() shouldBe "verdi_med-null"
    }

    @Test
    fun `kontrolltegn i området 007F til 009F erstattes med understrek`() {
        "verdi${'\u007F'}med-delete".sanitizeForLog() shouldBe "verdi_med-delete"
        "verdi${'\u0085'}med-nextline".sanitizeForLog() shouldBe "verdi_med-nextline"
    }

    @Test
    fun `flere kontrolltegn etter hverandre gir like mange understreker`() {
        "a\n\n\nb".sanitizeForLog() shouldBe "a___b"
    }

    @Test
    fun `forsøk på forfalskning av logglinje med injisert loggnivå fjernes`() {
        val ondsinnetInput = "gyldig-verdi\nWARN Falsk loggmelding injisert av bruker"
        ondsinnetInput.sanitizeForLog() shouldBe "gyldig-verdi_WARN Falsk loggmelding injisert av bruker"
    }

    @Test
    fun `null-verdi gir strengen null`() {
        val verdi: String? = null
        verdi.sanitizeForLog() shouldBe "null"
    }

    @Test
    fun `tom streng returneres uendret`() {
        "".sanitizeForLog() shouldBe ""
    }

    @Test
    fun `ikke-streng verdi konverteres via toString`() {
        val tall: Any = 12345L
        tall.sanitizeForLog() shouldBe "12345"
    }

    @Test
    fun `objekt med kontrolltegn i toString saneres`() {
        val objekt =
            object {
                override fun toString() = "verdi\nmed-linjeskift"
            }
        (objekt as Any).sanitizeForLog() shouldBe "verdi_med-linjeskift"
    }

    @Test
    fun `norske tegn beholdes uendret`() {
        "æøå ÆØÅ".sanitizeForLog() shouldBe "æøå ÆØÅ"
    }

    @Test
    fun `resultatet er alltid en ny streng selv om input ikke har kontrolltegn`() {
        val original = "uendret"
        val sanert = original.sanitizeForLog()
        sanert shouldBe original
    }
}
