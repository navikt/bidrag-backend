package no.nav.bidrag.dokument.bestilling.model

import com.fasterxml.jackson.core.type.TypeReference
import org.springframework.core.ParameterizedTypeReference
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

inline fun <reified T : Any> parameterizedTypeReference(): ParameterizedTypeReference<T> = object : ParameterizedTypeReference<T>() {}

inline fun <reified T : Any> typeRef(): TypeReference<T> = object : TypeReference<T>() {}

val MAX_DATE = LocalDate.parse("9999-12-31")

fun getLastDayOfPreviousMonth(date: LocalDate?): LocalDate? {
    if (date == null) return date
    if (date.year == 9999) return date
    val cDate = GregorianCalendar.from(date.atStartOfDay(ZoneId.systemDefault()))
    // Subtract one month from the current date
    cDate.add(Calendar.MONTH, -1)
    // Set the day of the month to the last day of the month
    cDate.set(Calendar.DAY_OF_MONTH, cDate.getActualMaximum(Calendar.DAY_OF_MONTH))
    return LocalDate.from(cDate.toZonedDateTime())
}
