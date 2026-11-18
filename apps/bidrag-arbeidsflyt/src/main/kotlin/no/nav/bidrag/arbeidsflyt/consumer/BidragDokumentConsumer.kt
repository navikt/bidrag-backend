package no.nav.bidrag.arbeidsflyt.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.arbeidsflyt.model.HentJournalpostFeiletFunksjoneltException
import no.nav.bidrag.arbeidsflyt.model.HentJournalpostFeiletTekniskException
import no.nav.bidrag.commons.util.sanitizeForLog
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.dokument.JournalpostResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class BidragDokumentConsumer(
    @Value($$"${BIDRAG_DOKUMENT_URL}") val url: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
    @Value($$"${retry.enabled:true}") val shouldRetry: Boolean,
) : AbstractRestClient(restTemplate, "bidrag-dokument") {
    companion object {
        @JvmStatic
        private val LOGGER = KotlinLogging.logger { }
    }

    private val baseUri
        get() =
            UriComponentsBuilder
                .fromUri(url)
                .pathSegment("bidrag-dokument")

    @Retryable(
        exceptionExpression = "@bidragDokumentConsumer.shouldRetry",
        value = [HentJournalpostFeiletTekniskException::class],
        maxAttempts = 10,
        backoff = Backoff(delay = 2000, maxDelay = 30000, multiplier = 2.0),
    )
    fun hentJournalpost(journalpostId: String): JournalpostResponse? {
        return try {
            getForEntity<JournalpostResponse>(
                baseUri
                    .pathSegment("journal")
                    .pathSegment(journalpostId)
                    .build()
                    .toUri(),
            )
        } catch (e: HttpStatusCodeException) {
            if (HttpStatus.NOT_FOUND == e.statusCode) {
                // Should not happen in production. Logging error to be notified
                LOGGER.error(e) { "Fant ikke journalpost ${journalpostId.sanitizeForLog()}" }
                return null
            }

            val errorMessage = "Det skjedde en feil ved henting av journalpost $journalpostId"
            if (e.statusCode.is4xxClientError) {
                LOGGER.error(e) { errorMessage.sanitizeForLog() }
                throw HentJournalpostFeiletFunksjoneltException(errorMessage, e)
            }
            throw HentJournalpostFeiletTekniskException(errorMessage, e)
        }
    }
}
