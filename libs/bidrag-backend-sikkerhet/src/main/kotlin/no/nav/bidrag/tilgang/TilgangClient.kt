package no.nav.bidrag.tilgang

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.tilgang.OpprinnelseTilgangsbeslutning
import no.nav.bidrag.transport.tilgang.TilgangTilPersonRequest
import no.nav.bidrag.transport.tilgang.TilgangTilSakRequest
import no.nav.bidrag.transport.tilgang.TilgangskontrollResponse
import no.nav.bidrag.transport.tilgang.TilgangskontrollResponseDetaljer
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

/**
 * Feil mot tilgangskontroll logges og gir [INGEN_TILGANG] (deny by default).
 */
class TilgangClient(
    private val restClient: RestClient,
) {

    fun sjekkTilgangSaksnummer(saksnummer: Saksnummer): TilgangskontrollResponse = try {
        hentTilgangSaksnummer(saksnummer)
    } catch (e: Exception) {
        ingenTilgangVedFeil(e, "sak ${saksnummer.verdi}")
    }

    fun sjekkTilgangPerson(personident: Personident): TilgangskontrollResponse = try {
        hentTilgangPerson(personident)
    } catch (e: Exception) {
        ingenTilgangVedFeil(e, "person")
    }

    /** Kaster ved feil. Brukes av [CachedTilgangService] så feil ikke caches som avslag. */
    internal fun hentTilgangSaksnummer(saksnummer: Saksnummer): TilgangskontrollResponse = restClient.post()
        .uri("/v2/api/tilgang/sak")
        .body(TilgangTilSakRequest(saksnummer))
        .retrieve()
        .body<TilgangskontrollResponse>()
        ?: error("Tomt svar fra tilgangskontroll")

    /** Kaster ved feil. Brukes av [CachedTilgangService] så feil ikke caches som avslag. */
    internal fun hentTilgangPerson(personident: Personident): TilgangskontrollResponse = restClient.post()
        .uri("/v2/api/tilgang/person")
        .body(TilgangTilPersonRequest(personident))
        .retrieve()
        .body<TilgangskontrollResponse>()
        ?: error("Tomt svar fra tilgangskontroll")

    internal fun ingenTilgangVedFeil(e: Exception, gjelder: String): TilgangskontrollResponse {
        log.error("Feil ved sjekk av tilgang til $gjelder. Gir ingen tilgang.", e)
        return INGEN_TILGANG
    }

    companion object {
        private val log = LoggerFactory.getLogger(TilgangClient::class.java)

        val INGEN_TILGANG = TilgangskontrollResponse(
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
}
