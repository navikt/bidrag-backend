package no.nav.bidrag.dokument.bestilling.consumer

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest
import no.nav.bidrag.transport.dokument.OpprettJournalpostResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class BidragDokumentConsumer(
    @Value($$"${BIDRAG_DOKUMENT_URL}") val url: URI,
    @Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-dokument") {
    private fun createUri(path: String?) = UriComponentsBuilder
        .fromUri(url)
        .path(path ?: "")
        .build()
        .toUri()

    fun opprettJournalpost(opprettJournalpostRequest: OpprettJournalpostRequest): OpprettJournalpostResponse? = postForEntity(createUri("/journalpost/BIDRAG"), opprettJournalpostRequest)

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 300, maxDelay = 2000, multiplier = 2.0),
    )
    fun hentDokument(
        journalpostId: String?,
        dokumentId: String?,
    ): ByteArray? {
        if (journalpostId.isNullOrEmpty()) return hentDokument(dokumentId)
        return getForEntity(
            UriComponentsBuilder
                .fromUri(url)
                .path("/dokument/$journalpostId${dokumentId?.let { "/$it" } ?: ""}")
                .queryParam("optimizeForPrint", "false")
                .build()
                .toUri(),
        )
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    fun hentDokument(dokumentId: String?): ByteArray? = getForEntity(
        UriComponentsBuilder
            .fromUri(url)
            .path("/dokumentreferanse/$dokumentId")
            .queryParam("optimizeForPrint", "false")
            .build()
            .toUri(),
    )
}
