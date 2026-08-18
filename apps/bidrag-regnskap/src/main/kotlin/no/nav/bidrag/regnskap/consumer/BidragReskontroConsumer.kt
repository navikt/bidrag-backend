package no.nav.bidrag.regnskap.consumer

import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.reskontro.request.SaksnummerRequest
import no.nav.bidrag.transport.reskontro.response.transaksjoner.TransaksjonerDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class BidragReskontroConsumer(
    @param:Value($$"${BIDRAG_RESKONTRO_URL}") val uri: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "bidrag-reskontro") {
    private val bidragReskontroUri
        get() = UriComponentsBuilder.fromUri(uri)

    fun hentTransaksjonerForSak(saksnummer: String): TransaksjonerDto? = try {
        postForEntity<TransaksjonerDto>(
            bidragReskontroUri
                .pathSegment("transaksjoner/bidragssak")
                .build()
                .toUri(),
            SaksnummerRequest(Saksnummer(saksnummer)),
        )
    } catch (e: HttpStatusCodeException) {
        val problem = runCatching { e.getResponseBodyAs(ProblemDetail::class.java) }.getOrNull()
        secureLogger.error(e) { "${problem?.title ?: "Ukjent feil"}: ${problem?.status ?: e.statusCode.value()} - ${problem?.detail ?: e.responseBodyAsString}" }
        throw e
    }
}
