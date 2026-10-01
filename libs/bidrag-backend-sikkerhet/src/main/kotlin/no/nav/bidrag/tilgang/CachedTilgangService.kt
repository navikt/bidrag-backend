package no.nav.bidrag.tilgang

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.tilgang.TilgangskontrollResponse
import java.time.Duration

/**
 * Cacher tilgangssvar fra [TilgangClient] per innlogget saksbehandler.
 *
 * Svaret gjelder saksbehandleren som gjør kallet, så cachenøkkelen inneholder NAV-ident.
 * Både tilgang og avslag caches i [ttl]. Feil mot tilgangskontroll caches ikke, men gir [TilgangClient.INGEN_TILGANG].
 *
 * @param navIdentSupplier gir NAV-ident for innlogget saksbehandler. Skal kaste exception hvis den mangler.
 */
class CachedTilgangService(
    private val tilgangClient: TilgangClient,
    private val navIdentSupplier: () -> String,
    ttl: Duration = Duration.ofMinutes(5),
    maksStørrelse: Long = 10_000,
    ticker: Ticker = Ticker.systemTicker(),
) {
    private val cache: Cache<Nøkkel, TilgangskontrollResponse> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(ttl)
            .maximumSize(maksStørrelse)
            .ticker(ticker)
            .build()

    fun sjekkTilgangSaksnummer(saksnummer: Saksnummer): TilgangskontrollResponse {
        val nøkkel = nøkkel(Type.SAK, saksnummer.verdi)
        return try {
            cache.get(nøkkel) { tilgangClient.hentTilgangSaksnummer(saksnummer) }
        } catch (e: Exception) {
            tilgangClient.ingenTilgangVedFeil(e, "sak ${saksnummer.verdi}")
        }
    }

    fun sjekkTilgangPerson(personident: Personident): TilgangskontrollResponse {
        val nøkkel = nøkkel(Type.PERSON, personident.verdi)
        return try {
            cache.get(nøkkel) { tilgangClient.hentTilgangPerson(personident) }
        } catch (e: Exception) {
            tilgangClient.ingenTilgangVedFeil(e, "person")
        }
    }

    fun harTilgangSaksnummer(saksnummer: Saksnummer): Boolean = sjekkTilgangSaksnummer(saksnummer).harTilgang

    fun harTilgangPerson(personident: Personident): Boolean = sjekkTilgangPerson(personident).harTilgang

    private fun nøkkel(type: Type, verdi: String): Nøkkel {
        val navIdent = navIdentSupplier()
        require(navIdent.isNotBlank()) { "Mangler NAV-ident for innlogget saksbehandler" }
        return Nøkkel(navIdent, type, verdi)
    }

    private enum class Type { SAK, PERSON }

    private data class Nøkkel(
        val navIdent: String,
        val type: Type,
        val verdi: String,
    ) {
        override fun toString(): String = "Nøkkel(navIdent=$navIdent, type=$type)"
    }
}
