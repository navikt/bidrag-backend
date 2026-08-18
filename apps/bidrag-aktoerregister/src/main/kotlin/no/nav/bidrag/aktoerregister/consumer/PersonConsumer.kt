package no.nav.bidrag.aktoerregister.consumer

import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.PersonRequest
import no.nav.bidrag.transport.person.PersondetaljerDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class PersonConsumer(
    @param:Value("\${BIDRAG_PERSON_URL}") val url: URI,
    @param:Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-aktoerregister-aktoerregister") {

    companion object {
        private const val PERSON_PATH = "/informasjon/detaljer"
    }

    fun hentPerson(personIdent: Ident): PersondetaljerDto? {
        try {
            val response: PersondetaljerDto? = postForEntity(
                UriComponentsBuilder.fromUri(url).path(PERSON_PATH).build().toUri(),
                PersonRequest(Personident(personIdent.verdi)),
            )
            secureLogger.debug { "${"Hentet person med id: {} fra bidrag-person: {}"} ${personIdent.verdi} $response" }
            return response
        } catch (e: Exception) {
            secureLogger.error(e) { "Noe gikk galt i kallet mot bidrag-person for ident: ${personIdent.verdi}. Svaret fra bidrag-person var: ${e.message}" }
            throw e
        }
    }
}
