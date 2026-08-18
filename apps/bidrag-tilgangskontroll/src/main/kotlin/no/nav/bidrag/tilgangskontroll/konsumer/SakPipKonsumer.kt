package no.nav.bidrag.tilgangskontroll.konsumer

import no.nav.bidrag.commons.cache.BrukerCacheable
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.tilgangskontroll.konfigurasjon.Cache
import no.nav.bidrag.tilgangskontroll.model.sakIkkeFunnet
import no.nav.bidrag.transport.sak.BidragssakPipDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class SakPipKonsumer(
    @param:Value($$"${BIDRAG_SAK_URL}") private val url: URI,
    @param:Qualifier("azureService") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-sak") {
    private fun sakPipUrl(saksnr: String) = UriComponentsBuilder
        .fromUri(url)
        .pathSegment("v2/pip/sak", saksnr)
        .build()
        .toUri()

    @BrukerCacheable(Cache.PIP_SAK)
    fun hentPipMetadata(saksnr: String): BidragssakPipDto = getForEntity(sakPipUrl(saksnr))
        ?: sakIkkeFunnet("Fant ingen sak med saksnr $saksnr")
}
