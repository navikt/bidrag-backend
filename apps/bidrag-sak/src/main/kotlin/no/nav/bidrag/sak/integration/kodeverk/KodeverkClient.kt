package no.nav.bidrag.sak.integration.kodeverk

import no.nav.bidrag.commons.web.client.AbstractPingableRestClient
import no.nav.bidrag.sak.integration.kodeverk.dto.KodeverkDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class KodeverkClient(
    @Value($$"${KODEVERK_URL}") private val kodeverkUri: URI,
    restTemplateBuilder: RestTemplateBuilder,
) : AbstractPingableRestClient(restTemplateBuilder.build(), "kodeverk") {
    override val pingUri: URI =
        UriComponentsBuilder
            .fromUri(kodeverkUri)
            .pathSegment(PATH_PING)
            .build()
            .toUri()

    fun hentPostnummer(): KodeverkDto = hentKodeverk("Postnummer")

    fun hentLandkoder(): KodeverkDto = hentKodeverk("Landkoder")

    fun hentLandkoderISO2(): KodeverkDto = hentKodeverk("LandkoderISO2")

    fun hentKodeverk(kodeverksnavn: String): KodeverkDto = getForNonNullEntity(kodeverkUri(kodeverksnavn))

    private fun kodeverkUri(kodeverksnavn: String): URI = UriComponentsBuilder
        .fromUri(kodeverkUri)
        .pathSegment("kodeverk/$kodeverksnavn")
        .build()
        .toUri()

    companion object {
        private const val PATH_PING = "internal/isAlive"
    }
}
