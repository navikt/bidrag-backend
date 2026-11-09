package no.nav.bidrag.cache.konfigurasjon

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.cache.Cache
import org.springframework.cache.interceptor.SimpleCacheErrorHandler

private val LOGGER = KotlinLogging.logger { }

/** Gjør at kall fortsetter ved lese- og skrivefeil til cache */
class CacheErrorHandler(
    private val registry: MeterRegistry,
) : SimpleCacheErrorHandler() {
    override fun handleCacheGetError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
    ) = recordError("get", exception)

    override fun handleCachePutError(
        exception: RuntimeException,
        cache: Cache,
        key: Any,
        value: Any?,
    ) = recordError("put", exception)

    private fun recordError(
        operation: String,
        exception: RuntimeException,
    ) {
        registry.counter("bidrag.cache.error", "operation", operation).increment()
        // Nøkkel, verdi og exception-melding kan inneholde personopplysninger og skal ikke logges her.
        LOGGER.error { "Valkey-cache feilet ved $operation (${exception.javaClass.simpleName}); henter fra kilden" }
    }
}
