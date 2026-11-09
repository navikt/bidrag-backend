package no.nav.bidrag.cache.konfigurasjon

import java.time.Clock
import java.time.Duration
import java.time.Instant

internal data class CachetVerdi(
    val verdi: Any,
    val utløperPåMillis: Long,
) {
    fun gjennværendeTtl(clock: Clock): Duration = Duration.between(clock.instant(), Instant.ofEpochMilli(utløperPåMillis))
}
