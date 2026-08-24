package no.nav.bidrag.dokument.bestilling.consumer

import no.nav.bidrag.commons.cache.BrukerCacheable
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.dokument.bestilling.config.CacheConfig.Companion.PERSON_ADRESSE_CACHE
import no.nav.bidrag.dokument.bestilling.config.CacheConfig.Companion.PERSON_CACHE
import no.nav.bidrag.dokument.bestilling.config.CacheConfig.Companion.PERSON_SPRAAK_CACHE
import no.nav.bidrag.dokument.bestilling.model.HentPersonFeiletException
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.PersonAdresseDto
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.person.PersonRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class BidragPersonConsumer(
    @Value($$"${BIDRAG_PERSON_URL}") val url: URI,
    @Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-person") {
    private fun createUri(path: String?) = UriComponentsBuilder
        .fromUri(url)
        .path(path ?: "")
        .build()
        .toUri()

    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 500, maxDelay = 1500, multiplier = 2.0))
    @BrukerCacheable(PERSON_CACHE)
    fun hentPerson(personId: String): PersonDto? {
        try {
            return postForEntity(createUri("/informasjon"), PersonRequest(Personident(personId)))
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                return null
            }
            throw HentPersonFeiletException("Henting av person $personId feilet", e)
        }
    }

    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 500, maxDelay = 1500, multiplier = 2.0))
    @BrukerCacheable(PERSON_ADRESSE_CACHE)
    fun hentAdresse(id: String): PersonAdresseDto? = try {
        postForEntity(createUri("/adresse/post"), PersonRequest(Personident(id)))
    } catch (e: HttpStatusCodeException) {
        if (e.statusCode == HttpStatus.NOT_FOUND) {
            null
        } else {
            throw HentPersonFeiletException("Henting av språk for person $id feilet", e)
        }
    }

    @Retryable(maxAttempts = 3, backoff = Backoff(delay = 500, maxDelay = 1500, multiplier = 2.0))
    @BrukerCacheable(PERSON_SPRAAK_CACHE)
    fun hentSpraak(id: String): String? {
        val headers = HttpHeaders()
        headers.accept = listOf(MediaType.TEXT_PLAIN)
        try {
            return postForEntity(createUri("/spraak"), PersonRequest(Personident(id)), headers)
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                return null
            }
            throw HentPersonFeiletException("Henting av språk for person $id feilet", e)
        }
    }
}
