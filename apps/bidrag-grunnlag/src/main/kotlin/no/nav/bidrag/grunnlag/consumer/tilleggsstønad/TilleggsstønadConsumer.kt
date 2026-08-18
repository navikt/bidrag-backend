package no.nav.bidrag.grunnlag.consumer.tilleggsstønad

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.TilleggsstønadRequest
import no.nav.bidrag.grunnlag.consumer.familiebasak.api.TilleggsstønadResponse
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
class TilleggsstønadConsumer(
    @Value("\${TILLEGGSSTONADERSAK_URL}") tilleggsstønadUrl: URI,
    @Qualifier("azureService") private val restTemplate: RestTemplate,
    private val grunnlagConsumer: GrunnlagConsumer,
) : AbstractRestClient(restTemplate, "tilleggsstønad") {

    private val hentTilleggsstønadUri =
        UriComponentsBuilder
            .fromUri(tilleggsstønadUrl)
            .pathSegment("api/ekstern/vedtak/tilsyn-barn")
            .build()
            .toUriString()

    fun hentTilleggsstønad(request: TilleggsstønadRequest): RestResponse<TilleggsstønadResponse> {
        val restResponse = restTemplate.tryExchange(
            url = hentTilleggsstønadUri,
            httpMethod = HttpMethod.POST,
            httpEntity = grunnlagConsumer.initHttpEntity(request),
            responseType = TilleggsstønadResponse::class.java,
            fallbackBody = TilleggsstønadResponse(false),
        )

        grunnlagConsumer.logResponse(
            type = "Tilleggsstønad til barnetilsyn",
            ident = request.ident,
            fom = null,
            tom = null,
            restResponse = restResponse,
        )

        return restResponse
    }
}
