package no.nav.bidrag.grunnlag.consumer.arbeidsforhold

import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api.HentEnhetsregisterRequest
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.api.HentEnhetsregisterResponse
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.exception.tryExchange
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

@Service
class EnhetsregisterConsumer(
    @Value($$"${EREG_URL}") private val eregUrl: URI,
    private val restTemplate: RestTemplate,
    private val grunnlagConsumer: GrunnlagConsumer,
) {

    fun hentEnhetsinfo(request: HentEnhetsregisterRequest): RestResponse<HentEnhetsregisterResponse> {
        val hentEnhetsinfoUri =
            UriComponentsBuilder
                .fromUri(eregUrl)
                .pathSegment(byggEregUrl(request))
                .build()
                .toUriString()

        val restResponse = restTemplate.tryExchange(
            url = hentEnhetsinfoUri,
            httpMethod = HttpMethod.GET,
            httpEntity = grunnlagConsumer.initHttpEntityEreg(request),
            responseType = HentEnhetsregisterResponse::class.java,
            fallbackBody = HentEnhetsregisterResponse(),
        )

        grunnlagConsumer.logResponse(
            type = "Enhetsregister",
            ident = request.organisasjonsnummer,
            fom = null,
            tom = null,
            restResponse = restResponse,
        )

        return restResponse
    }

    private fun byggEregUrl(request: HentEnhetsregisterRequest): String {
        // Valideres og saneres for å hindre SSRF via ugyldige verdier i URL-en.
        val organisasjonsnummer = request.organisasjonsnummer
        require(organisasjonsnummer.matches(Regex("\\d+"))) { "Ugyldig organisasjonsnummer: $organisasjonsnummer" }
        val url = "v2/organisasjon/$organisasjonsnummer/noekkelinfo"

        val gyldigDato = request.gyldigDato
        if (gyldigDato.isNullOrBlank()) return url

        val validertGyldigDato = runCatching { LocalDate.parse(gyldigDato) }
            .getOrElse { throw IllegalArgumentException("Ugyldig gyldigDato: $gyldigDato") }
        return url.plus("?gyldigDato=$validertGyldigDato")
    }
}
