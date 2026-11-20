package no.nav.bidrag.cache.konfigurasjon

import no.nav.bidrag.cache.util.VALKEY_INSTANS
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata
import org.springframework.context.EnvironmentAware
import org.springframework.core.env.Environment

/**
 * Hindrer Spring Boot i å sette opp Redis mot localhost når appen ikke har Valkey. Ellers ville
 * helsesjekken for Redis vist DOWN. Apper som selv konfigurerer `spring.data.redis` berøres ikke.
 */
class ValkeyAutoConfigFilter :
    AutoConfigurationImportFilter,
    EnvironmentAware {
    private var brukerRedis = true

    override fun setEnvironment(environment: Environment) {
        brukerRedis = REDIS_EGENSKAPER.any(environment::containsProperty)
    }

    override fun match(
        autoConfigurationClasses: Array<String?>,
        autoConfigurationMetadata: AutoConfigurationMetadata,
    ): BooleanArray = BooleanArray(autoConfigurationClasses.size) {
        brukerRedis || autoConfigurationClasses[it]?.startsWith(REDIS_AUTOKONFIGURASJON) != true
    }

    private companion object {
        const val REDIS_AUTOKONFIGURASJON = "org.springframework.boot.data.redis."
        val REDIS_EGENSKAPER = listOf(VALKEY_INSTANS, "spring.data.redis.host", "spring.data.redis.url")
    }
}
