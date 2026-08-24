package no.nav.bidrag.dokument.bestilling.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.cache.BrukerCacheable
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.dokument.bestilling.config.CacheConfig
import no.nav.bidrag.dokument.bestilling.model.HentVedtakFeiletException
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

private val log = KotlinLogging.logger {}

@Service
class BidragVedtakConsumer(
    @Value($$"${BIDRAG_VEDTAK_URL}") val url: URI,
    @Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-vedtak") {
    private fun createUri(path: String?) = UriComponentsBuilder
        .fromUri(url)
        .path(path ?: "")
        .build()
        .toUri()

    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 500, maxDelay = 1500, multiplier = 2.0))
    @BrukerCacheable(CacheConfig.VEDTAK_CACHE)
    fun hentVedtak(vedtakId: Int): VedtakDto? {
        try {
            return getForEntity(createUri("/vedtak/$vedtakId"))
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                return null
            }
            throw HentVedtakFeiletException("Henting av vedtak $vedtakId feilet", e)
        }
    }
}
