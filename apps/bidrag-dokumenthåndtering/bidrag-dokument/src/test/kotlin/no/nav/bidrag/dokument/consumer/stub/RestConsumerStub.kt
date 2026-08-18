package no.nav.bidrag.dokument.consumer.stub

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import no.nav.bidrag.dokument.consumer.BidragDokumentConsumer
import no.nav.bidrag.dokument.consumer.DokumentTilgangConsumer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.Int
import kotlin.Throws
import kotlin.text.StringBuilder

@Component
class RestConsumerStub {
    @Throws(IOException::class)
    fun runHenteJournalpostForSak(saksnr: String?) {
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathMatching(String.format(BidragDokumentConsumer.PATH_SAK_JOURNAL, saksnr)))
                .withQueryParam("fagomrade", WireMock.equalTo("BID"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(HttpStatus.OK.value())
                        .withBody(Files.readString(Path.of("src/test/resources/stubrespons/bdj-respons.json"), StandardCharsets.UTF_8)),
                ),
        )
    }

    fun runHenteJournalpostArkiv(
        jpId: String,
        queryParams: MutableMap<String, StringValuePattern>,
        status: HttpStatus,
        respons: String,
    ) {
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathMatching(String.format("/arkiv" + BidragDokumentConsumer.PATH_JOURNALPOST_UTEN_SAK, jpId)))
                .withQueryParams(queryParams)
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader(
                            "Content-Type",
                            MediaType.APPLICATION_JSON_VALUE,
                        ).withStatus(status.value())
                        .withBody(respons),
                ),
        )
    }

    fun runHenteJournalpost(
        jpId: String?,
        queryParams: MutableMap<String, StringValuePattern>,
        status: HttpStatus,
        respons: String?,
    ) {
        WireMock.stubFor(
            WireMock
                .get(
                    WireMock.urlPathMatching(String.format(BidragDokumentConsumer.PATH_JOURNALPOST_UTEN_SAK, jpId)),
                ).withQueryParams(queryParams)
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader(
                            "Content-Type",
                            MediaType.APPLICATION_JSON_VALUE,
                        ).withStatus(status.value())
                        .withBody(respons),
                ),
        )
    }

    @Throws(IOException::class)
    fun runEndreJournalpost(
        journalpostId: String?,
        status: HttpStatus,
    ) {
        WireMock.stubFor(
            WireMock
                .patch(
                    WireMock.urlPathMatching(String.format(BidragDokumentConsumer.PATH_JOURNALPOST_UTEN_SAK, journalpostId)),
                ).willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(status.value())
                        .withBody(Files.readString(Path.of("src/test/resources/stubrespons/bdj-respons.json"), StandardCharsets.UTF_8)),
                ),
        )
    }

    fun runEndreJournalpostMedHeader(
        journalpostId: String?,
        headerinput: HttpHeader,
        status: HttpStatus,
        respons: String,
    ) {
        val headers = HttpHeaders(headerinput, HttpHeader.httpHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE))
        WireMock.stubFor(
            WireMock
                .patch(WireMock.urlPathMatching(String.format(BidragDokumentConsumer.PATH_JOURNALPOST_UTEN_SAK, journalpostId)))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeaders(headers)
                        .withStatus(status.value())
                        .withBody(respons),
                ),
        )
    }

    fun runGiTilgangTilDokument(
        jpid: String?,
        dokref: String?,
        dokurl: String?,
        type: String?,
        status: Int,
    ) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching(String.format(DokumentTilgangConsumer.PATH_DOKUMENT_TILGANG, jpid, dokref))).willReturn(
                WireMock
                    .aResponse()
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withStatus(status)
                    .withBody(
                        listOf("\n", "{", "\"dokumentUrl\": \"" + dokurl + "\",", "\"type\": \"" + type + "\"", "}").joinToString(""),
                    ),
            ),
        )
    }

    fun runGetArkiv(
        path: kotlin.String?,
        queryParams: MutableMap<kotlin.String, StringValuePattern>,
        status: HttpStatus,
        respons: kotlin.String?,
    ) {
        runGet("/arkiv" + path, queryParams, status, respons)
    }

    fun runGetArkiv(
        path: kotlin.String?,
        status: HttpStatus,
        respons: kotlin.String?,
    ) {
        runGet("/arkiv" + path, status, respons)
    }

    fun runGetForsendelse(
        path: kotlin.String?,
        status: HttpStatus,
        respons: kotlin.String?,
    ) {
        runGet("/forsendelse" + path, status, respons)
    }

    fun runGet(
        path: kotlin.String?,
        queryParams: MutableMap<kotlin.String, StringValuePattern>,
        status: HttpStatus,
        respons: kotlin.String?,
    ) {
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo(path))
                .withQueryParams(queryParams)
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withHeader("Connection", "close")
                        .withStatus(status.value())
                        .withBody(respons),
                ),
        )
    }

    fun runGet(
        path: kotlin.String?,
        status: HttpStatus,
        respons: kotlin.String?,
    ) {
        WireMock.stubFor(
            WireMock
                .get(WireMock.urlPathEqualTo(path))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withHeader("Connection", "close")
                        .withStatus(status.value())
                        .withBody(respons),
                ),
        )
    }

    fun runPostArkiv(
        path: kotlin.String?,
        status: HttpStatus,
        respons: kotlin.String?,
    ) {
        runPost("/arkiv" + path, status, respons)
    }

    fun runPost(
        path: kotlin.String?,
        status: HttpStatus,
        respons: kotlin.String?,
    ) {
        WireMock.stubFor(
            WireMock
                .post(WireMock.urlPathEqualTo(path))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withHeader(
                            "Content-Type",
                            MediaType.APPLICATION_JSON_VALUE,
                        ).withStatus(status.value())
                        .withBody(respons),
                ),
        )
    }

    companion object {
        @Throws(IOException::class)
        fun lesResponsfilSomStreng(filnavn: kotlin.String?): kotlin.String = Files.readString(Path.of("src/test/resources/stubrespons/" + filnavn), StandardCharsets.UTF_8)

        fun generereJournalpostrespons(elementer: MutableMap<kotlin.String, kotlin.String>): kotlin.String {
            val startingElements = listOf("\n", " {", " \"journalpost\": {").joinToString("")

            val closingElements = listOf("\n", "}", "}").joinToString("")

            val respons = StringBuilder()
            respons.append(startingElements)

            var i = 0
            for (element in elementer.entries) {
                if (i > elementer.size - 1) {
                    respons.append(listOf("\n", " \"" + element.key + "\": \"" + element.value + "\",").joinToString(""))
                } else {
                    respons.append(listOf("\n", " \"" + element.key + "\": \"" + element.value + "\"").joinToString(""))
                }
                i++
            }
            respons.append(closingElements)
            return respons.toString()
        }
    }
}
