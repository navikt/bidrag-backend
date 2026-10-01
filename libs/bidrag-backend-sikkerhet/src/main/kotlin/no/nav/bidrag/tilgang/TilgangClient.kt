package no.nav.bidrag.tilgang

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.tilgang.*
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

class TilgangClient(
    private val restClient: RestClient,
) {

    fun harTilgangSaksnummer(saksnummer: Saksnummer): Boolean = restClient.post()
        .uri("/v2/api/tilgang/sak")
        .body(TilgangTilSakRequest(saksnummer))
        .retrieve()
        .body<TilgangskontrollResponse>()!!
        .harTilgang

    fun harTilgangPerson(personident: Personident): Boolean = restClient.post()
        .uri("/v2/api/tilgang/person")
        .body(TilgangTilPersonRequest(personident))
        .retrieve()
        .body<TilgangskontrollResponse>()!!
        .harTilgang
}
