package no.nav.bidrag.dokument.bestilling.config

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.bidrag.commons.cache.BrukerCacheKonfig
import no.nav.bidrag.commons.cache.InvaliderCacheFørStartenAvArbeidsdag
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import java.time.Duration

@Configuration
@EnableCaching
@Profile(value = ["!test"]) // Ignore cache on tests
@Import(BrukerCacheKonfig::class)
class CacheConfig {
    companion object {
        const val LANDKODER_CACHE = "LANDKODER_CACHE"
        const val LANDKODER_ISO2_CACHE = "LANDKODER_ISO2_CACHE"
        const val SJABLONGER_CACHE = "SJABLONGER_CACHE"
        const val PERSON_CACHE = "PERSON_CACHE"
        const val VEDTAK_CACHE = "VEDTAK_CACHE"
        const val BEHANDLING_CACHE = "BEHANDLING_CACHE"
        const val PERSON_ADRESSE_CACHE = "PERSON_ADRESSE_CACHE"
        const val PERSON_SPRAAK_CACHE = "PERSON_SPRAAK_CACHE"
        const val SAKSBEHANDLERINFO_CACHE = "SAKSBEHANDLERINFO_CACHE"
        const val ENHETINFO_CACHE = "ENHETINFO_CACHE"
        const val ENHETKONTAKTINFO_CACHE = "ENHETKONTAKTINFO_CACHE"
    }

    @Bean
    fun cacheManager(): CacheManager {
        val caffeineCacheManager = CaffeineCacheManager()
        caffeineCacheManager.registerCustomCache(
            SAKSBEHANDLERINFO_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            ENHETINFO_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            ENHETKONTAKTINFO_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            PERSON_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            VEDTAK_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            BEHANDLING_CACHE,
            Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).build(),
        )
        caffeineCacheManager.registerCustomCache(
            PERSON_ADRESSE_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            PERSON_SPRAAK_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        return caffeineCacheManager
    }
}
