package no.nav.bidrag.cache.konfigurasjon.nøkkelgenerering

/**
 * Nøkkel for en cacheverdi. [brukerIdent] er null når verdien deles mellom alle brukere.
 * [toString] brukes som nøkkel i Valkey.
 */
data class CacheNøkkel(
    val metode: String,
    val brukerIdent: String?,
    val parametere: Any,
) {
    override fun toString(): String = listOfNotNull(metode, brukerIdent, parametere).joinToString("::")
}
