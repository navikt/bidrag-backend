package no.nav.bidrag.cache.nøkkelgenerering

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test

class CacheNøkkelTest {
    @Test
    fun `skal ikke ha brukerident eller parametere i klartekst i nøkkelen`() {
        val nøkkel = CacheNøkkel("PersonService.hent", "12345678901", "98765432109")

        nøkkel.toString() shouldStartWith "PersonService.hent::"
        nøkkel.toString() shouldNotContain "12345678901"
        nøkkel.toString() shouldNotContain "98765432109"
    }

    @Test
    fun `skal gi samme nøkkel for samme input`() {
        CacheNøkkel("A.b", "bruker", "x").toString() shouldBe CacheNøkkel("A.b", "bruker", "x").toString()
    }

    @Test
    fun `skal gi ulik nøkkel for ulike brukere`() {
        CacheNøkkel("A.b", "bruker1", "x").toString() shouldNotBe CacheNøkkel("A.b", "bruker2", "x").toString()
    }
}
