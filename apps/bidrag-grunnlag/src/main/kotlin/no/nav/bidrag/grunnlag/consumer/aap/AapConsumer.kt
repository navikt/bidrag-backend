package no.nav.bidrag.grunnlag.consumer.aap

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.aap.api.HentBarnetilleggAAPRequest
import no.nav.bidrag.grunnlag.consumer.aap.api.HentBarnetilleggAAPResponse
import no.nav.bidrag.grunnlag.consumer.familiekssak.api.BisysDto
import no.nav.bidrag.grunnlag.consumer.familiekssak.api.BisysResponsDto
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
class AapConsumer(
    @Value("\${AAP_URL}") aapUrl: URI,
    @Qualifier("azureService") private val restTemplate: RestTemplate,
    private val grunnlagConsumer: GrunnlagConsumer,
) : AbstractRestClient(restTemplate, "aap-api") {

    private val hentAapUri =
        UriComponentsBuilder
            .fromUri(aapUrl)
            .pathSegment("bisys/barnetillegg")
            .build()
            .toUriString()

    fun hentBarnetillegg(request: HentBarnetilleggAAPRequest): RestResponse<HentBarnetilleggAAPResponse> {
        val restResponse = restTemplate.tryExchange(
            url = hentAapUri,
            httpMethod = HttpMethod.POST,
            httpEntity = grunnlagConsumer.initHttpEntity(request),
            responseType = HentBarnetilleggAAPResponse::class.java,
            fallbackBody = HentBarnetilleggAAPResponse(emptyList()),
        )

        grunnlagConsumer.logResponse(
            type = "Barnetillegg fra AAP",
            ident = request.personidentifikator,
            fom = null,
            tom = null,
            restResponse = restResponse,
        )

        return restResponse
    }
}
