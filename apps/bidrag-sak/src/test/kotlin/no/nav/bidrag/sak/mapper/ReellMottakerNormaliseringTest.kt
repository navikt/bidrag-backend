package no.nav.bidrag.sak.mapper

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.transport.sak.ReellMottakerDto
import no.nav.bidrag.transport.sak.RolleDto
import org.junit.jupiter.api.Test

@Suppress("DEPRECATION")
class ReellMottakerNormaliseringTest {
    private val barn = Personident("12345678901")
    private val person = ReellMottaker("01987654321")
    private val samhandler = ReellMottaker("80060878901")

    @Test
    fun `flytter RM fra utgått felt til reellMottaker`() {
        val rolle = RolleDto(fødselsnummer = barn, type = Rolletype.BARN, reellMottager = samhandler)

        val resultat = setOf(rolle).normaliserReellMottaker().single()

        resultat.reellMottaker shouldBe ReellMottakerDto(ident = samhandler)
        resultat.reellMottager shouldBe null
    }

    @Test
    fun `godtar samme RM i begge felt`() {
        val rolle = RolleDto(
            fødselsnummer = barn,
            type = Rolletype.BARN,
            reellMottaker = ReellMottakerDto(ident = person),
            reellMottager = person,
        )

        val resultat = setOf(rolle).normaliserReellMottaker().single()

        resultat.reellMottaker shouldBe ReellMottakerDto(ident = person)
        resultat.reellMottager shouldBe null
    }

    @Test
    fun `regner tom RM som ikke satt`() {
        val rolle = RolleDto(
            fødselsnummer = barn,
            type = Rolletype.BARN,
            reellMottaker = ReellMottakerDto(ident = ReellMottaker("")),
            reellMottager = ReellMottaker(""),
        )

        val resultat = setOf(rolle).normaliserReellMottaker().single()

        resultat.reellMottaker shouldBe null
        resultat.reellMottager shouldBe null
    }

    @Test
    fun `kaster feil når RM har ulik verdi i de to feltene`() {
        val rolle = RolleDto(
            fødselsnummer = barn,
            type = Rolletype.BARN,
            reellMottaker = ReellMottakerDto(ident = samhandler),
            reellMottager = person,
        )

        shouldThrow<IllegalArgumentException> { setOf(rolle).normaliserReellMottaker() }
            .message shouldBe "RM er oppgitt med ulik verdi i reellMottaker og reellMottager."
    }
}
