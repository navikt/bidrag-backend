package no.nav.bidrag.cache.konfigurasjon

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

/**
 * Properties for Spring Boot som appen kan overstyre i `application.yaml`, siden de legges bakerst med addLast.
 * - Helsesjekken for Redis er av. Cachen fortsetter uten Valkey, så nedetid i Valkey skal ikke påvirke oppstart.
 * - Vanlig `@Cacheable` bruker Caffeine. Ellers velger Spring Boot Redis når biblioteket har satt opp Valkey.
 */
class CacheProperties : EnvironmentPostProcessor {
    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        environment.propertySources.addLast(MapPropertySource(KILDE, STANDARDER))
    }

    private companion object {
        const val KILDE = "bidrag-cache-standarder"
        val STANDARDER: Map<String, Any> =
            mapOf(
                "management.health.redis.enabled" to false,
                "spring.cache.type" to "caffeine",
            )
    }
}
