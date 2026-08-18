package no.nav.bidrag.grunnlag.consumer.skattegrunnlag

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.skattegrunnlag.api.HentSummertSkattegrunnlagRequest
import no.nav.bidrag.grunnlag.consumer.skattegrunnlag.api.HentSummertSkattegrunnlagResponse
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.exception.tryExchange
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Service
class SigrunConsumer(
    @Value("\${SIGRUN_URL}") private val sigrunUrl: URI,
    @Qualifier("azureService") private val restTemplate: RestTemplate,
    private val grunnlagConsumer: GrunnlagConsumer,
) : AbstractRestClient(restTemplate, "sigrun") {

    fun hentSummertSkattegrunnlag(request: HentSummertSkattegrunnlagRequest): RestResponse<HentSummertSkattegrunnlagResponse> {
        val hentSummertSkattegrunnlagUri = UriComponentsBuilder
            .fromUri(sigrunUrl)
            .pathSegment("api/v2/summertskattegrunnlag")
            .build()
            .toUriString()

        val restResponse = restTemplate.tryExchange(
            url = hentSummertSkattegrunnlagUri,
            httpMethod = HttpMethod.POST,
            httpEntity = grunnlagConsumer.initHttpEntitySkattegrunnlag(request),
            responseType = HentSummertSkattegrunnlagResponse::class.java,
            fallbackBody = HentSummertSkattegrunnlagResponse(emptyList(), emptyList(), null),
        )

        grunnlagConsumer.logResponse(
            type = "Skattegrunnlag",
            ident = request.personident,
            fom = LocalDate.of(request.inntektsaar.toInt(), 1, 1),
            tom = null,
            restResponse = restResponse,
        )

        return restResponse
    }
}
