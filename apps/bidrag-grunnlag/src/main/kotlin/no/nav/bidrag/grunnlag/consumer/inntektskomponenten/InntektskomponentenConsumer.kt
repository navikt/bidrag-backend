package no.nav.bidrag.grunnlag.consumer.inntektskomponenten

import no.nav.bidrag.commons.web.client.AbstractRestClient
import no.nav.bidrag.grunnlag.SECURE_LOGGER
import no.nav.bidrag.grunnlag.consumer.GrunnlagConsumer
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.api.HentInntektListeRequest
import no.nav.bidrag.grunnlag.exception.RestResponse
import no.nav.bidrag.grunnlag.exception.tryExchange
import no.nav.bidrag.grunnlag.service.InntektskomponentenService
import no.nav.bidrag.grunnlag.util.GrunnlagUtil.Companion.tilJson
import no.nav.tjenester.aordningen.inntektsinformasjon.Aktoer
import no.nav.tjenester.aordningen.inntektsinformasjon.AktoerType
import no.nav.tjenester.aordningen.inntektsinformasjon.response.HentInntektListeResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import javax.naming.ServiceUnavailableException

@Service
class InntektskomponentenConsumer(
    @Value("\${INNTEKTSKOMPONENTEN_URL}") inntektskomponentenUrl: URI,
    @Qualifier("azureService") private val restTemplate: RestTemplate,
    private val grunnlagConsumer: GrunnlagConsumer,
    private val circuitBreakerFactory: CircuitBreakerFactory<*, *>,
) : AbstractRestClient(restTemplate, "inntektskomponenten") {

    private val circuitBreaker = circuitBreakerFactory.create("inntektskomponenten")

    private val hentInntekterUri =
        UriComponentsBuilder
            .fromUri(inntektskomponentenUrl)
            .pathSegment("rs/api/v1/hentinntektliste")
            .build()
            .toUriString()

    private val hentAbonnerteInntekterUri =
        UriComponentsBuilder
            .fromUri(inntektskomponentenUrl)
            .pathSegment("rs/api/v1/hentdetaljerteabonnerteinntekter")
            .build()
            .toUriString()

    fun hentInntekter(request: HentInntektListeRequest, abonnerteInntekterRequest: Boolean): RestResponse<HentInntektListeResponse> = circuitBreaker.run({
        val url = if (abonnerteInntekterRequest) hentAbonnerteInntekterUri else hentInntekterUri

        val restResponse = restTemplate.tryExchange(
            url = url,
            httpMethod = HttpMethod.POST,
            httpEntity = grunnlagConsumer.initHttpEntityInntektskomponenten(request),
            responseType = HentInntektListeResponse::class.java,
            fallbackBody = HentInntektListeResponse(emptyList(), Aktoer(request.ident.identifikator, AktoerType.NATURLIG_IDENT)),
        )
        when (restResponse) {
            is RestResponse.Success -> SECURE_LOGGER.info(
                "Henting av abonnerte ? $abonnerteInntekterRequest inntekter for perioden ${request.maanedFom} - ${request.maanedTom} " +
                    "ga følgende respons for ${request.ident.identifikator}: ${tilJson(restResponse.body)}",
            )

            is RestResponse.Failure -> {
                if (abonnerteInntekterRequest) {
                    // Utelater feillogging ved 400 - Bad Request (inntektsabonnement finnes ikke for personen)
                    // og 423 og 500 (inntektsabonnementet er ikke aktivt ennå)
                    if (restResponse.statusCode == HttpStatus.NOT_FOUND ||
                        restResponse.statusCode == HttpStatus.INTERNAL_SERVER_ERROR ||
                        restResponse.statusCode == HttpStatus.BAD_REQUEST ||
                        restResponse.statusCode == HttpStatus.LOCKED
                    ) {
                        InntektskomponentenService.LOGGER.warn(
                            "Mangler abonnement for henting av inntekter fra Inntektskomponenten. " +
                                "Statuskode ${restResponse.statusCode.value()}/${restResponse.message}",
                        )
                        SECURE_LOGGER.warn(
                            "Mangler abonnement for henting av inntekter fra Inntektskomponenten for ${request.ident.identifikator} for " +
                                "perioden ${request.maanedFom} - ${request.maanedTom}." +
                                " Prøver å hente inntekter uten abonnement. ${restResponse.message}",
                        )
                    } else {
                        InntektskomponentenService.LOGGER.error(
                            "Feil ved henting av inntekter med abonnement fra Inntektskomponenten. " +
                                "Statuskode ${restResponse.statusCode.value()}/${restResponse.message}",
                        )
                        SECURE_LOGGER.error(
                            "Feil ved henting av inntekter med abonnement for ${request.ident.identifikator} for perioden " +
                                "${request.maanedFom} - ${request.maanedTom}. Prøver å hente inntekter uten abonnement. /${restResponse.message}",
                        )
                    }
                } else {
                    InntektskomponentenService.LOGGER.error(
                        "Feil ved henting av inntekter uten abonnement fra Inntektskomponenten. " +
                            "Statuskode ${restResponse.statusCode.value()} /${restResponse.message}",
                    )
                    SECURE_LOGGER.error(
                        "Feil ved henting av inntekter uten abonnement for ${request.ident.identifikator} for perioden " +
                            "${request.maanedFom} - ${request.maanedTom}. Prøver å hente inntekter uten abonnement. /${restResponse.message}",
                    )
                }
            }
        }

        grunnlagConsumer.logResponse(logger = SECURE_LOGGER, restResponse = restResponse)

        restResponse
    }, { throwable ->
        håndtereFeil(throwable)
    })

    private fun håndtereFeil(throwable: Throwable?): RestResponse<HentInntektListeResponse> {
        SECURE_LOGGER.warn("Circuit breaker-logikk iverksatt for inntektskomponenten: ${throwable?.message}")
        return RestResponse.Failure("Inntektskomponenten svarer ikke", HttpStatus.SERVICE_UNAVAILABLE, ServiceUnavailableException())
    }
}
