package no.nav.bidrag.henvendelse.consumer

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.henvendelse.aop.PersonIkkeFunnetException
import no.nav.bidrag.transport.person.HentePersonidenterRequest
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.PersonidentDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
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

        // bidrag-person svarer 404 når identen ikke finnes i folkeregisteret. Det er ikke en feil
        // i tjenesten, så den oversettes her framfor å ende som 502 "Feil ved kall mot tjeneste".
        // Bare denne konsumenten gjør det: en 404 fra sf-henvendelse-api-proxy betyr at basestien
        // vår er feil, og skal fortsatt bli 502.
        val identer: List<PersonidentDto> = try {
            postForNonNullEntity(
                uri,
                HentePersonidenterRequest(
                    ident = personident.verdi,
                    grupper = setOf(Identgruppe.AKTORID),
                    inkludereHistoriske = false,
                ),
            )
        } catch (_: HttpClientErrorException.NotFound) {
            throw PersonIkkeFunnetException()
        }
        return identer.firstOrNull { it.gruppe == Identgruppe.AKTORID && !it.historisk }?.ident
    }
}
