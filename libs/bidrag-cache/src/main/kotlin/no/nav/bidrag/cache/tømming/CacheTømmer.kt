package no.nav.bidrag.cache.tømming

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.cache.tonivåcache.ToNivåCacheManager
import no.nav.bidrag.cache.util.NØKKELPREFIKS
import org.springframework.cache.CacheManager
import org.springframework.data.redis.cache.RedisCacheWriter
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.StringRedisTemplate

/**
 * Tømmer cache i Valkey og i Caffeine i alle podder. Valkey tømmes direkte. De andre poddene får beskjed
 * via pub/sub på [KANAL] og tømmer sin egen Caffeine. Podder som ikke er koblet til Valkey i øyeblikket
 * går glipp av beskjeden og beholder sine lokale verdier til de utløper.
 */
class CacheTømmer(
    private val kortCacheManager: CacheManager,
    private val langCacheManager: ToNivåCacheManager,
    private val valkeyCacheWriter: RedisCacheWriter,
    private val valkey: StringRedisTemplate,
) : MessageListener {
    /** Tømmer cachen [navn], eller alle cacher når [navn] er null. */
    fun tøm(navn: String?) {
        val mønster = if (navn == null) "$NØKKELPREFIKS*" else "$NØKKELPREFIKS$navn::*"
        valkeyCacheWriter.clear(navn ?: ALLE, mønster.toByteArray())
        tømLokalt(navn)
        valkey.convertAndSend(KANAL, navn ?: ALLE)
        LOGGER.info { "Tømte ${navn ?: "alle cacher"} og varslet de andre poddene" }
    }

    override fun onMessage(
        message: Message,
        pattern: ByteArray?,
    ) {
        val navn = message.body.decodeToString().takeUnless { it == ALLE }
        tømLokalt(navn)
        LOGGER.info { "Tømte ${navn ?: "alle cacher"} lokalt etter beskjed fra en annen pod" }
    }

    private fun tømLokalt(navn: String?) {
        val kortCacher = navn?.let(::listOf) ?: kortCacheManager.cacheNames
        kortCacher.forEach { kortCacheManager.getCache(it)?.clear() }
        langCacheManager.tømLokalt(navn)
    }

    companion object {
        const val KANAL = "bidrag-cache:tøm"
        private const val ALLE = "*"
        private val LOGGER = KotlinLogging.logger { }
    }
}
