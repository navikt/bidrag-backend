package no.nav.bidrag.grunnlag.consumer.valutakurs.domene.ecb

import java.time.LocalDate
import java.time.YearMonth

enum class Frequency {
    Daily,
    Monthly,
    ;

    fun toFrequencyParam() = when (this) {
        Daily -> "D"
        Monthly -> "M"
    }

    fun toQueryParams(exchangeRateDate: LocalDate) = when (this) {
        Daily -> "?startPeriod=$exchangeRateDate&endPeriod=$exchangeRateDate"
        Monthly -> "?startPeriod=${YearMonth.from(exchangeRateDate)}&endPeriod=${YearMonth.from(exchangeRateDate)}"
    }
}
