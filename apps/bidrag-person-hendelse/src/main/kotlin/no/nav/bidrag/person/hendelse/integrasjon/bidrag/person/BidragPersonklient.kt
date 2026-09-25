package no.nav.bidrag.person.hendelse.integrasjon.bidrag.person

import no.nav.bidrag.commons.util.sanitizeForLog
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.person.HentePersonidenterRequest
import no.nav.bidrag.transport.person.PersonidentDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class BidragPersonklient(
    @Value($$"${egenskaper.integrasjon.bidrag-person.url}") val bidragPersonUrl: URI,
    @Qualifier("azure") val restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "bidrag-person") {
    @Retryable(value = [Exception::class], maxAttempts = 10, backoff = Backoff(delay = 1000, multiplier = 2.0))
    fun henteAlleIdenterForPerson(personIdent: String): List<PersonidentDto>? = try {
        postForEntity(createUri(), HentePersonidenterRequest(personIdent))
    } catch (e: HttpStatusCodeException) {
        secureLogger.warn {
            "Kall mot bidrag-person for å hente alle registrerte personidenter " +
                "for personident $personIdent feilet med statuskode ${e.statusCode} og melding ${e.message}".sanitizeForLog()
        }
        throw e
    }

    private fun createUri() = UriComponentsBuilder
        .fromUri(bidragPersonUrl)
        .path(ENDEPUNKT_PERSONIDENTER)
        .build()
        .toUri()

    companion object {
        const val ENDEPUNKT_PERSONIDENTER = "/personidenter"
    }
}
