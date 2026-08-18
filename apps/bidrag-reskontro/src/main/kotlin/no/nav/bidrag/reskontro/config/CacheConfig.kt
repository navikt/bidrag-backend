package no.nav.bidrag.reskontro.config

import no.nav.bidrag.commons.cache.EnableUserCache
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@EnableCaching
@Profile(value = ["!test"]) // Ignore cache on tests
@EnableUserCache
class CacheConfig {
//    companion object {
//        const val PERSON_CACHE = "PERSON_CACHE"
//    }
//
//    @Bean
//    fun cacheManager(): CacheManager {
//        val caffeineCacheManager = CaffeineCacheManager()
//        caffeineCacheManager.registerCustomCache(
//            PERSON_CACHE,
//            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
//        )
//        return caffeineCacheManager
//    }
}
