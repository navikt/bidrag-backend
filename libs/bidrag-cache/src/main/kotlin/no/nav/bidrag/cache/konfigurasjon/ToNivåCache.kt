package no.nav.bidrag.cache.konfigurasjon

import java.util.concurrent.Callable
import org.springframework.cache.Cache
import org.springframework.cache.support.SimpleValueWrapper

/**
 * Leser fra lokal cache først, deretter delt cache. Treff i delt cache legges i lokal cache.
 * Null-verdier caches ikke.
 */
class ToNivåCache(
    private val lokal: Cache,
    private val delt: Cache,
) : Cache {
    override fun getName(): String = lokal.name

    override fun getNativeCache(): Any = this

    override fun get(key: Any): Cache.ValueWrapper? {
        lokal.get(key)?.let { return it }
        val verdi = delt.get(key)?.get() ?: return null
        lokal.put(key, verdi)
        return SimpleValueWrapper(verdi)
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
        val verdi = valueLoader.call()
        put(key, verdi)
        return verdi
    }

    override fun put(
        key: Any,
        value: Any?,
    ) {
        if (value == null) return
        delt.put(key, value)
        lokal.put(key, value)
    }

    override fun evict(key: Any) {
        delt.evict(key)
        lokal.evict(key)
    }

    override fun clear() {
        delt.clear()
        lokal.clear()
    }
}
