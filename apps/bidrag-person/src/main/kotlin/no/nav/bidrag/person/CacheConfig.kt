package no.nav.bidrag.person

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.bidrag.commons.cache.BrukerCacheKonfig
import no.nav.bidrag.commons.cache.InvaliderCacheFørStartenAvArbeidsdag
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary

@Configuration
@EnableCaching
@Import(BrukerCacheKonfig::class)
class CacheConfig {
    @Bean
    @Primary
    fun cacheManager() = CaffeineCacheManager().apply {
        registerCustomCache(KOMMUNER_CACHE, Caffeine.newBuilder().build())
        registerCustomCache(POSTNUMMERE_CACHE, Caffeine.newBuilder().build())
    }

    @Bean
    @Qualifier("pdl")
    fun cacheManagerPdl() = CaffeineCacheManager().apply {
        setCaffeine(Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()))
    }

    @Bean
    @Qualifier("krr")
    fun cacheManagerKrr() = CaffeineCacheManager().apply {
        setCaffeine(Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()))
    }

    @Bean
    @Qualifier("kontoregister")
    fun cacheManagerKontoregister() = CaffeineCacheManager().apply {
        setCaffeine(Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()))
    }

    companion object {
        const val KOMMUNER_CACHE = "KOMMUNER_CACHE"
        const val POSTNUMMERE_CACHE = "POSTNUMMERE_CACHE"
    }
}
