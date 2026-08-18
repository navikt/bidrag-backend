package no.nav.bidrag.organisasjon.consumer

import no.nav.bidrag.organisasjon.consumer.dto.SkjermingRequest
import no.nav.bidrag.organisasjon.exception.SkjermingConsumerException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder

class SkjermingConsumer(private val restTemplate: RestTemplate) {
    fun erPersonSkjermet(skjermingRequest: SkjermingRequest): Boolean {
        val uri = UriComponentsBuilder.fromPath(PATH_SKJERMING).toUriString()
        LOGGER.info("Skjerming uri: $uri")
        return try {
            val response = restTemplate.exchange<Boolean>(uri, HttpMethod.POST, createRequestEntity(skjermingRequest))
            response.body ?: false
        } catch (e: HttpClientErrorException) {
            val melding = "Feil ved kall til Skjerming API: " + e.message + ". Response body: " + e.responseBodyAsString
            LOGGER.error(melding)
            throw SkjermingConsumerException(melding, HttpStatus.valueOf(e.statusCode.value()))
        }
    }

    private fun createRequestEntity(body: SkjermingRequest): HttpEntity<SkjermingRequest> {
        val headers = initRequestHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return HttpEntity(body, headers)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SkjermingConsumer::class.java)
        private const val PATH_SKJERMING = "/skjermet"
    }
}
