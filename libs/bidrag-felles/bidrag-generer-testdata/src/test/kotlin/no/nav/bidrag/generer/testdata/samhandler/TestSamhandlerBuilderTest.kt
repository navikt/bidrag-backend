package no.nav.bidrag.generer.testdata.samhandler

import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class TestSamhandlerBuilderTest {
    @Test
    fun skalOppretteSamhandler() {
        val samhandler = genererSamhandler().opprett()
        samhandler shouldNotBe null
        println(samhandler)
    }
}
