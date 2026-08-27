package no.nav.bidrag.beregn.debug.app.consumer

import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningBBMConsumer
import no.nav.bidrag.beregn.debug.app.config.CacheConfig.Companion.BBM_ALLE_BEREGNINGER_CACHE
import no.nav.bidrag.beregn.debug.app.config.CacheConfig.Companion.BBM_BEREGNING_CACHE
import no.nav.bidrag.commons.cache.BrukerCacheable
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningRequestDto
import no.nav.bidrag.transport.behandling.beregning.felles.BidragBeregningResponsDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class BidragBBMConsumer(@Value("\${BIDRAG_BBM_URL}") private val bidragBBMurl: URI, @Qualifier("azure") restTemplate: RestTemplate) :
    AbstractRestClient(restTemplate, "bidrag-bbm"),
    BeregningBBMConsumer {
    private val bidragBBMUri
        get() = UriComponentsBuilder.fromUri(bidragBBMurl).pathSegment("api", "beregning")

    @BrukerCacheable(BBM_BEREGNING_CACHE)
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    override fun hentBeregning(request: BidragBeregningRequestDto): BidragBeregningResponsDto = postForNonNullEntity(
        bidragBBMUri.build().toUri(),
        request,
    )

    @BrukerCacheable(BBM_ALLE_BEREGNINGER_CACHE)
    override fun hentAlleBeregninger(request: BidragBeregningRequestDto): BidragBeregningResponsDto = postForNonNullEntity(
        bidragBBMUri.pathSegment("alleberegningerogsamvar").build().toUri(),
        request,
    )
}
