package no.nav.bidrag.dokument.journalpost.configuration

import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.bidrag.commons.cache.EnableUserCache
import no.nav.bidrag.commons.cache.InvaliderCacheFørStartenAvArbeidsdag
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@EnableCaching
@Profile(BidragDokumentJournalpostProfiles.NAIS)
@EnableUserCache
class CacheConfig {
    @Bean
    fun cacheManager(): CacheManager {
        val caffeineCacheManager = CaffeineCacheManager()
        caffeineCacheManager.registerCustomCache(
            SAKSBEHANDLER_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            PERSON_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            TILGANG_TEMA_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            TILGANG_SAK_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        caffeineCacheManager.registerCustomCache(
            TILGANG_PERSON_CACHE,
            Caffeine.newBuilder().expireAfter(InvaliderCacheFørStartenAvArbeidsdag()).build(),
        )
        return caffeineCacheManager
    }

    companion object {
        const val SAKSBEHANDLER_CACHE = "SAKSBEHANDLER_CACHE"
        const val PERSON_CACHE = "PERSON_CACHE"
        const val TILGANG_SAK_CACHE = "TILGANG_SAK_CACHE"
        const val TILGANG_PERSON_CACHE = "TILGANG_PERSON_CACHE"
        const val TILGANG_TEMA_CACHE = "TILGANG_TEMA_CACHE"
    }
}
