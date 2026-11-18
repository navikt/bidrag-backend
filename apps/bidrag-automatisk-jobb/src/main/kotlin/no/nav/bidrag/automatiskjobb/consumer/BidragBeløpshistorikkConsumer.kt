package no.nav.bidrag.automatiskjobb.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.beregn.barnebidrag.service.external.BeregningBeløpshistorikkConsumer
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentEngangsbeløpRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadHistoriskRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.LøpendeBidragPeriodeRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.LøpendeBidragssakerRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.EngangsbeløpDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidragPeriodeResponse
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidragssakerResponse
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

private val LOGGER = KotlinLogging.logger { }

@Component
class BidragBeløpshistorikkConsumer(
    @param:Value($$"${BIDRAG_BELOPSHISTORIKK_URL}") private val bidragBeløpshistorikkUrl: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "bidrag-stønad"),
    BeregningBeløpshistorikkConsumer {
    private val bidragBeløpshistorikkUri
        get() = UriComponentsBuilder.fromUri(bidragBeløpshistorikkUrl)

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 200, maxDelay = 1000, multiplier = 2.0),
    )
    override fun hentHistoriskeStønader(hentStønadHistoriskRequest: HentStønadHistoriskRequest): StønadDto? = try {
        postForEntity(
            bidragBeløpshistorikkUri.pathSegment("hent-stonad-historisk/").build().toUri(),
            hentStønadHistoriskRequest,
        )
    } catch (e: HttpStatusCodeException) {
        if (e.statusCode != HttpStatus.NOT_FOUND) throw e
        LOGGER.info { "Fant ikke historisk stønad for sak ${hentStønadHistoriskRequest.sak.verdi} i beløpshistorikk." }
        null
    }

    override fun hentLøpendeStønad(hentStønadRequest: HentStønadRequest): StønadDto? = try {
        postForEntity(
            bidragBeløpshistorikkUri.pathSegment("hent-stonad/").build().toUri(),
            hentStønadRequest,
        )
    } catch (e: HttpStatusCodeException) {
        if (e.statusCode != HttpStatus.NOT_FOUND) throw e
        LOGGER.info { "Fant ikke stønad for sak ${hentStønadRequest.sak.verdi} i beløpshistorikk." }
        null
    }

    fun hentEngangsbeløpForSak(sak: Saksnummer): List<EngangsbeløpDto> = getForNonNullEntity(
        bidragBeløpshistorikkUri.pathSegment("engangsbelop", sak.verdi).build().toUri(),
    )

    fun hentEngangsbeløp(hentEngangsbeløpRequest: HentEngangsbeløpRequest): EngangsbeløpDto? = try {
        postForEntity(
            bidragBeløpshistorikkUri.pathSegment("hent-engangsbelop").build().toUri(),
            hentEngangsbeløpRequest,
        )
    } catch (e: HttpStatusCodeException) {
        if (e.statusCode != HttpStatus.NOT_FOUND) throw e
        LOGGER.info { "Fant ikke engangsbeløp for sak ${hentEngangsbeløpRequest.sak.verdi} i beløpshistorikk." }
        null
    }

    override fun hentLøpendeBidrag(løpendeBidragssakerRequest: LøpendeBidragssakerRequest): LøpendeBidragssakerResponse {
        TODO("Not yet implemented")
    }

    override fun hentAlleLøpendeStønaderIPeriode(løpendeBidragPeriodeRequest: LøpendeBidragPeriodeRequest): LøpendeBidragPeriodeResponse {
        TODO("Not yet implemented")
    }
}
