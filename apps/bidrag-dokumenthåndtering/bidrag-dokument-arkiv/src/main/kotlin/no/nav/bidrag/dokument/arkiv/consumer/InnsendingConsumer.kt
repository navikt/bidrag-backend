package no.nav.bidrag.dokument.arkiv.consumer

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.dokument.arkiv.consumer.dto.DokumentSoknadDto
import no.nav.bidrag.dokument.arkiv.consumer.dto.EksternEttersendingsOppgave
import no.nav.bidrag.dokument.arkiv.consumer.dto.HentEtterseningsoppgaveRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class InnsendingConsumer(
    @Value($$"${INNSENDING_API_URL}") val url: URI,
    @Qualifier("azureService") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "innsending-api") {
    private fun createUri(path: String? = null) = UriComponentsBuilder
        .fromUri(url)
        .path("ekstern/v1/oppgaver")
        .path(path ?: "")
        .build()
        .toUri()

    fun opprettEttersendingsoppgave(oppgave: EksternEttersendingsOppgave): DokumentSoknadDto = operations.exchange<DokumentSoknadDto>(
        createUri(),
        HttpMethod.POST,
        HttpEntity(oppgave),
    ).body!!

    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 500, maxDelay = 1500, multiplier = 2.0))
    fun hentEttersendingsoppgave(oppgave: HentEtterseningsoppgaveRequest): List<DokumentSoknadDto> = operations.exchange<List<DokumentSoknadDto>>(
        createUri(),
        HttpMethod.GET,
        HttpEntity(oppgave),
    ).body!!
}
