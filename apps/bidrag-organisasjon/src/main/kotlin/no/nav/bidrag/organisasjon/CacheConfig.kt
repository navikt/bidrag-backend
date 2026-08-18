package no.nav.bidrag.organisasjon

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
@Profile("live")
class CacheConfig {
    @Bean
    fun cacheManager(): CacheManager {
        val caffeineCacheManager = CaffeineCacheManager()
        caffeineCacheManager.registerCustomCache(
            PERSON_GEOGRAFISK_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats()
                .build(),
        )
        caffeineCacheManager.registerCustomCache(
            PERSON_GRADERING_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats()
                .build(),
        )
        caffeineCacheManager.registerCustomCache(
            PERSON_ENHETER,
            Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats()
                .build(),
        )
        caffeineCacheManager.registerCustomCache(
            PERSONER_ENHET,
            Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats()
                .build(),
        )
        caffeineCacheManager.registerCustomCache(
            ARBEIDSFORDELING_ENHET,
            Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats()
                .build(),
        )
        caffeineCacheManager.registerCustomCache(
            ENTRA_PERSON_ENHETER,
            Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats()
                .build(),
        )
        caffeineCacheManager.registerCustomCache(
            ENTRA_BRUKERE_FOR_ENHET,
            Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats()
                .build(),
        )
        caffeineCacheManager.registerCustomCache(
            ENTRA_PERSON_INFORMASJON,
            Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats()
                .build(),
        )
        caffeineCacheManager.registerCustomCache(
            ENTRA_TILGANGER,
            Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats()
                .build(),
        )
        caffeineCacheManager.registerCustomCache(
            ENTRA_TEMA_SAKSBEHANDLERE,
            Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats()
                .build(),
        )
        return caffeineCacheManager
    }

    companion object {
        const val PERSON_GEOGRAFISK_CACHE = "PERSON_GEOGRAFISK_CACHE"
        const val PERSON_GRADERING_CACHE = "PERSON_GRADERING_CACHE"
        const val PERSON_ENHETER = "PERSON_ENHETER"
        const val PERSONER_ENHET = "PERSONER_ENHET"
        const val ARBEIDSFORDELING_ENHET = "ARBEIDSFORDELING_ENHET"
        const val ENTRA_PERSON_ENHETER = "ENTRA_PERSON_ENHETER"
        const val ENTRA_BRUKERE_FOR_ENHET = "ENTRA_BRUKERE_FOR_ENHET"
        const val ENTRA_PERSON_INFORMASJON = "ENTRA_PERSON_INFORMASJON"
        const val ENTRA_TILGANGER = "ENTRA_TILGANGER"
        const val ENTRA_TEMA_SAKSBEHANDLERE = "ENTRA_TEMA_SAKSBEHANDLERE"
    }
}
