package no.nav.bidrag.regnskap.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.security.maskinporten.MaskinportenClient
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.regnskap.behandlingsstatus.BehandlingsstatusResponse
import no.nav.bidrag.transport.regnskap.krav.Kravliste
import no.nav.bidrag.transport.regnskap.vedlikeholdsmodus.Vedlikeholdsmodus
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.net.URI

private val LOGGER = KotlinLogging.logger { }

@Service
class SkattConsumer(
    @param:Value("\${SKATT_URL}") private val skattUrl: String,
    @param:Value("\${maskinporten.scope}") private val scope: String,
    @param:Qualifier("regnskap") private val restTemplate: RestTemplate,
    private val maskinportenClient: MaskinportenClient,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        const val KRAV_PATH = "/api/krav"
        const val LIVENESS_PATH = "/api/liveness"
        const val VEDLIKEHOLDSMODUS_PATH = "/api/vedlikeholdsmodus"
    }

    fun sendKrav(kravliste: Kravliste): ResponseEntity<String> {
        secureLogger.debug { "Overfører krav til skatt:\n${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(kravliste)}" }
        return try {
            restTemplate.exchange<String>(
                opprettSkattUrl(KRAV_PATH),
                HttpMethod.POST,
                HttpEntity(kravliste, opprettHttpHeaders()),
            )
        } catch (e: HttpStatusCodeException) {
            ResponseEntity.status(e.statusCode).body(e.responseBodyAsString)
        }
    }

    @CacheEvict(value = ["vedlikeholdsmodus_cache"], allEntries = true)
    fun oppdaterVedlikeholdsmodus(vedlikeholdsmodus: Vedlikeholdsmodus): ResponseEntity<Any> {
        LOGGER.info { "Oppdaterer vedlikeholdsmodud til følgende: $vedlikeholdsmodus" }
        return restTemplate.exchange<Any>(
            opprettSkattUrl(VEDLIKEHOLDSMODUS_PATH),
            HttpMethod.POST,
            HttpEntity(vedlikeholdsmodus, opprettHttpHeaders()),
        )
    }

    @Cacheable(value = ["vedlikeholdsmodus_cache"], key = "#root.methodName")
    fun hentStatusPåVedlikeholdsmodus(): ResponseEntity<Any> = try {
        val response = restTemplate.exchange<Any>(
            opprettSkattUrl(LIVENESS_PATH),
            HttpMethod.GET,
            HttpEntity<String>(opprettHttpHeaders()),
        )
        LOGGER.info { "Status på vedlikeholdsmodus er $response." }
        ResponseEntity.status(response.statusCode).body(response.body)
    } catch (e: HttpStatusCodeException) {
        ResponseEntity.status(e.statusCode).body(e.responseBodyAsString)
    }

    fun sjekkBehandlingsstatus(batchUid: String): ResponseEntity<BehandlingsstatusResponse> {
        LOGGER.debug { "Henter behandlingsstatus for batchUid: $batchUid" }
        val response = restTemplate.exchange<BehandlingsstatusResponse>(
            opprettSkattUrl("$KRAV_PATH/$batchUid"),
            HttpMethod.GET,
            HttpEntity<String>(opprettHttpHeaders()),
        )
        return ResponseEntity.status(response.statusCode).body(response.body)
    }

    private fun opprettSkattUrl(path: String): URI = URI.create(skattUrl + path)

    private fun opprettHttpHeaders(): HttpHeaders {
        val httpHeaders = HttpHeaders()
        httpHeaders.set("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE)
        httpHeaders.set("Authorization", "Bearer " + hentJwtToken())
        return httpHeaders
    }

    private fun hentJwtToken(): String = maskinportenClient.hentMaskinportenToken(scope).parsedString
}
