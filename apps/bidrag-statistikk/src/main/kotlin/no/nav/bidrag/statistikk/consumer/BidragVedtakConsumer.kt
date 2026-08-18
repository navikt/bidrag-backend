package no.nav.bidrag.statistikk.consumer

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class BidragVedtakConsumer(@Value("\${BIDRAG_VEDTAK_URL}") private val bidragVedtakUrl: URI, @Qualifier("azure") restTemplate: RestTemplate) : AbstractRestClient(restTemplate, "bidrag-vedtak") {
    private val bidragVedtakUri
        get() = UriComponentsBuilder.fromUri(bidragVedtakUrl).pathSegment("vedtak")

    fun hentVedtak(vedtakId: Int): VedtakDto? = getForEntity(
        bidragVedtakUri.pathSegment(vedtakId.toString()).build().toUri(),
    )
}
