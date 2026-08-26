package no.nav.bidrag.transport.samhandler

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class SamhandlerValideringsfeilTest {
    @Test
    fun `Skal legge til feltvalideringsfeil i liste hvis den ikke finnes fra før av`() {
        val feltValideringsfeilListe = mutableListOf<FeltValideringsfeil>()
        feltValideringsfeilListe.leggTil("feltnavn", "feilmelding")

        feltValideringsfeilListe.size shouldBe 1
        feltValideringsfeilListe[0].feltnavn shouldBe "feltnavn"
        feltValideringsfeilListe[0].feilmelding shouldBe "feilmelding"
    }

    @Test
    fun `Skal legge til feltvalideringsfeil i liste hvis den finnes fra før av`() {
        val feltValideringsfeilListe = mutableListOf<FeltValideringsfeil>()
        feltValideringsfeilListe.leggTil("feltnavn", "feilmelding")

        feltValideringsfeilListe.size shouldBe 1
        feltValideringsfeilListe[0].feltnavn shouldBe "feltnavn"
        feltValideringsfeilListe[0].feilmelding shouldBe "feilmelding"

        feltValideringsfeilListe.leggTil("feltnavn", "feilmelding2")
        feltValideringsfeilListe[0].feilmelding shouldBe "feilmelding, feilmelding2"
        feltValideringsfeilListe.size shouldBe 1
    }
}
