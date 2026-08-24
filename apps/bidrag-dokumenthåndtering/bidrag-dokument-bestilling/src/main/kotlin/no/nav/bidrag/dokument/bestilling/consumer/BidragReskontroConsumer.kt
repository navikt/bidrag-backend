package no.nav.bidrag.dokument.bestilling.consumer

import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.reskontro.request.SaksnummerRequest
import no.nav.bidrag.transport.reskontro.response.transaksjoner.TransaksjonerDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class BidragReskontroConsumer(
    @Value($$"${BIDRAG_RESKONTRO_URL}") val url: URI,
    @Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-reskontro") {
    private fun createUri(path: String?) = UriComponentsBuilder
        .fromUri(url)
        .path(path ?: "")
        .build()
        .toUri()

    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 500, maxDelay = 1500, multiplier = 2.0))
    fun transaksjoner(saksnr: String): TransaksjonerDto? {
        try {
            return postForNonNullEntity(createUri("/transaksjoner/bidragssak"), SaksnummerRequest(Saksnummer(saksnr)))
        } catch (e: HttpStatusCodeException) {
            secureLogger.warn(e) { "Det skjedde en feil ved henting av transaksjoner fra bidrag-reskontro for sak $saksnr" }
            return null
        }
    }
}
