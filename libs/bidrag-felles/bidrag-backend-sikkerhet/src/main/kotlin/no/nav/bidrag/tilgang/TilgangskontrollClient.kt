package no.nav.bidrag.tilgang

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.tilgang.TilgangTilPersonRequest
import no.nav.bidrag.transport.tilgang.TilgangTilSakRequest
import no.nav.bidrag.transport.tilgang.TilgangskontrollResponse
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

/**
 * Kaster exception ved feil mot tilgangskontroll. Se [TilgangskontrollService] for håndtering.
 */
class TilgangskontrollClient(
    private val restClient: RestClient,
) {

    fun sjekkTilgangSaksnummer(saksnummer: Saksnummer): TilgangskontrollResponse = restClient.post()
        .uri("/v2/api/tilgang/sak")
        .body(TilgangTilSakRequest(saksnummer))
        .retrieve()
        .body<TilgangskontrollResponse>()
        ?: error("Tomt svar fra tilgangskontroll")

    fun sjekkTilgangPerson(personident: Personident): TilgangskontrollResponse = restClient.post()
        .uri("/v2/api/tilgang/person")
        .body(TilgangTilPersonRequest(personident))
        .retrieve()
        .body<TilgangskontrollResponse>()
        ?: error("Tomt svar fra tilgangskontroll")
}
