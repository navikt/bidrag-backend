package no.nav.bidrag.cache.util

import io.kotest.matchers.shouldBe
import no.nav.bidrag.cache.nøkkelgenerering.CacheNøkkel
import org.junit.jupiter.api.Test
import java.time.Duration

class LevetidTest {
    private val standard = Duration.ofHours(1)
    private val levetid = Levetid { standard }

    @Test
    fun `skal bruke levetid fra nøkkelen når den er satt`() {
        val nøkkel = CacheNøkkel("A.b", null, "x", Duration.ofMinutes(5))

        levetid.expireAfterCreate(nøkkel, "verdi", 0) shouldBe Duration.ofMinutes(5).toNanos()
    }

    @Test
    fun `skal bruke standard levetid når nøkkelen ikke har levetid`() {
        levetid.expireAfterCreate(CacheNøkkel("A.b", null, "x"), "verdi", 0) shouldBe standard.toNanos()
    }

    @Test
    fun `skal bruke standard levetid for andre nøkkeltyper`() {
        levetid.expireAfterCreate("nøkkel", "verdi", 0) shouldBe standard.toNanos()
    }

    @Test
    fun `skal gi ny levetid ved oppdatering`() {
        levetid.expireAfterUpdate("nøkkel", "verdi", 0, 10) shouldBe standard.toNanos()
    }

    @Test
    fun `skal ikke forlenge levetiden ved lesing`() {
        levetid.expireAfterRead("nøkkel", "verdi", 0, 10) shouldBe 10
    }
}
