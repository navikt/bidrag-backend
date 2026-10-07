package no.nav.bidrag.commons.util

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.HentePersonidenterRequest
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.person.PersonidentDto
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestOperations
import java.net.URI

private val LOGGER = KotlinLogging.logger {}

/**
 * Oppslag mot bidrag-person uten cache.
 * Returnerer null når bidrag-person ikke finner personen (204 eller 404). Andre feil kastes videre.
 */
class BidragPersonOppslagClient(
    personUrl: String,
    restOperations: RestOperations,
) : AbstractRestClient(restOperations, "bidrag-person") {
    private val personidenterUri = URI.create("$personUrl${IdentConsumer.PERSON_PATH}")
    private val informasjonUri = URI.create("$personUrl${IdentConsumer.INFORMASJON_PATH}")

    fun hentPersonidenter(
        ident: String,
        grupper: Set<Identgruppe>,
        inkludereHistoriske: Boolean,
    ): List<PersonidentDto>? = nullHvisIkkeFunnet {
        postForEntity<List<PersonidentDto>>(personidenterUri, HentePersonidenterRequest(ident, grupper, inkludereHistoriske))
            ?.ifEmpty { null }
    }

    fun hentPersonInformasjon(ident: Personident): PersonDto? = nullHvisIkkeFunnet {
        postForEntity<PersonDto>(informasjonUri, PersonDto(ident))
    }

    private fun <T> nullHvisIkkeFunnet(oppslag: () -> T?): T? = try {
        oppslag()
    } catch (e: RestClientResponseException) {
        if (e.statusCode.value() != HttpStatus.NOT_FOUND.value()) throw e
        LOGGER.info {
            "Bidrag-person fant ingen person på kalt ident. CallId: ${CorrelationId.fetchCorrelationIdForThread().sanitizeForLog()}."
        }
        null
    }
}
