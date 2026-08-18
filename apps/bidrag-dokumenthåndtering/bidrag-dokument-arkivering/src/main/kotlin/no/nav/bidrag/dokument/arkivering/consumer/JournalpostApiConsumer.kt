package no.nav.bidrag.dokument.arkivering.consumer

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.commons.web.HttpResponse
import no.nav.bidrag.commons.web.HttpResponse.Companion.from
import no.nav.bidrag.dokument.arkivering.dto.ArkivereJournalpostResponse
import no.nav.bidrag.dokument.arkivering.dto.OpprettJournalpostRequest
import no.nav.bidrag.dokument.arkivering.dto.OpprettJournalpostResponse
import no.nav.bidrag.transport.dokument.JournalpostResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory
import java.util.Optional

private val log = KotlinLogging.logger {}

@Service
class JournalpostApiConsumer(
    @Qualifier("base") baseRestTemplate: HttpHeaderRestTemplate,
    @Value("\${dokarkiv.journalpostapi.v1.url}") dokarkivBaseUrl: String,
    securityTokenService: SecurityTokenService,
    private val objectMapper: ObjectMapper,
) {
    lateinit var restTemplate: RestTemplate

    init {
        baseRestTemplate.uriTemplateHandler = DefaultUriBuilderFactory(dokarkivBaseUrl)
        baseRestTemplate.addHeaderGenerator(
            HttpHeaders.CONTENT_TYPE,
        ) { MediaType.APPLICATION_JSON_VALUE }
        baseRestTemplate.interceptors.add(securityTokenService.authTokenInterceptor("dokarkiv"))
        restTemplate = baseRestTemplate
    }

    fun arkivereJournalpost(
        journalpostResponse: JournalpostResponse,
        fysiskDokument: ByteArray?,
    ): HttpResponse<ArkivereJournalpostResponse> {
        val endpoint = "$ARKIVER_JOURNALPOST_PATH?forsoekFerdigstill=true"
        val request = OpprettJournalpostRequest(journalpostResponse, fysiskDokument!!)
        log.info { "Arkiverer journalpost ${journalpostResponse.journalpost!!.journalpostId} i Joark" }
        return try {
            val responseEntity =
                restTemplate.postForEntity(
                    endpoint,
                    HttpEntity(request),
                    OpprettJournalpostResponse::class.java,
                )
            Optional
                .ofNullable<ResponseEntity<OpprettJournalpostResponse>>(responseEntity)
                .map { response: ResponseEntity<OpprettJournalpostResponse> ->
                    from(
                        response.statusCode,
                        ArkivereJournalpostResponse(
                            response.body!!,
                            journalpostResponse.journalpost!!.journalpostId,
                        ),
                    )
                }.orElseGet { from(HttpStatus.INTERNAL_SERVER_ERROR) }
        } catch (clientErrorException: HttpClientErrorException) {
            if (clientErrorException.statusCode === HttpStatus.CONFLICT) {
                log.info {
                    "Journalpost med eksternReferanseId ${journalpostResponse.journalpost!!.journalpostId} er allerede arkivert i Joark"
                }
                return handleConflictResponse(clientErrorException, journalpostResponse)
            }
            throw clientErrorException
        }
    }

    private fun handleConflictResponse(
        clientErrorException: HttpClientErrorException,
        journalpostResponse: JournalpostResponse,
    ): HttpResponse<ArkivereJournalpostResponse> {
        val opprettJournalpostResponse =
            convertStringToResponse(clientErrorException.responseBodyAsString)
        return Optional
            .ofNullable(opprettJournalpostResponse)
            .map { response: OpprettJournalpostResponse? ->
                from(
                    HttpStatus.OK,
                    ArkivereJournalpostResponse(
                        response!!,
                        journalpostResponse.journalpost!!.journalpostId,
                    ),
                )
            }.orElseGet { from(HttpStatus.INTERNAL_SERVER_ERROR) }
    }

    private fun convertStringToResponse(responseString: String): OpprettJournalpostResponse? = try {
        objectMapper.readValue(responseString, OpprettJournalpostResponse::class.java)
    } catch (e: JsonProcessingException) {
        null
    }

    companion object {
        const val ARKIVER_JOURNALPOST_PATH = "/rest/journalpostapi/v1/journalpost"
    }
}
