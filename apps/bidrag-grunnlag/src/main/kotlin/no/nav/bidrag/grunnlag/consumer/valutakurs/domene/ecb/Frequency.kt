package no.nav.bidrag.grunnlag.consumer.valutakurs.domene.ecb

import java.time.LocalDate

enum class Frequency {
    Daily,
    Monthly,
    ;

    fun toFrequencyParam() =
        when (this) {
            Daily -> "D"
            Monthly -> "M"
        }

    fun toQueryParams(exchangeRateDate: LocalDate) =
        when (this) {
            Daily -> "?startPeriod=$exchangeRateDate&endPeriod=$exchangeRateDate"
            Monthly -> "?endPeriod=$exchangeRateDate&lastNObservations=1"
        }
}
