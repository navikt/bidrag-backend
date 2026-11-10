package no.nav.bidrag.cache.util

import com.github.benmanes.caffeine.cache.Expiry
import java.time.Duration
import java.time.LocalDateTime

fun tidTilStartenAvNesteArbeidsdag(): Duration {
    val nå = LocalDateTime.now()
    val utløpPåTidspunkt =
        nå
            .plusDays(1)
            .withHour(6)
            .withMinute(0)
            .withSecond(0)

    return Duration.between(nå, utløpPåTidspunkt)
}

class InvaliderCacheFørStartenAvArbeidsdag : Expiry<Any, Any> {
    override fun expireAfterCreate(
        p0: Any,
        p1: Any,
        currentTime: Long,
    ): Long = tidTilStartenAvNesteArbeidsdag().toNanos()

    override fun expireAfterUpdate(
        o1: Any,
        o2: Any,
        currentTime: Long,
        currentDuration: Long,
    ): Long = currentDuration

    override fun expireAfterRead(
        p0: Any,
        p1: Any,
        currentTime: Long,
        currentDuration: Long,
    ): Long = currentDuration
}
