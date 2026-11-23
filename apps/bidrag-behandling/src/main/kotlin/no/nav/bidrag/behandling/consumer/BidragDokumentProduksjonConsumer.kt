package no.nav.bidrag.behandling.consumer

import no.nav.bidrag.behandling.dto.v2.notat.NotatDel
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.transport.dokumentmaler.notat.VedtakNotatDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class BidragDokumentProduksjonConsumer(
    @Value($$"${BIDRAG_DOKUMENT_PRODUKSJON_URL}") private val bidragDokumentProduksjonUrl: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "bidrag-dokument-produksjon") {
    private val bidragDokumentProduksjonUri
        get() = UriComponentsBuilder.fromUri(bidragDokumentProduksjonUrl).pathSegment("api", "v2", "notat", "pdf")

    // Utelater del for KOMPLETT slik at forespørselen for samlet notat er uendret
    fun opprettNotat(
        request: VedtakNotatDto,
        del: NotatDel = NotatDel.KOMPLETT,
    ): ByteArray = postForEntity(
        bidragDokumentProduksjonUri
            .apply { if (del != NotatDel.KOMPLETT) queryParam("del", del.name) }
            .build()
            .toUri(),
        request,
    )!!
}
