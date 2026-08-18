package no.nav.bidrag.oppgave.client

import no.nav.bidrag.oppgave.dto.OppgaveDto
import org.springframework.web.client.RestClient

class OppgaveApiClient(
    private val restClient: RestClient,
) {
    fun hentHelloOppgave(): OppgaveDto = restClient.get()
        .uri("/api/hello")
        .retrieve()
        .body(OppgaveDto::class.java)
        ?: error("Tomt svar fra bidrag-oppgave ved GET /api/hello")

    companion object {
        fun create(baseUrl: String): OppgaveApiClient = OppgaveApiClient(RestClient.builder().baseUrl(baseUrl).build())
    }
}
