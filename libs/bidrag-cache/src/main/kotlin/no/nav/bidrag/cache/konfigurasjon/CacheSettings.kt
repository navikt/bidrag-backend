package no.nav.bidrag.cache.konfigurasjon

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

data class CacheSettings(
    val valkeyInstans: String,
    val cacher: Map<String, CacheExpiry>,
    val brukerId: () -> String?,
    val nøkkelprefix: (Any) -> String? = { null }, // Nøkler som ikke er basic types (String, Number, Boolean, Enum eller List) bør legges til
) {
    init {
        require(valkeyInstans.isNotBlank()) { "Valkey-instans må være angitt" }
        require(cacher.isNotEmpty()) { "Minst en cache må ha en utløpsregel" }
        require(cacher.keys.all { it.matches(Regex("[A-Za-z0-9._-]+")) }) { "Ugyldig cache-navn" }
    }
}

data class CacheExpiry(
    val ttl: Duration? = null,
    val expireAt: LocalTime? = null,
) {
    init {
        require((ttl == null) != (expireAt == null)) { "Velg enten ttl eller expireAt for en cache" }
        require(ttl == null || ttl.isPositive) { "Cachelevetid må være positiv" }
    }

    internal fun deadline(clock: Clock): Instant = expireAt?.let {
        // Neste kalenderdag i JVM-ens tidssone bevarer regelen gjennom sommertid.
        clock.instant().atZone(clock.zone).toLocalDate().plusDays(1).atTime(it).atZone(clock.zone).toInstant()
    } ?: clock.instant().plus(checkNotNull(ttl))
}
