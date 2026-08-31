package no.nav.bidrag.sak.integration

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.enums.behandling.SøknadsknytningStatus
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.søknad.FinnSammenknytningerHovedsøknadRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

data class FinnSammenknytningerHovedsøknadResponse(
    val hovedsøknadsid: Long? = null,
    val søknader: List<HentSøknad>,
)

@Component
class BidragBBMConsumer(
    @Value($$"${BIDRAG_BBM_URL}") private val bidragBBMurl: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "bidrag-bbm") {
    private val bidragBBMUri
        get() = UriComponentsBuilder.fromUri(bidragBBMurl).pathSegment("api", "beregning")

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    fun finnSammenknytningerHovedsøknad(
        søknadsid: Long,
    ) = postForNonNullEntity<FinnSammenknytningerHovedsøknadResponse>(
        bidragBBMUri.pathSegment("finnsammenknytningerhovedsoknad").build().toUri(),
        FinnSammenknytningerHovedsøknadRequest(søknadsid, statuser = listOf(SøknadsknytningStatus.Aktiv, SøknadsknytningStatus.Deaktiv)),
    )
}
