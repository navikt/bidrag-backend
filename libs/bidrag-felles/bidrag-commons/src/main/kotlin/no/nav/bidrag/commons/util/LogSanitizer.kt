package no.nav.bidrag.commons.util

/**
 * Fjerner linjeskift og andre kontrolltegn fra verdien før den logges, slik at
 * brukerkontrollert input ikke kan forfalske loggoppføringer.
 * Returnerer alltid en ny streng, uavhengig av opprinnelig verdi.
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun Any?.sanitizeForLog(): String {
    val verdi = (this?.toString() ?: "null") as java.lang.String
    return verdi.replaceAll("[^\\P{Cc}]", "_")
}
