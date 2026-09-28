package no.nav.bidrag.commons.util

private val KONTROLLTEGN = Regex("[\r\n\t\u0000-\u001F]")

/**
 * Fjerner linjeskift og andre kontrolltegn fra verdien før den logges, slik at
 * brukerkontrollert input ikke kan forfalske loggoppføringer (log-injection/CWE-117).
 * Returnerer alltid en ny streng, uavhengig av opprinnelig verdi.
 */
fun Any?.sanitizeForLog(): String = (this?.toString() ?: "null").replace(KONTROLLTEGN, "_")
