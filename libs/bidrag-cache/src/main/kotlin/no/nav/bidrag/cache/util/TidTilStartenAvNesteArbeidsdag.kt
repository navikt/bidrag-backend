package no.nav.bidrag.cache.util

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

private val NORSK_TIDSSONE = ZoneId.of("Europe/Oslo")

fun tidTilStartenAvNesteArbeidsdag(): Duration {
    val nå = ZonedDateTime.now(NORSK_TIDSSONE)
    val utløpPåTidspunkt =
        nå
            .plusDays(1)
            .withHour(6)
            .withMinute(0)
            .withSecond(0)

    return Duration.between(nå, utløpPåTidspunkt)
}
