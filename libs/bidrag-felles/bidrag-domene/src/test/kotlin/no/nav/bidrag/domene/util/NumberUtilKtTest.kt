package no.nav.bidrag.domene.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class NumberUtilKtTest {
    @Test
    fun `Skal avrunde tall til desimaler for tall med flere desimaler`() {
        val tallSomSkalAvrundes = BigDecimal("10.123456789101112")
        tallSomSkalAvrundes.avrundetMedToDesimaler shouldBe BigDecimal("10.12")
        tallSomSkalAvrundes.avrundetMedNullDesimaler shouldBe BigDecimal("10")
        tallSomSkalAvrundes.avrundetMedTiDesimaler shouldBe BigDecimal("10.1234567891")
    }

    @Test
    fun `Skal avrunde tall til desimaler for tall med få desimaler`() {
        val tallSomSkalAvrundes = BigDecimal("10.1")
        tallSomSkalAvrundes.avrundetMedToDesimaler shouldBe BigDecimal("10.10")
        tallSomSkalAvrundes.avrundetMedNullDesimaler shouldBe BigDecimal("10")
        tallSomSkalAvrundes.avrundetMedTiDesimaler shouldBe BigDecimal("10.1000000000")
    }

    @Test
    fun `Skal avrunde tall til nærmeste tier`() {
        BigDecimal("1024").avrundetTilNærmesteTier shouldBe BigDecimal("1020")
        BigDecimal("1021").avrundetTilNærmesteTier shouldBe BigDecimal("1020")
        BigDecimal("1025").avrundetTilNærmesteTier shouldBe BigDecimal("1030")
        BigDecimal("1026").avrundetTilNærmesteTier shouldBe BigDecimal("1030")
    }
}
