package no.nav.bidrag.cache.tolags

import com.github.benmanes.caffeine.cache.Caffeine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.cache.konfigurasjon.TwoLevelCache
import org.junit.jupiter.api.Test
import org.springframework.cache.Cache
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.concurrent.ConcurrentMapCache
import java.time.Clock
import java.time.Instant
import java.util.concurrent.Callable

class TwoLevelCacheTest {
    private var nå = Instant.parse("2026-01-02T05:30:00Z")
    private val klokke = mockk<Clock>().also { every { it.instant() } answers { nå } }
    private val deltCache = ConcurrentMapCache("test", false)

    private fun opprettCache(deltCache: Cache = this.deltCache): TwoLevelCache = TwoLevelCache(
        CaffeineCache("test", Caffeine.newBuilder().build(), false),
        deltCache,
        { nå.plusSeconds(10) },
        klokke,
    )

    @Test
    fun `skal kopiere opprinnelig utløpsfrist uten å fornye den`() {
        val første = opprettCache()
        val andre = opprettCache()
        første.put("key", "value")
        val lagret = deltCache.get("key").shouldNotBeNull().get()
        nå = nå.plusSeconds(9)

        andre.get("key", String::class.java) shouldBe "value"
        deltCache.get("key").shouldNotBeNull().get() shouldBe lagret
        nå = nå.plusSeconds(1)
        første.get("key") shouldBe null
        andre.get("key") shouldBe null
        deltCache.get("key").shouldNotBeNull().get() shouldBe lagret
    }

    @Test
    fun `skal hente fra kilden ved cache-bom og bruke lokalt og delt cachelag ved treff`() {
        val cacheMedLokalVerdi = opprettCache()
        val cacheUtenLokalVerdi = opprettCache()
        var antallKildekall = 0
        val hentFraKilde = Callable { "value-${++antallKildekall}" }

        cacheMedLokalVerdi.get("key", hentFraKilde) shouldBe "value-1"
        cacheMedLokalVerdi.get("key", hentFraKilde) shouldBe "value-1"
        cacheUtenLokalVerdi.get("key", hentFraKilde) shouldBe "value-1"
        nå = nå.plusSeconds(10)
        cacheMedLokalVerdi.get("key", hentFraKilde) shouldBe "value-2"
        antallKildekall shouldBe 2
    }

    @Test
    fun `skal ikke cache feil fra kilde`() {
        val cache = opprettCache()
        val feil = IllegalStateException("Kilden er nede")

        shouldThrow<Cache.ValueRetrievalException> {
            cache.get("key") { throw feil }
        }.cause shouldBe feil
        cache.get("key") shouldBe null
        deltCache.get("key") shouldBe null
    }

    @Test
    fun `skal ikke beholde lokal verdi når skriving til delt cache feiler`() {
        val deltCache = mockk<Cache>()
        every { deltCache.put(any(), any()) } throws IllegalStateException("Valkey er nede")
        val cache = opprettCache(deltCache)

        shouldThrow<IllegalStateException> { cache.put("key", "value") }
        cache.nativeCache.shouldBeInstanceOf<com.github.benmanes.caffeine.cache.Cache<*, *>>().estimatedSize() shouldBe 0L
    }

    @Test
    fun `skal avvise nullverdier og feil cacheformat`() {
        val cache = opprettCache()
        shouldThrow<IllegalArgumentException> { cache.put("key", null) }
        deltCache.get("key") shouldBe null
        deltCache.put("key", "ugyldig format")

        shouldThrow<IllegalStateException> { cache.get("key") }.message shouldBe "Ugyldig cacheformat"
    }
}
