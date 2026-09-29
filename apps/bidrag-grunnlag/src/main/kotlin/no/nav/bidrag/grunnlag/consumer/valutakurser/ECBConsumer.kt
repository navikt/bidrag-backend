package no.nav.bidrag.grunnlag.consumer.valutakurser

import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.ecb.Frequency
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxData
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxDimensions
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxStructure
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.exception.tryExchange
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.LocalDate

@Service
class ECBConsumer(
    @Value("\${ECB_URL}") private val ecbUrl: URI,
    @Qualifier("ecbRestTemplate") private val restTemplate: RestTemplate,
    private val grunnlagConsumer: GrunnlagConsumer,
) {
    fun hentValutakurs(
        frequency: Frequency,
        currencies: List<String>,
        exchangeRateDate: LocalDate,
    ): RestResponse<SdmxData> = restTemplate.tryExchange(
        url = lagECBURI(frequency, currencies, exchangeRateDate).toString(),
        httpMethod = HttpMethod.GET,
        httpEntity = grunnlagConsumer.initHttpEntityEcb(currencies.joinToString("+")),
        responseType = SdmxData::class.java,
        fallbackBody = SdmxData(emptyList(), SdmxStructure(SdmxDimensions(emptyList()))),
    )

    internal fun lagECBURI(
        frequency: Frequency,
        currencies: List<String>,
        exchangeRateDate: LocalDate,
    ): URI = URI.create(
        "${ecbUrl.toString().trimEnd('/').removeSuffix("/service/data/EXR")}/service/data/EXR/${frequency.toFrequencyParam()}.${currencies.joinToString("+")}.EUR.SP00.A${frequency.toQueryParams(exchangeRateDate)}",
    )
}
