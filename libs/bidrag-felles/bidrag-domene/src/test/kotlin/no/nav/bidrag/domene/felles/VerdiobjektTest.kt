package no.nav.bidrag.domene.felles

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.sak.Saksnummer
import org.junit.jupiter.api.Test

class VerdiobjektTest {
    @Test
    fun `verdier() for et set av verdiobjekter returnerer et sett med verdier`() {
        val saksnumre = setOf(Saksnummer("1234567"), Saksnummer("7654321"), Saksnummer("7142589"))

        val verdier = saksnumre.verdier()

        verdier::class shouldBe LinkedHashSet::class
        verdier shouldHaveSize 3
        val first = verdier.first()
        first::class shouldBe String::class
    }

    @Test
    fun `verdier() for en liste med verdiobjekter returnerer en liste med verdier`() {
        val saksnumre = listOf(Saksnummer("1234567"), Saksnummer("7654321"), Saksnummer("7142589"))

        val verdier = saksnumre.verdier()

        verdier::class shouldBe ArrayList::class
        verdier shouldHaveSize 3
        val first = verdier.first()
        first::class shouldBe String::class
    }
}
