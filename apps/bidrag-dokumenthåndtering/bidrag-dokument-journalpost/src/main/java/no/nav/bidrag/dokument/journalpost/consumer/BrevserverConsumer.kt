package no.nav.bidrag.dokument.journalpost.consumer

import io.micrometer.core.annotation.Timed
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.dokument.journalpost.exception.HentingAvDokumentFeiletException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class BrevserverConsumer(
    @Value($$"${BREVSERVER_NAIS_URL}") private val url: URI,
    @Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "brevserver-nais") {
    private fun createUri(path: String) = UriComponentsBuilder
        .fromUri(url)
        .path(path)
        .build()
        .toUri()

    @Retryable(value = [Exception::class], maxAttempts = 3, backoff = Backoff(delay = 500, maxDelay = 1000, multiplier = 2.0))
    @Timed("hentDokumentBrevserverNais")
    fun hentDokument(brevreferanse: String): ByteArray = getForEntity<ByteArray>(createUri("/brevweb/rest/hentdokument/bisys/$brevreferanse"))
        ?: throw HentingAvDokumentFeiletException(
            "Henting av dokument for brevreferanse $brevreferanse fra brevserver returnerte tomt svar",
        )
}
