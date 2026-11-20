package no.nav.bidrag.grunnlag.consumer.valutakurser

import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.valutakurs.domene.norgesbank.Frekvens
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxData
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxDimensions
import no.nav.bidrag.grunnlag.consumer.valutakurser.api.SdmxSimplified
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
import java.time.YearMonth

@Service
class NorgesBankConsumer(
    @Value("\${NORGESBANK_URL}") private val nbUrl: URI,
    @Qualifier("norgesBankRestTemplate")
    private val restTemplate: RestTemplate,
    private val grunnlagConsumer: GrunnlagConsumer,
) {
    fun hentValutakurs(frekvens: Frekvens, valuta: String, kursDato: LocalDate): RestResponse<SdmxSimplified> = restTemplate.tryExchange(
        url = lagNorgesBankURI(frekvens, valuta, kursDato).toString(),
        httpMethod = HttpMethod.GET,
        httpEntity = grunnlagConsumer.initHttpEntityNorgesBank(valuta),
        responseType = SdmxSimplified::class.java,
        fallbackBody = SdmxSimplified(SdmxData(emptyList(), SdmxStructure(SdmxDimensions(emptyList())))),
    )

    internal fun lagNorgesBankURI(frekvens: Frekvens, valuta: String, kursDato: LocalDate): URI {
        val periode = if (frekvens == Frekvens.MÅNEDLIG) YearMonth.from(kursDato).toString() else kursDato.toString()
        return URI.create(
            "${nbUrl.toString().trimEnd('/').removeSuffix("/api/data/EXR")}/api/data/EXR/${frekvens.verdi}.$valuta.NOK.SP?format=sdmx-json&startPeriod=$periode&endPeriod=$periode&locale=no",
        )
    }
}
