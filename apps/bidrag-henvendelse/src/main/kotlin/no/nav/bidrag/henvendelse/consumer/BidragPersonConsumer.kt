package no.nav.bidrag.henvendelse.consumer

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.henvendelse.aop.PersonIkkeFunnetException
import no.nav.bidrag.henvendelse.aop.TjenesteFeilException
import no.nav.bidrag.transport.person.HentePersonidenterRequest
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.PersonidentDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
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
        //
        // Fanges på statuskode og ikke på HttpClientErrorException.NotFound: AbstractRestClient
        // kaster ikke nødvendigvis den underklassen. Får den en ikke-2xx respons uten at
        // RestTemplate selv har kastet, lager validerOgPakkUt en HttpServerErrorException - med
        // 404 i statusfeltet. Begge er RestClientResponseException.
        //
        // `postForEntity` og ikke `postForNonNullEntity`: sistnevnte lager en syntetisk
        // HttpServerErrorException med status 404 når kroppen er tom, og den ville ikke vært til å
        // skille fra en ekte 404 her. Et tomt svar er en feil hos tjenesten, ikke en beskjed om at
        // personen ikke finnes, og saksbehandleren skal ikke få vite det motsatte.
        val identer: List<PersonidentDto>? = try {
            postForEntity(
                uri,
                HentePersonidenterRequest(
                    ident = personident.verdi,
                    grupper = setOf(Identgruppe.AKTORID),
                    inkludereHistoriske = false,
                ),
            )
        } catch (exception: Exception) {
            if (exception is RestClientResponseException && exception.statusCode == HttpStatus.NOT_FOUND) {
                throw PersonIkkeFunnetException()
            }
            throw TjenesteFeilException(TJENESTE, exception)
        }
        if (identer == null) {
            throw TjenesteFeilException(TJENESTE, IllegalStateException("Tom kropp fra /personidenter"))
        }
        return identer.firstOrNull { it.gruppe == Identgruppe.AKTORID && !it.historisk }?.ident
    }
}

private const val TJENESTE = "bidrag-person"
