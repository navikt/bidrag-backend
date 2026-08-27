package no.nav.bidrag.domene.ident

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class OrganisasjonsnummerTest {
    @Test
    fun `gyldig() returnerer false ved lengder ulik 9`() {
        Organisasjonsnummer("9").gyldig() shouldBe false
        Organisasjonsnummer("98").gyldig() shouldBe false
        Organisasjonsnummer("987").gyldig() shouldBe false
        Organisasjonsnummer("9876").gyldig() shouldBe false
        Organisasjonsnummer("98765").gyldig() shouldBe false
        Organisasjonsnummer("987654").gyldig() shouldBe false
        Organisasjonsnummer("9876543").gyldig() shouldBe false
        Organisasjonsnummer("98765432").gyldig() shouldBe false
        Organisasjonsnummer("9876543210").gyldig() shouldBe false
        Organisasjonsnummer("98765432109").gyldig() shouldBe false
    }

    @Test
    fun `gyldig() returnerer false ved feilet modulo 11-sjekk`() {
        Organisasjonsnummer("826506932").gyldig() shouldBe false
        Organisasjonsnummer("913067386").gyldig() shouldBe false
        Organisasjonsnummer("925603320").gyldig() shouldBe false
        Organisasjonsnummer("926354751").gyldig() shouldBe false
        Organisasjonsnummer("927393373").gyldig() shouldBe false
        Organisasjonsnummer("922124212").gyldig() shouldBe false
        Organisasjonsnummer("925817926").gyldig() shouldBe false
        Organisasjonsnummer("930489103").gyldig() shouldBe false
        Organisasjonsnummer("928807449").gyldig() shouldBe false
        Organisasjonsnummer("924846123").gyldig() shouldBe false
        Organisasjonsnummer("924345460").gyldig() shouldBe false
    }

    @Test
    fun `gyldig() returnerer true for gyldige verdier`() {
        Organisasjonsnummer("826606932").gyldig() shouldBe true
        Organisasjonsnummer("913068386").gyldig() shouldBe true
        Organisasjonsnummer("921603320").gyldig() shouldBe true
        Organisasjonsnummer("926357751").gyldig() shouldBe true
        Organisasjonsnummer("927493373").gyldig() shouldBe true
        Organisasjonsnummer("922124612").gyldig() shouldBe true
        Organisasjonsnummer("925317926").gyldig() shouldBe true
        Organisasjonsnummer("930489603").gyldig() shouldBe true
        Organisasjonsnummer("928907449").gyldig() shouldBe true
        Organisasjonsnummer("924847123").gyldig() shouldBe true
        Organisasjonsnummer("929345460").gyldig() shouldBe true
    }
}
