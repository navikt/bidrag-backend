package no.nav.bidrag.tilgangskontroll.konfigurasjon

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.bidrag.commons.cache.EnableUserCache
import no.nav.bidrag.commons.cache.InvaliderCacheFørStartenAvArbeidsdag
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
@Profile(value = ["!test"]) // Ignore cache on tests
@EnableUserCache
class CacheConfig {
    @Bean
    fun cacheManager(): CacheManager {
        val caffeineCacheManager = CaffeineCacheManager()

        caffeineCacheManager.registerCustomCache(
            Cache.PIP_SAK,
            Caffeine
                .newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS) // Cache for 1 day
                .recordStats()
                .build(),
        )

        caffeineCacheManager.registerCustomCache(
            Cache.TILGANGSMASKIN_KJERNEREGLER,
            Caffeine
                .newBuilder()
                .expireAfter(InvaliderCacheFørStartenAvArbeidsdag())
                .recordStats()
                .build(),
        )

        caffeineCacheManager.registerCustomCache(
            Cache.TILGANGSMASKIN_KJERNEREGLER_BULK,
            Caffeine
                .newBuilder()
                .expireAfter(InvaliderCacheFørStartenAvArbeidsdag())
                .recordStats()
                .build(),
        )

        caffeineCacheManager.registerCustomCache(
            Cache.TILGANGSMASKIN_KOMPLETTEREGLER,
            Caffeine
                .newBuilder()
                .expireAfter(InvaliderCacheFørStartenAvArbeidsdag())
                .recordStats()
                .build(),
        )

        caffeineCacheManager.registerCustomCache(
            Cache.TILGANGSMASKIN_KOMPLETTEREGLER_BULK,
            Caffeine
                .newBuilder()
                .expireAfter(InvaliderCacheFørStartenAvArbeidsdag())
                .recordStats()
                .build(),
        )

        caffeineCacheManager.registerCustomCache(
            Cache.BRUKERINFO,
            Caffeine
                .newBuilder()
                .expireAfter(InvaliderCacheFørStartenAvArbeidsdag())
                .recordStats()
                .build(),
        )

        caffeineCacheManager.registerCustomCache(
            Cache.BRUKERGRUPPER,
            Caffeine
                .newBuilder()
                .expireAfter(InvaliderCacheFørStartenAvArbeidsdag())
                .recordStats()
                .build(),
        )

        caffeineCacheManager.registerCustomCache(
            Cache.BRUKERENHETER,
            Caffeine
                .newBuilder()
                .expireAfter(InvaliderCacheFørStartenAvArbeidsdag())
                .recordStats()
                .build(),
        )
        caffeineCacheManager.registerCustomCache(
            Cache.GRUPPE_DETALJER,
            Caffeine
                .newBuilder()
                .expireAfter(InvaliderCacheFørStartenAvArbeidsdag())
                .recordStats()
                .build(),
        )

        caffeineCacheManager.registerCustomCache(
            Cache.BRUKERE_FOR_ENHET,
            Caffeine
                .newBuilder()
                .expireAfter(InvaliderCacheFørStartenAvArbeidsdag())
                .recordStats()
                .build(),
        )
        caffeineCacheManager.registerCustomCache(
            Cache.GRUPPE_DETALJER_TEMA,
            Caffeine
                .newBuilder()
                .expireAfter(InvaliderCacheFørStartenAvArbeidsdag())
                .recordStats()
                .build(),
        )

        return caffeineCacheManager
    }
}

object Cache {
    const val PIP_SAK = "PIP_SAK"
    const val TILGANGSMASKIN_KJERNEREGLER = "TILGANGSMASKIN_KJERNEREGLER"
    const val TILGANGSMASKIN_KJERNEREGLER_BULK = "TILGANGSMASKIN_KJERNEREGLER_BULK"
    const val TILGANGSMASKIN_KOMPLETTEREGLER = "TILGANGSMASKIN_KOMPLETTEREGLER"
    const val TILGANGSMASKIN_KOMPLETTEREGLER_BULK = "TILGANGSMASKIN_KOMPLETTEREGLER_BULK"
    const val BRUKERINFO = "BRUKERINFO"
    const val BRUKERGRUPPER = "BRUKERGRUPPER"
    const val BRUKERE_FOR_ENHET = "BRUKERE_FOR_ENHET"
    const val GRUPPE_DETALJER = "GRUPPE_DETALJER"
    const val GRUPPE_DETALJER_TEMA = "GRUPPE_DETALJER_TEMA"
    const val BRUKERENHETER = "BRUKERENHETER"

    enum class CacheType {
        PIP_SAK,
        TILGANGSMASKIN_KJERNEREGLER,
        TILGANGSMASKIN_KJERNEREGLER_BULK,
        TILGANGSMASKIN_KOMPLETTEREGLER,
        TILGANGSMASKIN_KOMPLETTEREGLER_BULK,
        TEMA,
        BRUKERGRUPPER,
    }
}
