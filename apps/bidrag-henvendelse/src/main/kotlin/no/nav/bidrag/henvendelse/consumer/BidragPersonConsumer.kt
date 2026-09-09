package no.nav.bidrag.henvendelse.consumer

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.HentePersonidenterRequest
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.PersonidentDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * Henvendelsesløsningen slår opp på aktørid, ikke fødselsnummer, så identen må veksles først.
 * bidrag-person eksponerer dette via `/personidenter`.
 *
 * ### Forespørsel
 * ```
 * POST /personidenter
 * ```
 * ```json
 * {
 *   "ident": "17490123474",
 *   "grupper": ["AKTORID"],
 *   "inkludereHistoriske": false
 * }
 * ```
 *
 * ### Svar (200)
 * ```json
 * [
 *   { "ident": "2000012345678", "historisk": false, "gruppe": "AKTORID" }
 * ]
 * ```
 * Tom liste betyr at personen ikke har noen aktørid.
 */
@Service
class BidragPersonConsumer(
    @param:Value($$"${BIDRAG_PERSON_URL}") private val bidragPersonUrl: URI,
    @param:Qualifier("azure") restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidrag-person") {
    /** Returnerer gjeldende aktørid, eller `null` om personen ikke har noen. */
    fun hentAktørid(personident: Personident): String? {
        val uri = UriComponentsBuilder
            .fromUri(bidragPersonUrl)
            .pathSegment("personidenter")
            .build()
            .toUri()

        val identer: List<PersonidentDto> = postForNonNullEntity(
            uri,
            HentePersonidenterRequest(
                ident = personident.verdi,
                grupper = setOf(Identgruppe.AKTORID),
                inkludereHistoriske = false,
            ),
        )
        return identer.firstOrNull { it.gruppe == Identgruppe.AKTORID && !it.historisk }?.ident
    }
}
