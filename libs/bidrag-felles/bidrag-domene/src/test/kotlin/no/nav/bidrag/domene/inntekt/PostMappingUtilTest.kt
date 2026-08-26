package no.nav.bidrag.domene.inntekt

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PostMappingUtilTest {
    @Test
    fun `skal hente inntekter KAPS`() {
        val kaps = hentMappingerKapitalinntekt()
        kaps.size shouldBe 189
    }

    @Test
    fun `skal hente mappinger LIGS`() {
        val ligs = hentMappingerLigs()
        ligs.size shouldBe 84
    }
}
