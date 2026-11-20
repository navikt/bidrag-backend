package no.nav.bidrag.beregn.core.consumer

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

class ValutakursConsumer(
    @Value($$"${BIDRAG_GRUNNLAG_URL}") private val url: URI,
    @Qualifier("azure") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate, "bidrag-grunnlag") {
    fun hentValutakurs(valutakode: Valutakode, dato: LocalDate): ValutakursgrunnlagResponse = getForNonNullEntity(
        UriComponentsBuilder.fromUri(url)
            .pathSegment("valutakursgrunnlag")
            .queryParam("valutakode", valutakode)
            .queryParam("dato", dato)
            .build()
            .toUri(),
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ValutakursgrunnlagResponse(
    val brukFra: LocalDateTime,
    val brukTil: LocalDateTime,
    val kurs: BigDecimal?,
    val multiplikator: Int?,
    val basisvaluta: Valutakode,
    val kvoteringsvaluta: Valutakode,
    val feiletHenting: Boolean,
)
