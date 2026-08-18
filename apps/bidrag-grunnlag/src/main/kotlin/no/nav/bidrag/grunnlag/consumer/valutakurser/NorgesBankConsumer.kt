package no.nav.bidrag.grunnlag.consumer.valutakurser

import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api.HentEnhetsregisterRequest
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api.HentEnhetsregisterResponse
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxData
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxDimensions
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxSimplified
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxStructure
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursRequest
import no.nav.bidrag.grunnlag.consumer.valutakurser.dto.HentValutakursResponse
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.exception.tryExchange
import no.nav.bidrag.transport.felles.toYearMonth
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.time.YearMonth

@Service
class NorgesBankConsumer(
    @Value("\${NORGESBANK_URL}") private val nbUrl: URI,
    private val restTemplate: RestTemplate,
    private val grunnlagConsumer: GrunnlagConsumer,
) {

    fun hentValutakurs(valutakode: String, dato: LocalDate): RestResponse<SdmxSimplified> {
        val hentNbUri =
            UriComponentsBuilder
                .fromUri(nbUrl)
                .pathSegment(byggNbUrl(valutakode, dato))
                .build()
                .toUriString()

        val restResponse = restTemplate.tryExchange(
            url = hentNbUri,
            httpMethod = HttpMethod.GET,
            httpEntity = grunnlagConsumer.initHttpEntityNorgesBank(valutakode),
            responseType = SdmxSimplified::class.java,
            fallbackBody = SdmxSimplified(
                data = SdmxData(
                    dataSets = emptyList(),
                    structure = SdmxStructure(
                        dimensions = SdmxDimensions(series = emptyList()),
                    ),
                ),
            ),
        )

        return restResponse
    }

    private fun byggNbUrl(valutakode: String, dato: LocalDate): String {
        val periodeFra = dato.minusMonths(1).withDayOfMonth(1)
        val periodeTil = dato.withDayOfMonth(1)
        val url =
            "/api/data/EXR/M.$valutakode.NOK.SP?format=sdmx-json&startPeriod=$periodeFra&endPeriod=$periodeTil&locale=no"
        secureLogger.info { url }
        return url
    }
}

// https://data.norges-bank.no/api/data/EXR/M.USD.NOK.SP?format=sdmx-json&startPeriod=2025-10-01&endPeriod=2025-11-01&locale=no
//                            /api/data/EXR/M.ISK.NOK.SP?format=sdmx-json&startPeriod=2025-10&endPeriod=2025-11&locale=no
