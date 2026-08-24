package no.nav.bidrag.oppgave.client

import no.nav.bidrag.oppgave.dto.OppgaveDto
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.client.RestClient

class OppgaveApiClient(
    private val restClient: RestClient,
) {
    fun hentOppgaverForSak(saksnummer: String): List<OppgaveDto> = restClient.get()
        .uri { uriBuilder ->
            uriBuilder.path("/api/oppgaver").queryParam("saksnummer", saksnummer).build()
        }
        .retrieve()
        .body(object : ParameterizedTypeReference<List<OppgaveDto>>() {})
        ?: error("Tomt svar fra bidrag-oppgave ved GET /api/oppgaver")

    companion object {
        fun create(baseUrl: String): OppgaveApiClient = OppgaveApiClient(RestClient.builder().baseUrl(baseUrl).build())
    }
}
