package no.nav.bidrag.dokument.arkivering.consumer.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.web.EnhetFilter
import no.nav.bidrag.dokument.arkivering.consumer.BidragDokumentConsumer
import no.nav.bidrag.dokument.arkivering.consumer.JournalpostApiConsumer
import no.nav.bidrag.dokument.arkivering.dto.JournalStatus
import no.nav.bidrag.dokument.arkivering.testutil.TestdataUtil.mockDokumentbehandlingConsumerRetur
import no.nav.bidrag.transport.dokument.JournalpostResponse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.util.Arrays

private val log = KotlinLogging.logger {}

@Component
class Stubs {
    private val objectMapper: ObjectMapper = commonObjectmapper

    // TODO: Ferdigstille stubbing av Soap-endepunkt
    fun runDokumentbehandlingStub() {
        val headers = HttpHeaders()
        headers.plus(
            HttpHeader(
                "Content-Type",
                "multipart/related; type=\"application/xop+xml\"; start-info=\"text/xml\"",
            ),
        )
        WireMock.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/brevweb/Dokumentbehandling"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeaders(headers)
                        .withStatus(200)
                        .withBodyFile("dokumentbehandling-soap-respons.xml"),
                ),
        )
    }

    fun runSecurityTokenServiceStub(mockedIdtoken: String) {
        WireMock.stubFor(
            WireMock
                .post(WireMock.urlPathMatching("/sts/.*"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(201)
                        .withBody(
                            java.lang.String.join(
                                "\n",
                                "{",
                                " \"access_token\": \"$mockedIdtoken\",",
                                " \"token_type\": \"Bearer\",",
                                " \"expires_in\": 5",
                                "}",
                            ),
                        ),
                ),
        )
    }

    fun runBidragDokumentHentJournalpostStub(
        jpResponse: JournalpostResponse,
        httpStatus: HttpStatus,
    ) {
        runBidragDokumentHentJournalpostStub(
            jpResponse.journalpost!!.journalpostId!!.replace(
                "BID-",
                "",
            ),
            jpResponse,
            httpStatus,
        )
    }

    fun runBidragSendAvvik(
        id: String?,
        httpStatus: HttpStatus,
    ) {
        WireMock.stubFor(
            WireMock
                .post(
                    WireMock.urlEqualTo(
                        String.format(
                            "/bidrag-dokument" + avvikEndpoint,
                            id,
                        ),
                    ),
                ).willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(httpStatus.value()),
                ),
        )
    }

    fun runKanArkivereJournalpostStub(
        id: String,
        httpStatus: HttpStatus,
    ) {
        try {
            WireMock.stubFor(
                WireMock
                    .get(
                        WireMock.urlEqualTo(
                            String.format(
                                "/bidrag-dokument" + kanArkivereJournalpostEndpoint,
                                "BID-$id",
                            ),
                        ),
                    ).willReturn(
                        WireMock
                            .aResponse()
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withHeader("Warning", "FEIL")
                            .withStatus(httpStatus.value()),
                    ),
            )
        } catch (e: Exception) {
            log.error(e) { "Det skjedde en feil ved stubbin av hent journalpost" }
        }
    }

    fun runHentDokumentStub(status: HttpStatus) {
        try {
            WireMock.stubFor(
                WireMock
                    .get(WireMock.urlPathMatching("/bidrag-dokument/dokument/.*"))
                    .willReturn(
                        WireMock
                            .aResponse()
                            .withHeader("Content-Type", MediaType.APPLICATION_PDF_VALUE)
                            .withStatus(status.value())
                            .withBody(mockDokumentbehandlingConsumerRetur().get()),
                    ),
            )
        } catch (e: Exception) {
            log.error(e) { "Det skjedde en feil ved stubbin av hent dokument" }
        }
    }

    fun runBidragDokumentHentJournalpostStub(
        id: String,
        jpResponse: JournalpostResponse?,
        httpStatus: HttpStatus,
    ) {
        try {
            WireMock.stubFor(
                WireMock
                    .get(
                        WireMock.urlEqualTo(
                            String.format(
                                "/bidrag-dokument" + hentJournalpostEndpoint,
                                "BID-$id",
                            ),
                        ),
                    ).willReturn(
                        WireMock
                            .aResponse()
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withStatus(httpStatus.value())
                            .withBody(objectMapper!!.writeValueAsString(jpResponse)),
                    ),
            )
        } catch (e: Exception) {
            log.error(e) { "Det skjedde en feil ved stubbin av hent journalpost" }
        }
    }

    fun runBidragDokumentEndreJournalpostStub(
        id: String,
        httpStatus: HttpStatus,
    ) {
        WireMock.stubFor(
            WireMock
                .patch(
                    WireMock.urlEqualTo(
                        String.format(
                            "/bidrag-dokument" + endreJournalpostEndpoint,
                            "JOARK-$id",
                        ),
                    ),
                ).willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(httpStatus.value()),
                ),
        )
    }

    fun runBidragDokumentHentJournalpostStub(
        id: String,
        httpStatus: HttpStatus,
    ) {
        runBidragDokumentHentJournalpostStub(id, null, httpStatus)
    }

    fun runArkiverJournalpostStub(
        httpStatus: HttpStatus,
        jpId: String,
        jpStatus: JournalStatus,
        melding: String,
        dokId: String,
        jpFerdigstilt: Boolean,
    ) {
        WireMock.stubFor(
            WireMock
                .post(
                    WireMock.urlEqualTo(arkiverJournalpostEndpoint + "?forsoekFerdigstill=true"),
                ).willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(httpStatus.value())
                        .withBody(
                            java.lang.String.join(
                                "\n",
                                "{",
                                " \"journalpostId\": \"$jpId\",",
                                " \"journalstatus\": \"" + jpStatus.kode + "\",",
                                " \"melding\": \"$melding\",",
                                " \"journalpostferdigstilt\": \"$jpFerdigstilt\",",
                                " \"dokumenter\": [",
                                "   {",
                                "      \"dokumentInfoId\": \"$dokId\"",
                                "    }",
                                "  ]",
                                " }",
                            ),
                        ),
                ),
        )
    }

    fun verify(): Assert = Assert()

    inner class Assert {
        fun avvikCalledWith(
            jpId: String?,
            header: String?,
            vararg contains: String,
        ) {
            val requestVerify =
                WireMock
                    .postRequestedFor(
                        WireMock.urlEqualTo(
                            String.format(
                                "/bidrag-dokument" + avvikEndpoint,
                                jpId,
                            ),
                        ),
                    ).withHeader(EnhetFilter.X_ENHET_HEADER, WireMock.equalTo(header))
            Arrays.stream(contains).forEach { contain: String? ->
                requestVerify.withRequestBody(
                    WireMock.containing(contain),
                )
            }
            WireMock.verify(requestVerify)
        }

        fun avvikNotCalledWith(
            jpId: String,
            header: String?,
            vararg contains: String,
        ) {
            val requestVerify =
                WireMock
                    .postRequestedFor(
                        WireMock.urlEqualTo(
                            String.format(
                                "/bidrag-dokument" + avvikEndpoint,
                                "BID-$jpId",
                            ),
                        ),
                    ).withHeader(EnhetFilter.X_ENHET_HEADER, WireMock.equalTo(header))
            Arrays.stream(contains).forEach { contain: String? ->
                requestVerify.withRequestBody(
                    WireMock.containing(contain),
                )
            }
            WireMock.verify(0, requestVerify)
        }

        fun endreJoarkJournalpostCalledWith(
            jpId: String,
            header: String?,
            vararg contains: String,
        ) {
            val requestVerify =
                WireMock
                    .patchRequestedFor(
                        WireMock.urlEqualTo(
                            String.format(
                                "/bidrag-dokument" + endreJournalpostEndpoint,
                                "JOARK-$jpId",
                            ),
                        ),
                    ).withHeader(EnhetFilter.X_ENHET_HEADER, WireMock.equalTo(header))
            Arrays.stream(contains).forEach { contain: String? ->
                requestVerify.withRequestBody(
                    WireMock.containing(contain),
                )
            }
            WireMock.verify(requestVerify)
        }

        fun arkiverJournalpostCalledWith(vararg contains: String) {
            val requestVerify =
                WireMock.postRequestedFor(
                    WireMock.urlEqualTo(
                        arkiverJournalpostEndpoint + "?forsoekFerdigstill=true",
                    ),
                )
            Arrays.stream(contains).forEach { contain: String? ->
                requestVerify.withRequestBody(
                    WireMock.containing(contain),
                )
            }
            WireMock.verify(requestVerify)
        }

        fun arkiverJournalpostNotCalled() {
            val requestVerify =
                WireMock.postRequestedFor(
                    WireMock.urlEqualTo(
                        arkiverJournalpostEndpoint + "?forsoekFerdigstill=true",
                    ),
                )
            WireMock.verify(0, requestVerify)
        }
    }

    companion object {
        private val arkiverJournalpostEndpoint = JournalpostApiConsumer.ARKIVER_JOURNALPOST_PATH
        private val hentJournalpostEndpoint = BidragDokumentConsumer.HENTE_JOURNALPOST_PATH
        private val kanArkivereJournalpostEndpoint =
            BidragDokumentConsumer.KAN_DISTRIBUERE_JOURNALPOST_PATH
        private val endreJournalpostEndpoint = BidragDokumentConsumer.ENDRE_JOURNALPOST_PATH
        val avvikEndpoint = BidragDokumentConsumer.AVVIK_PATH
    }
}
