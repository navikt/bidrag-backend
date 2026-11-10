package no.nav.bidrag.cache.konfigurasjon.nøkkelgenerering

data class CacheBrukerNøkkel(
    val brukerIdent: String,
    val nøkkel: Any,
) {
    override fun toString(): String = "$brukerIdent::$nøkkel"
}