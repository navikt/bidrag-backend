package no.nav.bidrag.tilgangskontroll.tjeneste

import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class CacheService(
    private val cacheManager: CacheManager,
) {
    fun tømCache(navn: String) {
        cacheManager.getCache(navn)?.invalidate()
    }
}
