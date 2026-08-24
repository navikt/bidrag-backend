package no.nav.bidrag.dokument.bestilling.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.dokumentmaler.DokumentBestilling
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

private val log = KotlinLogging.logger {}

@Service
class BidragDokumentProduksjonConsumer(
    @Value($$"${BIDRAG_DOKUMENT_PRODUKSJON_URL}") val url: URI,
    @Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-dokument-produksjon") {
    private fun createUri(path: String?) =
        UriComponentsBuilder
            .fromUri(url)
            .path(path ?: "")
            .build()
            .toUri()

    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 500, maxDelay = 1500, multiplier = 2.0))
    fun opprettPDF(
        malId: String,
        request: DokumentBestilling,
    ): ByteArray =
        try {
            postForNonNullEntity(createUri("/api/v2/dokumentmal/pdf/$malId"), request)
        } catch (e: HttpStatusCodeException) {
            throw e
        }
}
