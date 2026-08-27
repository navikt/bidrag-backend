package no.nav.bidrag.domene.ident

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SamhandlerIdTest {
    @Test
    fun `gyldig returnerer false for annet enn 11 siffer`() {
        SamhandlerId("987654321987").gyldig() shouldBe false
        SamhandlerId("9876543210").gyldig() shouldBe false
        SamhandlerId("98765432101").gyldig() shouldBe true
    }

    @Test
    fun `gyldig() krever at samhandlerId begynner med 8 eller 9`() {
        SamhandlerId("08765432101").gyldig() shouldBe false
        SamhandlerId("18765432101").gyldig() shouldBe false
        SamhandlerId("28765432101").gyldig() shouldBe false
        SamhandlerId("38765432101").gyldig() shouldBe false
        SamhandlerId("48765432101").gyldig() shouldBe false
        SamhandlerId("58765432101").gyldig() shouldBe false
        SamhandlerId("68765432101").gyldig() shouldBe false
        SamhandlerId("78765432101").gyldig() shouldBe false
        SamhandlerId("98765432101").gyldig() shouldBe true
        SamhandlerId("89765432101").gyldig() shouldBe true
    }
}
