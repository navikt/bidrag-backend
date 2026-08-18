package no.nav.bidrag.sak.integration.samhandler

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.SamhandlerId
import no.nav.bidrag.transport.samhandler.SamhandlerDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class BidragSamhandlerClient(
    @Value($$"${BIDRAG_SAMHANDLER_URL}") bidragSamhandlerBaseUrl: URI,
    @Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidragSamhandler") {
    private val bidragSamhandlerUri =
        UriComponentsBuilder
            .fromUri(bidragSamhandlerBaseUrl)
            .pathSegment("samhandler")
            .build()
            .toUri()

    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    fun hentSamhandler(samhandlerIdent: SamhandlerId): SamhandlerDto = postForNonNullEntity(bidragSamhandlerUri, samhandlerIdent)
}
