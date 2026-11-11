package no.nav.bidrag.cache.nøkkelgenerering

import com.fasterxml.jackson.annotation.JsonValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test

class CacheNøkkelTest {

    @Test
    fun `skal ikke ha brukerident eller parametere i klartekst i nøkkelen`() {
        val nøkkel = CacheNøkkel("PersonService.hent", "1234", listOf("9876"))

        nøkkel.toString() shouldStartWith "PersonService.hent::"
        nøkkel.toString() shouldNotContain "1234"
        nøkkel.toString() shouldNotContain "9876"
    }

    @Test
    fun `skal gi samme nøkkel for samme input`() {
        CacheNøkkel("A.b", "bruker", listOf("x")).toString() shouldBe CacheNøkkel("A.b", "bruker", listOf("x")).toString()
    }

    @Test
    fun `skal gi ulik nøkkel for ulike brukere`() {
        CacheNøkkel("A.b", "bruker1", listOf("x")).toString() shouldNotBe CacheNøkkel("A.b", "bruker2", listOf("x")).toString()
    }

    @Test
    fun `skal gi ulik nøkkel når parametere har lik maskert toString`() {
        val første = MaskertIdent("12345")
        val andre = MaskertIdent("02040")
        første.toString() shouldBe andre.toString()

        CacheNøkkel("A.b", null, listOf(første)).toString() shouldNotBe CacheNøkkel("A.b", null, listOf(andre)).toString()
    }

    @Test
    fun `skal gi samme nøkkel uavhengig av rekkefølgen i map`() {
        val nøkkel1 = CacheNøkkel("A.b", null, listOf(linkedMapOf("a" to 1, "b" to 2)))
        val nøkkel2 = CacheNøkkel("A.b", null, listOf(linkedMapOf("b" to 2, "a" to 1)))

        nøkkel1.toString() shouldBe nøkkel2.toString()
    }

    class MaskertIdent(
        @get:JsonValue val verdi: String,
    ) {
        override fun toString(): String = verdi.mapIndexed { index, tegn -> if (index % 2 != 0) tegn else '*' }.joinToString("")
    }
}
