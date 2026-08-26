package no.nav.bidrag.generer.testdata.tid

import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Suppress("unused")
object DateUtils {
    val DEFAULT_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun parseDate(dateStr: String?): LocalDate? =
        if (dateStr != null) {
            LocalDate.parse(dateStr, DEFAULT_DATE_FORMAT)
        } else {
            null
        }
}
