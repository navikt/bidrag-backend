package no.nav.bidrag.cache.konfigurasjon

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

/**
 * Slår av Spring Boots helsesjekk for Redis. Cachen fortsetter uten Valkey, så nedetid i Valkey
 * skal ikke påvirke oppstart. Bruker addLast slik at appen kan overstyre med `management.health.redis.enabled: true`.
 */
class ValkeyHelsesjekk : EnvironmentPostProcessor {
    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        environment.propertySources.addLast(MapPropertySource(KILDE, mapOf(REDIS_HELSESJEKK to false)))
    }

    internal companion object {
        const val KILDE = "bidrag-cache-valkey-helsesjekk"
        const val REDIS_HELSESJEKK = "management.health.redis.enabled"
    }
}
