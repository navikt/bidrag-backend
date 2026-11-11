package no.nav.bidrag.cache.konfigurasjon

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.Cache
import java.util.concurrent.Callable

/**
 * Leser fra caffeine cache først, deretter valkey cache. Treff i valkey legges i caffeine.
 * Null-verdier caches. Feil ved lesing og lagring i valkey stopper ikke kallet.
 * Feil ved sletting kastes videre, slik at utdaterte verdier ikke blir liggende uten at noen merker det.
 */
class ToNivåCache(
    private val caffeine: Cache,
    private val valkey: Cache,
) : Cache {
    override fun getName(): String = caffeine.name

    override fun getNativeCache(): Any = this

    // Treff gir en ValueWrapper, også når verdien er null. Bom gir null.
    override fun get(key: Any): Cache.ValueWrapper? {
        caffeine.get(key)?.let { return it }
        val treffIValkey = hentFraValkey(key) ?: return null
        caffeine.put(key, treffIValkey.get())
        return treffIValkey
    }

    override fun <T : Any> get(
        key: Any,
        type: Class<T>?,
    ): T? {
        val verdi = get(key)?.get()
        check(verdi == null || type == null || type.isInstance(verdi)) { "Cacheverdi har feil type" }
        @Suppress("UNCHECKED_CAST")
        return verdi as T?
    }

    override fun <T : Any> get(
        key: Any,
        valueLoader: Callable<T>,
    ): T? {
        @Suppress("UNCHECKED_CAST")
        get(key)?.let { return it.get() as T? }
        val verdi =
            try {
                valueLoader.call()
            } catch (e: Exception) {
                throw Cache.ValueRetrievalException(key, valueLoader, e)
            }
        put(key, verdi)
        return verdi
    }

    override fun put(
        key: Any,
        value: Any?,
    ) {
        lagreIValkey(key, value)
        caffeine.put(key, value)
    }

    override fun evict(key: Any) {
        valkey.evict(key)
        caffeine.evict(key)
    }

    override fun clear() {
        valkey.clear()
        caffeine.clear()
    }

    fun tømLokalt() = caffeine.clear()

    // Feil mot valkey feks ved nedetid eller tidsavbrudd behandles som cache-bom.
    private fun hentFraValkey(key: Any): Cache.ValueWrapper? = try {
        valkey.get(key)
    } catch (e: RuntimeException) {
        LOGGER.warn { "Klarte ikke lese fra valkey $name (${e.javaClass.simpleName}). Behandles som cache-bom." }
        secureLogger.warn { "Klarte ikke lese $key fra valkey $name (${e.javaClass.simpleName}). Behandles som cache-bom." }
        null
    }

    private fun lagreIValkey(
        key: Any,
        value: Any?,
    ) {
        try {
            valkey.put(key, value)
        } catch (e: RuntimeException) {
            LOGGER.warn { "Klarte ikke lagre i valkey $name (${e.javaClass.simpleName}). Lagres kun i caffeine." }
            secureLogger.warn { "Klarte ikke lagre $key i valkey $name (${e.javaClass.simpleName}). Lagres kun i caffeine." }
        }
    }

    private companion object {
        private val LOGGER = KotlinLogging.logger { }
        val secureLogger = KotlinLogging.logger("secureLogger")
    }
}
