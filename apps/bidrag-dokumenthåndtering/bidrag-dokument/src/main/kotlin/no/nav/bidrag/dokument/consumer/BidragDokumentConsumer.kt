package no.nav.bidrag.dokument.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.commons.web.HttpResponse
import no.nav.bidrag.transport.dokument.AvvikType
import no.nav.bidrag.transport.dokument.Avvikshendelse
import no.nav.bidrag.transport.dokument.BehandleAvvikshendelseResponse
import no.nav.bidrag.transport.dokument.DistribuerJournalpostRequest
import no.nav.bidrag.transport.dokument.DistribuerJournalpostResponse
import no.nav.bidrag.transport.dokument.DistribusjonInfoDto
import no.nav.bidrag.transport.dokument.DokumentMetadata
import no.nav.bidrag.transport.dokument.EndreJournalpostCommand
import no.nav.bidrag.transport.dokument.JournalpostDto
import no.nav.bidrag.transport.dokument.JournalpostResponse
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest
import no.nav.bidrag.transport.dokument.OpprettJournalpostResponse
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

private val log = KotlinLogging.logger {}

class BidragDokumentConsumer(
    private val name: String,
    private val restTemplate: RestTemplate,
    private val rootUri: String,
    private val metricsRegistry: MeterRegistry,
) {
    fun finnAvvik(
        saksnummer: String?,
        journalpostId: String?,
    ): HttpResponse<List<AvvikType>> {
        val avviksResponse =
            if (saksnummer != null) {
                restTemplate.exchange(
                    PATH_AVVIK_PA_JOURNALPOST_MED_SAK_PARAM_TEMPLATE,
                    HttpMethod.GET,
                    null,
                    typereferansenErListeMedAvvikstyper(),
                    uriVariable(journalpostId),
                    saksnummer,
                )
            } else {
                restTemplate.exchange(
                    PATH_AVVIK_PA_JOURNALPOST_TEMPLATE,
                    HttpMethod.GET,
                    null,
                    typereferansenErListeMedAvvikstyper(),
                    uriVariable(journalpostId),
                )
            }
        return HttpResponse(avviksResponse)
    }

    fun behandleAvvik(
        enhetsnummer: String?,
        journalpostId: String?,
        avvikshendelse: Avvikshendelse?,
    ): HttpResponse<BehandleAvvikshendelseResponse> {
        val avviksResponse =
            restTemplate
                .exchange(
                    PATH_AVVIK_PA_JOURNALPOST_TEMPLATE,
                    HttpMethod.POST,
                    HttpEntity(avvikshendelse, createEnhetHeader(enhetsnummer)),
                    BehandleAvvikshendelseResponse::class.java,
                    uriVariable(journalpostId),
                )
        return HttpResponse(avviksResponse)
    }

    fun hentJournalpost(
        saksnummer: String?,
        id: String?,
    ): HttpResponse<JournalpostResponse> {
        val journalpostExchange =
            if (saksnummer == null) {
                restTemplate.exchange(
                    PATH_JOURNALPOST_TEMPLATE,
                    HttpMethod.GET,
                    null,
                    JournalpostResponse::class.java,
                    uriVariable(id),
                )
            } else {
                restTemplate.exchange(
                    PATH_JOURNALPOST_MED_SAKPARAM_TEMPLATE,
                    HttpMethod.GET,
                    null,
                    JournalpostResponse::class.java,
                    uriVariable(id),
                    saksnummer,
                )
            }
        return HttpResponse(journalpostExchange)
    }

    fun finnJournalposter(
        saksnummer: String?,
        fagomrade: List<String> = emptyList(),
    ): List<JournalpostDto> {
        val uriBuilder =
            UriComponentsBuilder
                .fromUriString(rootUri)
                .path("/sak")
                .pathSegment(uriVariable(saksnummer))
                .path("/journal")
        fagomrade.forEach { uriBuilder.queryParam(PARAM_FAGOMRADE, it) }
        val uri = uriBuilder.build().encode().toUri()
        log.info { "Henter journalposter for sak $saksnummer" }
        val timer = metricsRegistry.timer("finnJournalposter", "service", name)
        return try {
            val journalposterFraArkiv =
                timer.recordCallable {
                    restTemplate
                        .exchange(uri, HttpMethod.GET, null, typereferansenErListeMedJournalposter())
                }!!
            journalposterFraArkiv.body ?: emptyList()
        } catch (e: HttpStatusCodeException) {
            log.error(e) {
                "Det skjedde en feil ved henting av journal for sak $saksnummer og fagområder ${
                    fagomrade.joinToString(
                        ",",
                    )
                } fra url $uri"
            }
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                emptyList()
            } else {
                throw e
            }
        }
    }

    fun endre(
        enhet: String?,
        endreJournalpostCommand: EndreJournalpostCommand,
    ): HttpResponse<Void> {
        val endretJournalpostResponse =
            restTemplate
                .exchange(
                    PATH_JOURNALPOST_TEMPLATE,
                    HttpMethod.PATCH,
                    HttpEntity(endreJournalpostCommand, createEnhetHeader(enhet)),
                    Void::class.java,
                    uriVariable(endreJournalpostCommand.journalpostId),
                )
        return HttpResponse(endretJournalpostResponse)
    }

    fun opprett(opprettJournalpostRequest: OpprettJournalpostRequest): HttpResponse<OpprettJournalpostResponse> {
        val endretJournalpostResponse =
            restTemplate
                .exchange(
                    PATH_OPPRETT_JOURNALPOST,
                    HttpMethod.POST,
                    HttpEntity(opprettJournalpostRequest),
                    OpprettJournalpostResponse::class.java,
                )
        return HttpResponse(endretJournalpostResponse)
    }

    fun distribuerJournalpost(
        journalpostId: String?,
        batchId: String?,
        distribuerJournalpostRequest: DistribuerJournalpostRequest,
    ): HttpResponse<DistribuerJournalpostResponse> {
        val distribuerJournalpostResponse =
            if (!batchId.isNullOrEmpty()) {
                restTemplate
                    .exchange(
                        PATH_DISTRIBUER_MED_BATCHID_TEMPLATE,
                        HttpMethod.POST,
                        HttpEntity(distribuerJournalpostRequest),
                        DistribuerJournalpostResponse::class.java,
                        uriVariable(journalpostId),
                        batchId,
                    )
            } else {
                restTemplate
                    .exchange(
                        PATH_DISTRIBUER_TEMPLATE,
                        HttpMethod.POST,
                        HttpEntity(distribuerJournalpostRequest),
                        DistribuerJournalpostResponse::class.java,
                        uriVariable(journalpostId),
                    )
            }
        return HttpResponse(distribuerJournalpostResponse)
    }

    fun kanDistribuereJournalpost(journalpostId: String?): HttpResponse<Void> {
        val distribuerJournalpostResponse =
            restTemplate.exchange(
                PATH_DISTRIBUER_ENABLED_TEMPLATE,
                HttpMethod.GET,
                null,
                Void::class.java,
                uriVariable(journalpostId),
            )
        return HttpResponse(distribuerJournalpostResponse)
    }

    fun hentDistribusjonsInfo(journalpostId: String): DistribusjonInfoDto? = restTemplate
        .exchange(
            PATH_HENT_DIST_INFO_TEMPLATE,
            HttpMethod.GET,
            null,
            DistribusjonInfoDto::class.java,
            uriVariable(journalpostId),
        ).body

    fun hentDokument(
        journalpostId: String?,
        dokumentreferanse: String?,
    ): ResponseEntity<ByteArray> {
        if (journalpostId.isNullOrEmpty()) return hentDokument(dokumentreferanse)
        return if (dokumentreferanse.isNullOrEmpty()) {
            restTemplate.exchange(
                PATH_HENT_DOKUMENT_TEMPLATE,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                ByteArray::class.java,
                uriVariable(journalpostId),
            )
        } else {
            restTemplate.exchange(
                PATH_HENT_DOKUMENT_MED_REFERANSE_TEMPLATE,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                ByteArray::class.java,
                uriVariable(journalpostId),
                uriVariable(dokumentreferanse),
            )
        }
    }

    fun hentDokumentMetadata(
        journalpostId: String?,
        dokumentreferanse: String?,
    ): List<DokumentMetadata> {
        if (journalpostId.isNullOrEmpty()) {
            return hentDokumentMetadata(dokumentreferanse)
                ?: emptyList()
        }
        return if (dokumentreferanse.isNullOrEmpty()) {
            restTemplate
                .exchange(
                    PATH_HENT_DOKUMENT_TEMPLATE,
                    HttpMethod.OPTIONS,
                    HttpEntity.EMPTY,
                    object : ParameterizedTypeReference<List<DokumentMetadata>>() {},
                    uriVariable(journalpostId),
                ).body
                ?: emptyList()
        } else {
            restTemplate
                .exchange(
                    PATH_HENT_DOKUMENT_MED_REFERANSE_TEMPLATE,
                    HttpMethod.OPTIONS,
                    HttpEntity.EMPTY,
                    object : ParameterizedTypeReference<List<DokumentMetadata>>() {},
                    uriVariable(journalpostId),
                    uriVariable(dokumentreferanse),
                ).body
                ?: emptyList()
        }
    }

    fun hentDokumentMetadata(dokumentreferanse: String?): List<DokumentMetadata>? = restTemplate
        .exchange(
            PATH_HENT_DOKUMENT_REFERANSE_TEMPLATE,
            HttpMethod.OPTIONS,
            HttpEntity.EMPTY,
            object : ParameterizedTypeReference<List<DokumentMetadata>>() {},
            uriVariable(dokumentreferanse),
        ).body

    fun hentDokument(dokumentreferanse: String?): ResponseEntity<ByteArray> = restTemplate.exchange(
        PATH_HENT_DOKUMENT_REFERANSE_TEMPLATE,
        HttpMethod.GET,
        HttpEntity.EMPTY,
        ByteArray::class.java,
        uriVariable(dokumentreferanse),
    )

    fun erFerdigstilt(dokumentreferanse: String): ResponseEntity<Boolean> = restTemplate.exchange(
        PATH_HENT_DOKUMENT_ER_FERDIGSTILT_TEMPLATE,
        HttpMethod.GET,
        HttpEntity.EMPTY,
        Boolean::class.java,
        uriVariable(dokumentreferanse),
    )

    private fun typereferansenErListeMedAvvikstyper(): ParameterizedTypeReference<List<AvvikType>> = object : ParameterizedTypeReference<List<AvvikType>>() {}

    private fun uriVariable(value: String?): String = value ?: "null"

    companion object {
        private const val PATH_JOURNAL = "/sak/%s/journal"
        private const val PATH_OPPRETT_JOURNALPOST = "/journalpost"
        const val PATH_JOURNALPOST_UTEN_SAK = "/journal/%s"
        const val PATH_SAK_JOURNAL = "/sak/%s/journal"
        private const val PATH_JOURNALPOST = "/journal/%s"
        private const val PATH_DISTRIBUER = "/journal/distribuer/%s"
        private const val PATH_DISTRIBUER_ENABLED = "/journal/distribuer/%s/enabled"
        private const val PATH_HENT_DIST_INFO = "/journal/distribuer/info/%s"
        private const val PATH_JOURNALPOST_MED_SAKPARAM = "/journal/%s?saksnummer=%s"
        private const val PARAM_FAGOMRADE = "fagomrade"
        private const val PARAM_BATCHID = "batchId"
        private const val PARAM_SAKSNUMMER = "saksnummer"
        const val PATH_AVVIK_PA_JOURNALPOST_MED_SAK_PARAM =
            "/journal/%s/avvik?" + PARAM_SAKSNUMMER + "=%s"
        const val PATH_AVVIK_PA_JOURNALPOST = "/journal/%s/avvik"
        const val PATH_HENT_DOKUMENT = "/dokument/%s"
        const val PATH_HENT_DOKUMENT_REFERANSE = "/dokumentreferanse/%s"
        const val PATH_HENT_DOKUMENT_ER_FERDIGSTILT = "/dokumentreferanse/%s/erFerdigstilt"
        private const val PATH_AVVIK_PA_JOURNALPOST_TEMPLATE = "/journal/{journalpostId}/avvik"
        private const val PATH_AVVIK_PA_JOURNALPOST_MED_SAK_PARAM_TEMPLATE = "/journal/{journalpostId}/avvik?saksnummer={saksnummer}"
        private const val PATH_JOURNALPOST_TEMPLATE = "/journal/{journalpostId}"
        private const val PATH_JOURNALPOST_MED_SAKPARAM_TEMPLATE = "/journal/{journalpostId}?saksnummer={saksnummer}"
        private const val PATH_DISTRIBUER_TEMPLATE = "/journal/distribuer/{journalpostId}"
        private const val PATH_DISTRIBUER_MED_BATCHID_TEMPLATE = "/journal/distribuer/{journalpostId}?batchId={batchId}"
        private const val PATH_DISTRIBUER_ENABLED_TEMPLATE = "/journal/distribuer/{journalpostId}/enabled"
        private const val PATH_HENT_DIST_INFO_TEMPLATE = "/journal/distribuer/info/{journalpostId}"
        private const val PATH_HENT_DOKUMENT_TEMPLATE = "/dokument/{journalpostId}"
        private const val PATH_HENT_DOKUMENT_MED_REFERANSE_TEMPLATE = "/dokument/{journalpostId}/{dokumentreferanse}"
        private const val PATH_HENT_DOKUMENT_REFERANSE_TEMPLATE = "/dokumentreferanse/{dokumentreferanse}"
        private const val PATH_HENT_DOKUMENT_ER_FERDIGSTILT_TEMPLATE = "/dokumentreferanse/{dokumentreferanse}/erFerdigstilt"

        private fun typereferansenErListeMedJournalposter(): ParameterizedTypeReference<List<JournalpostDto>> = object : ParameterizedTypeReference<List<JournalpostDto>>() {}

        @JvmStatic
        fun createEnhetHeader(enhet: String?): HttpHeaders {
            val header = HttpHeaders()
            header.add(EnhetFilter.X_ENHET_HEADER, enhet)
            return header
        }
    }
}
