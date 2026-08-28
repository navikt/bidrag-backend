package no.nav.bidrag.dokument.arkivering.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.security.service.SecurityTokenService
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.commons.web.HttpResponse
import no.nav.bidrag.commons.web.HttpResponse.Companion.from
import no.nav.bidrag.dokument.arkivering.dto.ArkiverDecision
import no.nav.bidrag.dokument.arkivering.exceptions.HentingAvDokumentFeiletException
import no.nav.bidrag.transport.dokument.Avvikshendelse
import no.nav.bidrag.transport.dokument.BehandleAvvikshendelseResponse
import no.nav.bidrag.transport.dokument.EndreJournalpostCommand
import no.nav.bidrag.transport.dokument.JournalpostResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory
import java.util.stream.Collectors

private val log = KotlinLogging.logger {}

@Service
class BidragDokumentConsumer(
    @Qualifier("base") baseRestTemplate: HttpHeaderRestTemplate,
    @Value("\${bidrag.dokument.url}") bidragDokumentBaseUrl: String,
    securityTokenService: SecurityTokenService,
) {
    private lateinit var restTemplate: RestTemplate

    init {
        baseRestTemplate.uriTemplateHandler = DefaultUriBuilderFactory(bidragDokumentBaseUrl)
        baseRestTemplate.addHeaderGenerator(
            HttpHeaders.CONTENT_TYPE,
        ) { MediaType.APPLICATION_JSON_VALUE }
        baseRestTemplate.interceptors.add(
            securityTokenService.authTokenInterceptor("bidrag-dokument"),
        )
        restTemplate = baseRestTemplate
    }

    fun tilknyttSakerTilJoarkJournalpost(
        id: String,
        enhet: String,
        saker: List<String>,
    ): HttpResponse<JournalpostResponse> {
        val endreJournalpostRequest =
            EndreJournalpostCommand(
                tilknyttSaker = saker,
            )
        val joarkPrefix = "JOARK-"
        val path = String.format(ENDRE_JOURNALPOST_PATH, joarkPrefix + id)
        val headers = HttpHeaders()
        headers.add(EnhetFilter.X_ENHET_HEADER, enhet)
        log.info {
            "Tilknytter saker ${saker.stream().collect(
                Collectors.joining(","),
            )} til Joark journalpost $id ved å kalle bidrag-dokument$path"
        }
        val responsFraBdjp =
            restTemplate.exchange(
                path,
                HttpMethod.PATCH,
                HttpEntity(endreJournalpostRequest, headers),
                JournalpostResponse::class.java,
            )
        log.info { "Tilknytt saker fikk http status ${ responsFraBdjp.statusCode} fra bidrag-dokument" }
        return from(responsFraBdjp.statusCode, responsFraBdjp.body!!)
    }

    @Retryable(backoff = Backoff(delay = 500, maxDelay = 1500))
    open fun hentDokument(journalpostId: String): ByteArray {
        val safeJournalpostId =
            JOURNALPOST_ID_PATTERN.matchEntire(journalpostId)?.value
                ?: throw IllegalArgumentException("Ugyldig journalpostId: $journalpostId")
        val path = String.format(HENTE_DOKUMENT_PATH, safeJournalpostId)
        log.info { "Henter dokument fra bidrag-dokument$path" }
        return try {
            restTemplate.exchange(path, HttpMethod.GET, null, ByteArray::class.java).body!!
        } catch (e: HttpStatusCodeException) {
            if (HttpStatus.NOT_FOUND == e.statusCode) {
                throw HentingAvDokumentFeiletException(
                    String.format(
                        "Fant ikke dokument %s i midlertidig brevlager.",
                        journalpostId,
                    ),
                )
            }
            throw HentingAvDokumentFeiletException(
                String.format(
                    "Det skjedde en feil ved henting av dokument %s fra midlertidig brevlager.",
                    journalpostId,
                ),
                e,
            )
        }
    }

    fun hentBidragJournalpost(id: String): HttpResponse<JournalpostResponse> {
        val safeId = id.toLongOrNull()?.toString() ?: throw IllegalArgumentException("Ugyldig journalpostId: $id")
        val bidragPrefix = "BID-"
        val path = String.format(HENTE_JOURNALPOST_PATH, bidragPrefix + safeId)
        log.info { "Henter journalpost fra bidrag-dokument$path" }
        val responsFraBdjp =
            restTemplate.exchange(path, HttpMethod.GET, null, JournalpostResponse::class.java)
        log.info { "Hente journalpost fikk http status ${responsFraBdjp.statusCode} fra bidrag-dokument" }
        return from(responsFraBdjp.statusCode, responsFraBdjp.body!!)
    }

    fun kanArkivereJournalpost(journalpostId: String): ArkiverDecision {
        val safeId = journalpostId.toLongOrNull()?.toString() ?: throw IllegalArgumentException("Ugyldig journalpostId: $journalpostId")
        val bidragPrefix = "BID-"
        val path = String.format(KAN_DISTRIBUERE_JOURNALPOST_PATH, bidragPrefix + safeId)
        val headers = HttpHeaders()
        headers.add(EnhetFilter.X_ENHET_HEADER, "")
        return try {
            log.info { "Sjekker om journalpost $journalpostId kan arkiveres" }
            restTemplate.exchange(path, HttpMethod.GET, HttpEntity<Any>(headers), Void::class.java)
            ArkiverDecision(true, null)
        } catch (statusCodeException: HttpStatusCodeException) {
            val reason = getWarningHeader(statusCodeException)
            log.warn {
                "Kan ikke journalføre journalpost. Fikk status ${statusCodeException.statusCode} med feilmelding $reason"
            }
            ArkiverDecision(false, reason)
        }
    }

    fun getWarningHeader(statusCodeException: HttpStatusCodeException): String? {
        if (statusCodeException.responseHeaders == null) {
            return null
        }
        val warningHeaders = statusCodeException.responseHeaders?.get(HttpHeaders.WARNING)
        return if (warningHeaders.isNullOrEmpty()) {
            null
        } else {
            warningHeaders[0]
        }
    }

    fun sendAvvikHendelse(
        journalpostId: String,
        prefix: String,
        enhet: String,
        avvikshendelse: Avvikshendelse,
    ) {
        val safeId = journalpostId.toLongOrNull()?.toString() ?: throw IllegalArgumentException("Ugyldig journalpostId: $journalpostId")
        val path = String.format(AVVIK_PATH, prefix + safeId)
        log.info { "Sender avvik ${avvikshendelse.avvikType} til bidrag-dokument$path" }
        val headers = HttpHeaders()
        headers.add(EnhetFilter.X_ENHET_HEADER, enhet)
        val respons =
            restTemplate.exchange(
                path,
                HttpMethod.POST,
                HttpEntity(avvikshendelse, headers),
                BehandleAvvikshendelseResponse::class.java,
            )
        log.info {
            "Avvik ${avvikshendelse.avvikType} fikk http status ${respons.statusCode} fra bidrag-dokument"
        }
    }

    companion object {
        const val ENDRE_JOURNALPOST_PATH = "/journal/%s"

        const val HENTE_JOURNALPOST_PATH = "/journal/%s"
        const val HENTE_DOKUMENT_PATH = "/dokument/%s?resizeToA4=false&optimizeForPrint=false"
        val JOURNALPOST_ID_PATTERN = Regex("(BID-)?\\d+")

        const val KAN_DISTRIBUERE_JOURNALPOST_PATH = "/journal/distribuer/%s/enabled"

        const val AVVIK_PATH = "/journal/%s/avvik"
    }
}
