package no.nav.bidrag.sak.integration.person

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.Graderingsinfo
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class BidragPersonClient(
    @Value($$"${BIDRAG_PERSON_URL}") bidragPersonBaseUrl: URI,
    @Qualifier("azure") restTemplate: RestOperations,
) : AbstractRestClient(restTemplate, "bidragPerson") {
    private val graderingsinfoUri =
        UriComponentsBuilder
            .fromUri(bidragPersonBaseUrl)
            .pathSegment("graderingsinfo")
            .build()
            .toUri()

    /**
     * Henter adressebeskyttelse fra PDL og skjerming fra skjermingsregisteret for personene.
     * Alle feil kastes videre.
     */
    fun hentGraderingsinfo(identer: Set<Personident>): Graderingsinfo = postForNonNullEntity(graderingsinfoUri, identer)
}
