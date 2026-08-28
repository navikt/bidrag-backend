package no.nav.bidrag.beregn.debug.app.consumer

import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningVedtakConsumer
import no.nav.bidrag.beregn.debug.app.config.CacheConfig.Companion.MANUELLE_VEDTAK_FOR_BP
import no.nav.bidrag.beregn.debug.app.config.CacheConfig.Companion.VEDTAK_CACHE
import no.nav.bidrag.beregn.debug.app.config.CacheConfig.Companion.VEDTAK_FOR_STØNAD_CACHE
import no.nav.bidrag.commons.cache.BrukerCacheable
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.behandling.vedtak.request.HentManuelleVedtakRequest
import no.nav.bidrag.transport.behandling.vedtak.request.HentVedtakForStønadRequest
import no.nav.bidrag.transport.behandling.vedtak.response.HentVedtakForStønadResponse
import no.nav.bidrag.transport.behandling.vedtak.response.VedtakDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class BidragVedtakConsumer(@Value("\${BIDRAG_VEDTAK_URL}") private val bidragVedtakUrl: URI, @Qualifier("azure") restTemplate: RestTemplate) :
    AbstractRestClient(restTemplate, "bidrag-vedtak"),
    BeregningVedtakConsumer {
    private val bidragVedtakUri
        get() = UriComponentsBuilder.fromUri(bidragVedtakUrl).pathSegment("vedtak")

    @BrukerCacheable(VEDTAK_CACHE)
    override fun hentVedtak(vedtaksid: Int): VedtakDto? = getForEntity(
        bidragVedtakUri.pathSegment(vedtaksid.toString()).build().toUri(),
    )

    @BrukerCacheable(VEDTAK_FOR_STØNAD_CACHE)
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    override fun hentVedtakForStønad(request: HentVedtakForStønadRequest): HentVedtakForStønadResponse = postForNonNullEntity(
        bidragVedtakUri.pathSegment("hent-vedtak").build().toUri(),
        request,
    )

    @BrukerCacheable(MANUELLE_VEDTAK_FOR_BP)
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    override fun hentManuelleVedtak(request: HentManuelleVedtakRequest): HentVedtakForStønadResponse = postForNonNullEntity(
        bidragVedtakUri.pathSegment("hent-manuelle-vedtak").build().toUri(),
        request,
    )
}
