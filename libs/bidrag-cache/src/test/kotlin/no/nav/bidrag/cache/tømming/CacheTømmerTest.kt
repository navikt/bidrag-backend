package no.nav.bidrag.cache.tømming

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.DefaultMessage
import org.springframework.data.redis.core.StringRedisTemplate

class CacheTømmerTest {
    private val kort = ConcurrentMapCacheManager("kort1", "kort2")
    private val lang = ConcurrentMapCacheManager("lang")
    private val cacheWriter = mockk<RedisCacheWriter>(relaxed = true)
    private val template = mockk<StringRedisTemplate> { every { convertAndSend(any(), any()) } returns 1L }

    private fun fyllCacher() = listOf("kort1", "kort2").forEach { kort.getCache(it)!!.put("nøkkel", "verdi") }.also {
        lang.getCache("lang")!!.put("nøkkel", "verdi")
    }

    private fun verdi(
        manager: ConcurrentMapCacheManager,
        navn: String,
    ) = manager.getCache(navn)!!.get("nøkkel")?.get()

    @Test
    fun `skal tømme alle lokale cacher uten valkey`() {
        fyllCacher()

        CacheTømmer(kort, lang, null).tøm(null)

        verdi(kort, "kort1") shouldBe null
        verdi(kort, "kort2") shouldBe null
        verdi(lang, "lang") shouldBe null
    }

    @Test
    fun `skal bare tømme cachen med gitt navn`() {
        fyllCacher()

        CacheTømmer(kort, lang, null).tøm("kort1")

        verdi(kort, "kort1") shouldBe null
        verdi(kort, "kort2") shouldBe "verdi"
        verdi(lang, "lang") shouldBe "verdi"
    }

    @Test
    fun `skal tømme valkey og varsle andre podder`() {
        CacheTømmer(kort, lang, CacheTømmer.Valkey(cacheWriter, template)).tøm("lang")

        verify { cacheWriter.clear("lang", "bidrag-cache::lang::*".toByteArray()) }
        verify { template.convertAndSend(CacheTømmer.KANAL, "lang") }
    }

    @Test
    fun `skal bare tømme lokalt ved beskjed fra en annen pod`() {
        fyllCacher()

        CacheTømmer(kort, lang, CacheTømmer.Valkey(cacheWriter, template))
            .onMessage(DefaultMessage(CacheTømmer.KANAL.toByteArray(), "lang".toByteArray()), null)

        verdi(lang, "lang") shouldBe null
        verdi(kort, "kort1") shouldBe "verdi"
        verify(exactly = 0) { cacheWriter.clear(any(), any()) }
    }
}
