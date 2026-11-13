package no.nav.bidrag.cache.tonivåcache

import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCache

class ToNivåCacheTest {
    private class FeilendeValkey : ConcurrentMapCache("test") {
        override fun get(key: Any): Cache.ValueWrapper = throw IllegalStateException("Valkey er nede")

        override fun put(
            key: Any,
            value: Any?,
        ): Unit = throw IllegalStateException("Valkey er nede")
    }

    private val caffeine = ConcurrentMapCache("test")
    private val valkey = ConcurrentMapCache("test")
    private val metrikker = SimpleMeterRegistry()
    private val cache = ToNivåCache(caffeine, valkey, metrikker)

    @Test
    fun `skal lagre i begge nivåer`() {
        cache.put("nøkkel", "verdi")

        caffeine.get("nøkkel")?.get() shouldBe "verdi"
        valkey.get("nøkkel")?.get() shouldBe "verdi"
    }

    @Test
    fun `skal legge treff fra valkey i caffeine`() {
        valkey.put("nøkkel", "verdi")

        cache.get("nøkkel")?.get() shouldBe "verdi"
        caffeine.get("nøkkel")?.get() shouldBe "verdi"
        metrikker.counter("bidrag.cache.valkey.gets", "cache", "test", "result", "hit").count() shouldBe 1.0
    }

    @Test
    fun `skal gi bom når verdien mangler i begge nivåer`() {
        cache.get("nøkkel") shouldBe null
        metrikker.counter("bidrag.cache.valkey.gets", "cache", "test", "result", "miss").count() shouldBe 1.0
    }

    @Test
    fun `skal cache null`() {
        cache.put("nøkkel", null)

        cache.get("nøkkel")?.get() shouldBe null
        (cache.get("nøkkel") != null) shouldBe true
    }

    @Test
    fun `skal behandle feil ved lesing fra valkey som bom`() {
        val cache = ToNivåCache(caffeine, FeilendeValkey(), metrikker)

        cache.get("nøkkel") shouldBe null
        metrikker.counter("bidrag.cache.error", "cache", "test", "operasjon", "get").count() shouldBe 1.0
    }

    @Test
    fun `skal lagre i caffeine når lagring i valkey feiler`() {
        val cache = ToNivåCache(caffeine, FeilendeValkey(), metrikker)

        cache.put("nøkkel", "verdi")

        caffeine.get("nøkkel")?.get() shouldBe "verdi"
        metrikker.counter("bidrag.cache.error", "cache", "test", "operasjon", "put").count() shouldBe 1.0
    }

    @Test
    fun `skal hente fra kilden og lagre ved bom`() {
        cache.get("nøkkel") { "fra kilden" } shouldBe "fra kilden"

        valkey.get("nøkkel")?.get() shouldBe "fra kilden"
    }

    @Test
    fun `skal slette fra begge nivåer`() {
        cache.put("nøkkel", "verdi")

        cache.evict("nøkkel")

        caffeine.get("nøkkel") shouldBe null
        valkey.get("nøkkel") shouldBe null
    }

    @Test
    fun `skal bare tømme caffeine ved lokal tømming`() {
        cache.put("nøkkel", "verdi")

        cache.tømLokalt()

        caffeine.get("nøkkel") shouldBe null
        valkey.get("nøkkel")?.get() shouldBe "verdi"
    }
}
