package no.nav.bidrag.beregn.debug.app.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningBeløpshistorikkConsumer
import no.nav.bidrag.beregn.debug.app.config.CacheConfig
import no.nav.bidrag.commons.cache.BrukerCacheable
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadHistoriskRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.LøpendeBidragPeriodeRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.LøpendeBidragssakerRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidragPeriodeResponse
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidragssakerResponse
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

private val LOGGER = KotlinLogging.logger { }

@Component
class BidragBeløpshistorikkConsumer(
    @Value("\${BIDRAG_BELOPSHISTORIKK_URL}") private val bidragBeløpshistorikkUrl: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "bidrag-beløpshistorikk"),
    BeregningBeløpshistorikkConsumer {
    private val bidragBeløpshistorikkUri
        get() = UriComponentsBuilder.fromUri(bidragBeløpshistorikkUrl)

    @BrukerCacheable(CacheConfig.STØNAD_LØPENDE_BIDRAG_CACHE)
    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    override fun hentLøpendeBidrag(løpendeBidragssakerRequest: LøpendeBidragssakerRequest): LøpendeBidragssakerResponse = postForNonNullEntity(
        bidragBeløpshistorikkUri.pathSegment("hent-lopende-bidragssaker-for-skyldner").build().toUri(),
        løpendeBidragssakerRequest,
    )

    override fun hentAlleLøpendeStønaderIPeriode(løpendeBidragPeriodeRequest: LøpendeBidragPeriodeRequest): LøpendeBidragPeriodeResponse = postForNonNullEntity(
        bidragBeløpshistorikkUri.pathSegment("hent-stonader-i-periode/").build().toUri(),
        løpendeBidragPeriodeRequest,
    )

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    @BrukerCacheable(CacheConfig.STØNAD_HISTORIKK_CACHE)
    override fun hentHistoriskeStønader(hentStønadHistoriskRequest: HentStønadHistoriskRequest): StønadDto? = try {
        postForEntity(
            bidragBeløpshistorikkUri.pathSegment("hent-stonad-historisk/").build().toUri(),
            hentStønadHistoriskRequest,
        )
    } catch (e: HttpStatusCodeException) {
        if (e.statusCode != HttpStatus.NOT_FOUND) throw e
        LOGGER.info { "Fant ikke historisk stønad for sak ${hentStønadHistoriskRequest.sak.verdi} i beløpshistorikk." }
        null
    }

    @BrukerCacheable(CacheConfig.Companion.STØNAD_HISTORIKK_CACHE_2)
    override fun hentLøpendeStønad(hentStønadRequest: HentStønadRequest): StønadDto? = try {
        postForEntity(
            bidragBeløpshistorikkUri.pathSegment("hent-stonad-historisk/").build().toUri(),
            hentStønadRequest,
        )
    } catch (e: HttpStatusCodeException) {
        if (e.statusCode != HttpStatus.NOT_FOUND) throw e
        LOGGER.info { "Fant ikke stønad for sak ${hentStønadRequest.sak.verdi} i beløpshistorikk." }
        null
    }
}
