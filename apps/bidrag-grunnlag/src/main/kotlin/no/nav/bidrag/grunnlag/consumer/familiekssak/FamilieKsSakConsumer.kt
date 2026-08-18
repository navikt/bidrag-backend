package no.nav.bidrag.grunnlag.consumer.familiekssak

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
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
class FamilieKsSakConsumer(
    @Value("\${FAMILIEKSSAK_URL}") familieKsSakUrl: URI,
    @Qualifier("azureService") private val restTemplate: RestTemplate,
    private val grunnlagConsumer: GrunnlagConsumer,
) : AbstractRestClient(restTemplate, "familie-ks-sak") {

    private val hentFamilieKsSakUri =
        UriComponentsBuilder
            .fromUri(familieKsSakUrl)
            .pathSegment("api/bisys/hent-utbetalingsinfo")
            .build()
            .toUriString()

    fun hentKontantstøtte(request: BisysDto): RestResponse<BisysResponsDto> {
        val restResponse = restTemplate.tryExchange(
            url = hentFamilieKsSakUri,
            httpMethod = HttpMethod.POST,
            httpEntity = grunnlagConsumer.initHttpEntity(request),
            responseType = BisysResponsDto::class.java,
            fallbackBody = BisysResponsDto(emptyList(), emptyList()),
        )

        grunnlagConsumer.logResponse(
            type = "Kontantstøtte fra KS-Sak",
            ident = request.identer.first(),
            fom = request.fom,
            tom = null,
            restResponse = restResponse,
        )

        return restResponse
    }
}
