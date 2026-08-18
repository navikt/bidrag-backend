package no.nav.bidrag.grunnlag.consumer.pensjon

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.pensjon.api.BarnetilleggPensjon
import no.nav.bidrag.grunnlag.consumer.pensjon.api.HentBarnetilleggPensjonRequest
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.exception.tryExchange
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class PensjonConsumer(
    @Value("\${PENSJON_URL}") pensjonUrl: URI,
    @Qualifier("azureService") private val restTemplate: RestTemplate,
    private val grunnlagConsumer: GrunnlagConsumer,
) : AbstractRestClient(restTemplate, "pensjon") {

    private val hentBarnetilleggPensjonUri =
        UriComponentsBuilder
            .fromUri(pensjonUrl)
            .pathSegment("pen/api/barnetillegg/search")
            .build()
            .toUriString()

    fun hentBarnetilleggPensjon(request: HentBarnetilleggPensjonRequest): RestResponse<List<BarnetilleggPensjon>> {
        val responseType = object : ParameterizedTypeReference<List<BarnetilleggPensjon>>() {}

        val restResponse = restTemplate.tryExchange(
            url = hentBarnetilleggPensjonUri,
            httpMethod = HttpMethod.POST,
            httpEntity = grunnlagConsumer.initHttpEntity(request),
            responseType = responseType,
            fallbackBody = emptyList(),
        )

        grunnlagConsumer.logResponse(
            type = "Barnetillegg fra pensjon",
            ident = request.mottaker,
            fom = request.fom,
            tom = request.tom,
            restResponse = restResponse,
        )

        return restResponse
    }
}
