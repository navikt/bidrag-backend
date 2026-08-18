package no.nav.bidrag.organisasjon.consumer

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.organisasjon.CacheConfig
import no.nav.bidrag.organisasjon.exception.PersonConsumerException
import no.nav.bidrag.organisasjon.exception.ikkeFunnet
import no.nav.bidrag.transport.person.GeografiskTilknytningDto
import no.nav.bidrag.transport.person.Graderingsinfo
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class PersonConsumer(
    @Value($$"${PERSON_URL}")
    private val personBaseUri: URI,
    @Qualifier("azureService")
    private val restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "person") {
    private val personGeografiskTilknytnigUri: URI =
        UriComponentsBuilder.fromUri(personBaseUri).pathSegment(PATH_PERSON_GEOGRAFISK).build().toUri()

    private val graderingsinfoUri: URI =
        UriComponentsBuilder.fromUri(personBaseUri).pathSegment(PATH_PERSON_GRADERINGSINFO).build().toUri()

    @Cacheable(CacheConfig.PERSON_GEOGRAFISK_CACHE)
    fun hentPersonGeografiskTilknytning(ident: Personident): GeografiskTilknytningDto = postForEntity(personGeografiskTilknytnigUri, ident)
        ?: throw PersonConsumerException("Fant ikke geografisk enhet for person")

    @Cacheable(CacheConfig.PERSON_GRADERING_CACHE)
    fun hentGraderingsinfo(identer: Collection<Personident>): Graderingsinfo = postForEntity(graderingsinfoUri, identer) ?: ikkeFunnet("Fant ikke graderingsinfo for person")

    companion object {
        private const val PATH_PERSON_GEOGRAFISK = "geografisk_tilknytning"
        private const val PATH_PERSON_GRADERINGSINFO = "graderingsinfo"
    }
}
