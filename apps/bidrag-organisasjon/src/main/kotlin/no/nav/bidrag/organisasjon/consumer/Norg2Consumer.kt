package no.nav.bidrag.organisasjon.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.commons.web.HttpResponse
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.organisasjon.CacheConfig.Companion.ARBEIDSFORDELING_ENHET
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterBestMatchRequest
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterBestMatchResponse
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterRequest
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterRequestBody
import no.nav.bidrag.organisasjon.consumer.dto.ArbeidsfordelingEnheterResponse
import no.nav.bidrag.organisasjon.consumer.dto.EnhetArbeidsfordelingRespons
import no.nav.bidrag.organisasjon.consumer.dto.EnhetInfoResponse
import no.nav.bidrag.organisasjon.exception.ArbeidsfordelingConsumerException
import no.nav.bidrag.organisasjon.exception.EnhetIkkeFunnetException
import no.nav.bidrag.organisasjon.service.EnhetYamlConverter.hentAlleEnheterGrupper
import no.nav.bidrag.transport.organisasjon.EnhetDetaljerDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.DefaultUriBuilderFactory
import org.springframework.web.util.UriComponentsBuilder

@Service
class Norg2Consumer(@Value($$"${ARBEIDSFORDELING_URL}") arbeidsfordelingBaseUrl: String, private var restTemplate: RestTemplate = RestTemplate()) {
    private val enhetKontaktinformasjonMap: Map<String, EnhetDetaljerDto> = hentAlleEnheterGrupper().flatMap { it.enheter }.associateBy { it.enhetId }

    init {
        restTemplate.uriTemplateHandler = DefaultUriBuilderFactory(arbeidsfordelingBaseUrl)
    }

    @Cacheable(ARBEIDSFORDELING_ENHET)
    fun hentArbeidsfordelingForEnhet(enhetsnummer: Enhetsnummer): List<EnhetArbeidsfordelingRespons>? {
        LOGGER.info("NORG2 hent enhetinfo for enhet {}", enhetsnummer)
        return try {
            val responseType = object : ParameterizedTypeReference<List<EnhetArbeidsfordelingRespons>>() {}
            restTemplate.exchange(
                "/enhet/$enhetsnummer/arbeidsfordeling",
                HttpMethod.GET,
                createRequestEntity<Void>(),
                responseType,
            ).body
        } catch (e: HttpClientErrorException) {
            if (HttpStatus.NOT_FOUND == e.statusCode) {
                throw EnhetIkkeFunnetException(String.format("Enhet med id %s ikke funnet", enhetsnummer), e)
            }
            val melding = "Feil ved kall til NORG2 Arbeidsfordeling API: " + e.message + ". Response body: " + e.responseBodyAsString
            LOGGER.error(melding)
            throw ArbeidsfordelingConsumerException(melding, HttpStatus.valueOf(e.statusCode.value()))
        }
    }

    fun hentEnhetInfo(enhetsnummer: Enhetsnummer): EnhetInfoResponse {
        LOGGER.info("NORG2 hent enhetinfo for enhet {}", enhetsnummer)
        return try {
            val response = restTemplate.exchange("/enhet/$enhetsnummer", HttpMethod.GET, createRequestEntity<Void>(), EnhetInfoResponse::class.java)
            response.body!!
        } catch (e: HttpClientErrorException) {
            if (HttpStatus.NOT_FOUND == e.statusCode) {
                throw EnhetIkkeFunnetException(String.format("Enhet med id %s ikke funnet", enhetsnummer), e)
            }
            val melding = "Feil ved kall til NORG2 Arbeidsfordeling API: " + e.message + ". Response body: " + e.responseBodyAsString
            LOGGER.error(melding)
            throw ArbeidsfordelingConsumerException(melding, HttpStatus.valueOf(e.statusCode.value()))
        }
    }

    fun hentEnhetKontaktinfo(enhetsnummer: String): EnhetDetaljerDto? {
        LOGGER.info("Hentet enhet kontaktinfo for enhet {}", enhetsnummer)
        return enhetKontaktinformasjonMap[enhetsnummer]
    }

    fun finnArbeidsfordelingEnheterBestMatch(
        arbeidsfordelingEnheterBestMatchRequest: ArbeidsfordelingEnheterBestMatchRequest,
    ): List<ArbeidsfordelingEnheterBestMatchResponse>? {
        val uri = UriComponentsBuilder.fromPath(PATH_ARBEIDSFORDELING_ENHETER_BESTMATCH).toUriString()
        LOGGER.info("Arbeidsfordeling enheter bestmatch uri: $uri")
        return try {
            val response: ResponseEntity<List<ArbeidsfordelingEnheterBestMatchResponse>> =
                restTemplate.exchange(uri, HttpMethod.POST, createRequestEntity(arbeidsfordelingEnheterBestMatchRequest))
            response.body
        } catch (e: HttpClientErrorException) {
            val melding = "Feil ved kall til NORG2 Arbeidsfordeling API. Feilmelding: " + getErrorMessage(e)
            LOGGER.error(melding)
            throw ArbeidsfordelingConsumerException(melding, HttpStatus.valueOf(e.statusCode.value()))
        }
    }

    fun finnArbeidsfordelingEnheterListe(
        arbeidsfordelingEnheterRequest: ArbeidsfordelingEnheterRequest,
    ): HttpResponse<List<ArbeidsfordelingEnheterResponse>> {
        val uri = buildUriArbeidsfordelingEnheterListe(arbeidsfordelingEnheterRequest.typeListe)
        LOGGER.info("Arbeidsfordeling enheter liste uri: $uri")
        val requestEntity: HttpEntity<ArbeidsfordelingEnheterRequestBody> =
            createRequestEntity(ArbeidsfordelingEnheterRequestBody(arbeidsfordelingEnheterRequest.tema))
        return try {
            val response: ResponseEntity<List<ArbeidsfordelingEnheterResponse>> = restTemplate.exchange(uri, HttpMethod.POST, requestEntity)
            HttpResponse.from(HttpStatus.valueOf(response.statusCode.value()), response.body)
        } catch (e: HttpClientErrorException) {
            val melding = (
                "Feil ved kall til NORG2 Arbeidsfordeling API (endpoint: " + PATH_ARBEIDSFORDELING_ENHETER_LISTE + ") : " + e.message +
                    ". Response body: " + e.responseBodyAsString
                )
            LOGGER.error(melding)
            throw ArbeidsfordelingConsumerException(melding, HttpStatus.valueOf(e.statusCode.value()))
        }
    }

    private fun <T : Any> createRequestEntity(body: T? = null): HttpEntity<T> {
        val headers = initRequestHeaders()
        if (body != null) {
            headers.contentType = MediaType.APPLICATION_JSON
        }
        return HttpEntity(body, headers)
    }

    private fun buildUriArbeidsfordelingEnheterListe(typeListe: List<String>): String = UriComponentsBuilder.fromPath(PATH_ARBEIDSFORDELING_ENHETER_LISTE).queryParam("enhetstype", typeListe).toUriString()

    private fun getErrorMessage(e: HttpClientErrorException): String = try {
        ObjectMapper().findAndRegisterModules().readTree(e.responseBodyAsString)["message"].asText()
    } catch (err: Exception) {
        e.responseBodyAsString
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(Norg2Consumer::class.java)
        private const val PATH_ARBEIDSFORDELING_ENHETER_BESTMATCH = "/arbeidsfordeling/enheter/bestmatch"
        private const val PATH_ARBEIDSFORDELING_ENHETER_LISTE = "/arbeidsfordeling/enheter"
        private const val PATH_ENHET_KONTAKTINFO = "/enhet/%s/kontaktinformasjon"
    }
}
