package no.nav.bidrag.tilgang

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Ticker
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.tilgang.OpprinnelseTilgangsbeslutning
import no.nav.bidrag.transport.tilgang.TilgangskontrollResponse
import no.nav.bidrag.transport.tilgang.TilgangskontrollResponseDetaljer
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Sjekker tilgang til sak eller person via tilgangskontroll. Resultat fra tilgangskontroll caches i minnet for å redusere antall kall.
 * Ved feil mot tilgangskontroll logges og gir ingen tilgang.
 *
 * @param navIdentSupplier gir NAV-ident for innlogget saksbehandler.
 */
class TilgangskontrollService(
    private val tilgangskontrollClient: TilgangskontrollClient,
    private val navIdentSupplier: () -> String,
    ttl: Duration = Duration.ofMinutes(5),
    maksStørrelse: Long = 10_000,
    ticker: Ticker = Ticker.systemTicker(),
) {
    private val log = LoggerFactory.getLogger(TilgangskontrollService::class.java)

    private val cache: Cache<CacheKey, TilgangskontrollResponse> =
        Caffeine
            .newBuilder()
            .expireAfterWrite(ttl)
            .maximumSize(maksStørrelse)
            .ticker(ticker)
            .build()

    fun hentTilgangSaksnummer(saksnummer: Saksnummer): TilgangskontrollResponse {
        val nøkkel = cacheKey(Type.SAK, saksnummer.verdi)
        return try {
            cache.get(nøkkel) { tilgangskontrollClient.sjekkTilgangSaksnummer(saksnummer) }
        } catch (e: Exception) {
            ingenTilgangVedFeil(e, "sak ${saksnummer.verdi}")
        }
    }

    fun hentTilgangPerson(personident: Personident): TilgangskontrollResponse {
        val nøkkel = cacheKey(Type.PERSON, personident.verdi)
        return try {
            cache.get(nøkkel) { tilgangskontrollClient.sjekkTilgangPerson(personident) }
        } catch (e: Exception) {
            ingenTilgangVedFeil(e, "person")
        }
    }

    /**
     * Sjekker om saksbehandler har tilgang til saken.
     * @throws TilgangskontrollException
     */
    fun sjekkTilgangSaksnummer(saksnummer: Saksnummer) {
        val response = hentTilgangSaksnummer(saksnummer)
        if (!response.harTilgang) throw TilgangskontrollException(response)
    }

    /**
     * Sjekker om saksbehandler har tilgang til personen.
     * @throws TilgangskontrollException
     */
    fun sjekkTilgangPerson(personident: Personident) {
        val response = hentTilgangPerson(personident)
        if (!response.harTilgang) throw TilgangskontrollException(response)
    }

    private fun ingenTilgangVedFeil(e: Exception, gjelder: String): TilgangskontrollResponse {
        log.error("Feil ved sjekk av tilgang til $gjelder. Gir ingen tilgang.", e)
        return TilgangskontrollResponse(
            harTilgang = false,
            detaljer = listOf(
                TilgangskontrollResponseDetaljer(
                    harTilgang = false,
                    begrunnelse = "Feil ved kall mot tilgangskontroll",
                    opprinnelseTilgangsbeslutning = OpprinnelseTilgangsbeslutning.BIDRAG_TILGANGSKONTROLL,
                ),
            ),
        )
    }

    private fun cacheKey(type: Type, verdi: String): CacheKey {
        val navIdent = navIdentSupplier()
        require(navIdent.isNotBlank()) { "Mangler NAV-ident for innlogget saksbehandler" }
        return CacheKey(navIdent, type, verdi)
    }

    private enum class Type { SAK, PERSON }

    private data class CacheKey(
        val navIdent: String,
        val type: Type,
        val verdi: String,
    ) {
        override fun toString(): String = "CacheKey(navIdent=$navIdent, type=$type)"
    }
}
