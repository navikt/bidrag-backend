package no.nav.bidrag.arbeidsflyt.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.behandling.behandling.BehandlingDetaljerDtoV2
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

private val LOGGER = KotlinLogging.logger { }

@Service
class BidragBehandlingConsumer(
    @param:Value($$"${BIDRAG_BEHANDLING_URL}") val url: URI,
    @param:Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-behandling") {
    private fun createUri(path: String?) = UriComponentsBuilder
        .fromUri(url)
        .path(path ?: "")
        .build()
        .toUri()

    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 500, maxDelay = 1500, multiplier = 2.0))
    fun hentBehandling(
        behandlingId: Long,
        inkluderSlettet: Boolean = false,
    ): BehandlingDetaljerDtoV2? = try {
        getForEntity<BehandlingDetaljerDtoV2>(
            UriComponentsBuilder
                .fromUri(createUri("/api/v2/behandling/detaljer/$behandlingId"))
                .queryParam("inkluderSlettet", inkluderSlettet)
                .build()
                .toUri(),
        )
    } catch (e: HttpStatusCodeException) {
        if (e.statusCode == HttpStatus.NOT_FOUND) {
            null
        } else {
            LOGGER.warn(e) { "Det skjedde en feil ved henting av behandling $behandlingId" }
            throw e
        }
    }

    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 500, maxDelay = 1500, multiplier = 2.0))
    fun erBehandlingSlettet(behandlingId: Long): Boolean? = try {
        getForEntity<Boolean>(
            createUri("/api/v2/behandling/$behandlingId/slettet"),
        )
    } catch (e: HttpStatusCodeException) {
        if (e.statusCode == HttpStatus.NOT_FOUND) {
            null
        } else {
            LOGGER.warn(e) { "Det skjedde en feil ved sjekk om behandling $behandlingId er slettet" }
            throw e
        }
    }
}
