package no.nav.bidrag.cache.util

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
