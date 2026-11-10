package no.nav.bidrag.cache.konfigurasjon

import com.github.benmanes.caffeine.cache.Expiry
import no.nav.bidrag.cache.konfigurasjon.nøkkelgenerering.CacheNøkkel
import java.time.Duration

/** Levetiden fra annotasjonen når den er satt, ellers [defaultLevetid]. */
fun finnLevetid(
    nøkkel: Any,
    defaultLevetid: () -> Duration,
): Duration = (nøkkel as? CacheNøkkel)?.levetid ?: defaultLevetid()

/** Caffeine-ttl per nøkkel. Ny verdi på samme nøkkel får ny levetid, lesing forlenger ikke. */
class Levetid(
    private val defaultLevetid: () -> Duration,
) : Expiry<Any, Any> {
    override fun expireAfterCreate(
        key: Any,
        value: Any,
        currentTime: Long,
    ): Long = finnLevetid(key, defaultLevetid).toNanos()

    override fun expireAfterUpdate(
        key: Any,
        value: Any,
        currentTime: Long,
        currentDuration: Long,
    ): Long = finnLevetid(key, defaultLevetid).toNanos()

    override fun expireAfterRead(
        key: Any,
        value: Any,
        currentTime: Long,
        currentDuration: Long,
    ): Long = currentDuration
}
