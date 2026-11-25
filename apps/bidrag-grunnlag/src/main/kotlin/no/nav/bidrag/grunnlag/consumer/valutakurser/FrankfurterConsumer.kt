package no.nav.bidrag.grunnlag.consumer.valutakurser

import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.exception.tryExchange
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.time.YearMonth

data class FrankfurterRate(
    val date: LocalDate,
    val base: String,
    val quote: String,
    val rate: BigDecimal,
)

@Service
class FrankfurterConsumer(
    @Value("\${FRANKFURTER_URL:https://api.frankfurter.dev}") private val frankfurterUrl: URI,
    @Qualifier("frankfurterRestTemplate") private val restTemplate: RestTemplate,
) {
    fun hentMaanedskurs(
        basisvaluta: String,
        maaned: YearMonth,
    ): RestResponse<List<FrankfurterRate>> = restTemplate.tryExchange(
        url = lagURI(basisvaluta, maaned).toString(),
        httpMethod = HttpMethod.GET,
        httpEntity = HttpEntity.EMPTY,
        responseType = object : ParameterizedTypeReference<List<FrankfurterRate>>() {},
        fallbackBody = emptyList(),
    )

    internal fun lagURI(
        basisvaluta: String,
        maaned: YearMonth,
    ): URI = URI.create(
        "${frankfurterUrl.toString().trimEnd('/')}/v2/rates" +
            "?from=${maaned.atDay(1)}&to=${maaned.atEndOfMonth()}&group=month&base=$basisvaluta&quotes=NOK",
    )
}
