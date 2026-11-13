package no.nav.bidrag.cache.nøkkelgenerering

/**
 * Implementeres av applikasjonen for å skille cacheverdier per bruker når `perBruker = true`.
 */
fun interface CacheBrukerIdentifikator {
    fun hentBrukerIdent(): String
}
