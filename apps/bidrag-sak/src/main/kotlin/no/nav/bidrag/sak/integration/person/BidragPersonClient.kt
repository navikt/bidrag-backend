package no.nav.bidrag.sak.integration.person

import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.util.IdentConsumer
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.sak.util.takeIfNotNullOrEmpty
import no.nav.bidrag.transport.person.Fødselsdatoer
import no.nav.bidrag.transport.person.HentePersonidenterRequest
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.person.PersonidentDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Service
class BidragPersonClient(
    @Value($$"${BIDRAG_PERSON_URL}") bidragPersonBaseUrl: URI,
    @Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidragPerson") {
    private val bidragPersonUri =
        UriComponentsBuilder
            .fromUri(bidragPersonBaseUrl)
            .pathSegment("fodselsdatoer")
            .build()
            .toUri()

    @Retryable(value = [Exception::class], backoff = Backoff(delay = 500))
    fun hentFødselsdatoer(personIdent: List<Personident>): Map<Personident, LocalDate?> {
        val fødselsdatoer: Fødselsdatoer = postForNonNullEntity(bidragPersonUri, personIdent)

        return fødselsdatoer.identerTilDatoer
    }
}

fun hentPerson(ident: String?): PersonDto? = try {
    ident.takeIfNotNullOrEmpty {
        AppContext.getBean(IdentConsumer::class.java).hentPersonInformasjon(Personident(it))
    }
} catch (e: Exception) {
    secureLogger.debug(e) { "Feil ved henting av person for ident $ident" }
    null
}

fun hentNyesteIdent(ident: String?) = ident?.let { hentPerson(ident)?.ident ?: Personident(ident) }
