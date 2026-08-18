package no.nav.bidrag.sak.integration.organisasjon

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.organisasjon.EnhetDto
import no.nav.bidrag.transport.organisasjon.HentEnhetRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class BidragOrganisasjonClient(
    @Value($$"${BIDRAG_ORGANISASJON_URL}") bidragOrganisasjonBaseUrl: URI,
    @Qualifier("azure") private val restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidragOrganisasjon") {
    private val bidragOrganisasjonUri =
        UriComponentsBuilder
            .fromUri(bidragOrganisasjonBaseUrl)
            .pathSegment("arbeidsfordeling", "enhet", "geografisktilknytning")
            .build()
            .toUri()

    fun hentEnhetForArbeidsfordelingGeografiskTilknytning(hentEnhetRequest: HentEnhetRequest): EnhetDto = postForNonNullEntity(bidragOrganisasjonUri, hentEnhetRequest)
}
