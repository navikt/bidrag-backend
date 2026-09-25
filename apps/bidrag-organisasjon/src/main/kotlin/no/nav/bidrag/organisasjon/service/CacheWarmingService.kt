package no.nav.bidrag.organisasjon.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/**
 * Service that warms up the cache on application startup.
 *
 * This service asynchronously pre-populates the cache for `hentPersonerEnhet` method
 * for configured enheter to improve response times for initial requests.
 *
 * Configuration:
 * - `cache.warming.enabled`: Enable/disable cache warming (default: false)
 * - `cache.warming.enheter`: Comma-separated list of enhet numbers to warm up (e.g., "4806,4833,4817")
 *
 * Example configuration in application.yaml:
 * ```yaml
 * cache:
 *   warming:
 *     enabled: true
 *     enheter: 4806,4833,4817,4812,4863,4842
 * ```
 */
@Service
@ConditionalOnProperty(name = ["cache.warming.enabled"], havingValue = "true", matchIfMissing = false)
class CacheWarmingService(
    private val organisasjonService: OrganisasjonService,
    @param:Value($$"${cache.warming.enheter:}") private val enhetList: String,
) {
    @EventListener(ApplicationReadyEvent::class)
    @Async
    fun warmupCacheOnStartup() {
        if (enhetList.isBlank()) {
            LOGGER.debug { "No enheter configured for cache warming, skipping..." }
            return
        }

        LOGGER.debug { "Starting cache warming for hentPersonerEnhet..." }
        try {
            val enheter = enhetList.split(",").map { it.trim() }.filter { it.isNotBlank() }
            LOGGER.debug { "Warming cache for ${enheter.size} enhet(er): $enheter" }

            enheter.forEach { enhet ->
                try {
                    LOGGER.debug { "Warming cache for enhet: $enhet" }
                    organisasjonService.hentPersonerEnhet(enhet)
                    LOGGER.debug { "Successfully warmed cache for enhet: $enhet" }
                } catch (e: Exception) {
                    LOGGER.warn { "Failed to warm cache for enhet: $enhet - ${e.message}" }
                }
            }
            LOGGER.debug { "Cache warming completed for ${enheter.size} enhet(er)" }
        } catch (e: Exception) {
            LOGGER.error(e) { "Error during cache warming" }
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }
}
