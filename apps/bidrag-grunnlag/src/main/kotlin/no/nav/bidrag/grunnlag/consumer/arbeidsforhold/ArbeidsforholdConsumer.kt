package no.nav.bidrag.grunnlag.consumer.arbeidsforhold

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api.Arbeidsforhold
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api.HentArbeidsforholdRequest
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
class ArbeidsforholdConsumer(
    @Value("\${AAREG_URL}") aaregUrl: URI,
    @Qualifier("azureService") private val restTemplate: RestTemplate,
    private val grunnlagConsumer: GrunnlagConsumer,
) : AbstractRestClient(restTemplate, "arbeidsforhold") {

    private val hentArbeidsforholdUri =
        UriComponentsBuilder
            .fromUri(aaregUrl)
            .pathSegment("api/v2/arbeidstaker/arbeidsforhold")
            .build()
            .toUriString()

    fun hentArbeidsforhold(request: HentArbeidsforholdRequest): RestResponse<List<Arbeidsforhold>> {
        val responseType = object : ParameterizedTypeReference<List<Arbeidsforhold>>() {}

        val restResponse = restTemplate.tryExchange(
            url = hentArbeidsforholdUri,
            httpMethod = HttpMethod.GET,
            httpEntity = grunnlagConsumer.initHttpEntityAareg(body = request, ident = request.arbeidstakerId),
            responseType = responseType,
            fallbackBody = emptyList(),
        )

        grunnlagConsumer.logResponse(type = "Arbeidsforhold", ident = request.arbeidstakerId, fom = null, tom = null, restResponse = restResponse)

        return restResponse
    }
}
