package no.nav.bidrag.person.consumer

import no.nav.bidrag.commons.cache.BrukerCacheable
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class SkjermingConsumer(restTemplateBuilder: RestTemplateBuilder, @param:Value("\${SKJERMING_URL}") private val skjermingUrl: URI) :
    AbstractRestClient(
        restTemplateBuilder.build(),
        "skjerming",
    ) {
    private val skjermingRequestUrl: URI =
        UriComponentsBuilder.fromUri(skjermingUrl).pathSegment(PATH_SKJERMING).build().toUri()

    @BrukerCacheable(cacheNames = ["skjermingBulk"], cacheManager = "cacheManagerPdl")
    fun erPersonerSkjermet(identer: Set<Personident>): Map<Personident, Boolean> = postForNonNullEntity(skjermingRequestUrl, mapOf("personidenter" to identer.toSet()))

    companion object {
        private const val PATH_SKJERMING = "skjermetBulk"
    }
}
