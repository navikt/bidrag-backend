package no.nav.bidrag.dokument.journalpost.model

import java.time.Duration
import java.time.LocalDateTime
import java.util.function.Supplier

class Cacheable<T>(
    private val duration: Duration,
) {
    private var cached: T? = null
    private lateinit var cachedTime: LocalDateTime

    fun fetchOrRenew(cacheSupplier: Supplier<T>): T {
        if (cached != null && LocalDateTime.now().minus(duration).isBefore(cachedTime)) {
            return cached!!
        }

        cached = cacheSupplier.get()
        cachedTime = LocalDateTime.now()

        return cached!!
    }
}
