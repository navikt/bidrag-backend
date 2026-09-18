package no.nav.bidrag.grunnlag.consumer.valutakurser

import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxData
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxDimensions
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxSimplified
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxStructure
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.exception.tryExchange
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Service
class EuropeiskeSentralbankenConsumer(
    @Value("\${ECB_URL}") private val ecbUrl: URI,
    private val restTemplate: RestTemplate,
    private val grunnlagConsumer: GrunnlagConsumer,
) {

    fun hentValutakurs(valutakode: String, dato: LocalDate): RestResponse<SdmxData> {
        val hentEcbUri =
            UriComponentsBuilder
                .fromUri(ecbUrl)
                .pathSegment(byggEcbUrl(valutakode, dato))
                .build()
                .toUriString()

        val restResponse = restTemplate.tryExchange(
            url = hentEcbUri,
            httpMethod = HttpMethod.GET,
            httpEntity = grunnlagConsumer.initHttpEntityEcb(valutakode),
            responseType = SdmxData::class.java,
            fallbackBody = SdmxData(
                    dataSets = emptyList(),
                    structure = SdmxStructure(
                        dimensions = SdmxDimensions(series = emptyList()),
                    ),
            ),
        )

        return restResponse
    }

    private fun byggEcbUrl(valutakode: String, dato: LocalDate): String {
        val periodeFra = dato.minusMonths(1).withDayOfMonth(1)
        val periodeTil = dato.withDayOfMonth(1)
        val url =
            "service/data/EXR/M.E03.$valutakode.EN00.A?startPeriod=$periodeFra&endPeriod=$periodeTil&locale=no"
        secureLogger.info { url }
        return url
    }
}
