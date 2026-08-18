package no.nav.bidrag.grunnlag.consumer.familiebasak

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.FamilieBaSakRequest
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.FamilieBaSakResponse
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
class FamilieBaSakConsumer(
    @Value("\${FAMILIEBASAK_URL}") familieBaSakUrl: URI,
    @Qualifier("azureService") private val restTemplate: RestTemplate,
    private val grunnlagConsumer: GrunnlagConsumer,
) : AbstractRestClient(restTemplate, "familie-ba-sak") {

    private val hentFamilieBaSakUri =
        UriComponentsBuilder
            .fromUri(familieBaSakUrl)
            .pathSegment("api/bisys/hent-utvidet-barnetrygd")
            .build()
            .toUriString()

    fun hentFamilieBaSak(request: FamilieBaSakRequest): RestResponse<FamilieBaSakResponse> {
        val restResponse = restTemplate.tryExchange(
            url = hentFamilieBaSakUri,
            httpMethod = HttpMethod.POST,
            httpEntity = grunnlagConsumer.initHttpEntity(request),
            responseType = FamilieBaSakResponse::class.java,
            fallbackBody = FamilieBaSakResponse(emptyList()),
        )

        grunnlagConsumer.logResponse(
            type = "Utvidet barnetrygd og småbarnstillegg",
            ident = request.personIdent,
            fom = request.fraDato,
            tom = null,
            restResponse = restResponse,
        )

        return restResponse
    }
}
