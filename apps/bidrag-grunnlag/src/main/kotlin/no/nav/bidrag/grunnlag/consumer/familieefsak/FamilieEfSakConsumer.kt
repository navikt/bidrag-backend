package no.nav.bidrag.grunnlag.consumer.familieefsak

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.familieefsak.api.BarnetilsynRequest
import no.nav.bidrag.grunnlag.consumer.familieefsak.api.BarnetilsynResponse
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.exception.tryExchange
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class FamilieEfSakConsumer(
    @Value("\${FAMILIEEFSAK_URL}") familieEfSakUrl: URI,
    @Qualifier("azureService") private val restTemplate: RestTemplate,
    private val grunnlagConsumer: GrunnlagConsumer,
) : AbstractRestClient(restTemplate, "familie-ef-sak") {

    private val hentBarnetilsynUri =
        UriComponentsBuilder
            .fromUri(familieEfSakUrl)
            .pathSegment("api/ekstern/bisys/perioder-barnetilsyn")
            .build()
            .toUriString()

    fun hentBarnetilsyn(request: BarnetilsynRequest): RestResponse<BarnetilsynResponse> {
        val restResponse = restTemplate.tryExchange(
            url = hentBarnetilsynUri,
            httpMethod = HttpMethod.POST,
            httpEntity = grunnlagConsumer.initHttpEntity(request),
            responseType = BarnetilsynResponse::class.java,
            fallbackBody = BarnetilsynResponse(emptyList()),
        )

        grunnlagConsumer.logResponse(
            type = "Barnetilsyn fra EF-Sak",
            ident = request.ident,
            fom = request.fomDato,
            tom = null,
            restResponse = restResponse,
        )

        return restResponse
    }
}
