package no.nav.bidrag.cache.konfigurasjon

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

class ValkeyAutoConfigFilterTest {
    private val klasser = arrayOf<String?>("org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration", "org.springframework.boot.Annet")

    private fun match(vararg egenskaper: Pair<String, String>): List<Boolean> {
        val filter = ValkeyAutoConfigFilter()
        filter.setEnvironment(MockEnvironment().apply { egenskaper.forEach { setProperty(it.first, it.second) } })
        return filter.match(klasser, mockk()).toList()
    }

    @Test
    fun `skal fjerne redis-autokonfigurasjon uten valkey`() {
        match() shouldBe listOf(false, true)
    }

    @Test
    fun `skal beholde redis-autokonfigurasjon med valkey`() {
        match("CACHE_VALKEY_INSTANCE" to "cache") shouldBe listOf(true, true)
    }

    @Test
    fun `skal beholde redis-autokonfigurasjon når appen selv har satt opp redis`() {
        match("spring.data.redis.host" to "localhost") shouldBe listOf(true, true)
    }
}
