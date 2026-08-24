import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import no.nav.bidrag.dokument.bestilling.consumer.dto.BehandlingDetaljerDtoV2
import no.nav.bidrag.dokument.bestilling.consumer.dto.EnhetInfo
import no.nav.bidrag.dokument.bestilling.consumer.dto.EnhetKontaktInfoDto
import no.nav.bidrag.dokument.bestilling.consumer.dto.SaksbehandlerInfoResponse
import no.nav.bidrag.dokument.bestilling.utils.SAKSBEHANDLER_IDENT
import no.nav.bidrag.dokument.bestilling.utils.SAKSBEHANDLER_NAVN
import no.nav.bidrag.dokument.bestilling.utils.createEnhetKontaktInformasjon
import no.nav.bidrag.dokument.bestilling.utils.createOpprettJournalpostResponse
import no.nav.bidrag.dokument.bestilling.utils.createPostAdresseResponse
import no.nav.bidrag.dokument.bestilling.utils.createSakResponse
import no.nav.bidrag.dokument.bestilling.utils.lagVedtaksdata
import no.nav.bidrag.dokument.bestilling.utils.opprettBehandlingDetaljer
import no.nav.bidrag.transport.dokument.OpprettJournalpostResponse
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.bidrag.transport.person.PersonAdresseDto
import no.nav.bidrag.transport.person.PersonDto
import no.nav.bidrag.transport.sak.BidragssakDto
import org.junit.Assert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.util.Arrays

@Component
class StubUtils {
    var objectMapper: ObjectMapper = commonObjectmapper

    companion object {
        fun aClosedJsonResponse(): ResponseDefinitionBuilder =
            aResponse()
                .withHeader(HttpHeaders.CONNECTION, "close")
                .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
    }

    fun stubHentPerson(
        fnr: String? = null,
        personResponse: PersonDto,
    ) {
        WireMock.stubFor(
            WireMock
                .post(WireMock.urlMatching("/person/informasjon"))
                .withRequestBody(if (fnr.isNullOrEmpty()) AnythingPattern() else ContainsPattern(fnr))
                .willReturn(
                    aClosedJsonResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody(convertObjectToString(personResponse)),
                ),
        )
    }

    fun stubHentPersonSpraak(result: String = "nb") {
        WireMock.stubFor(
            WireMock.post(WireMock.urlMatching("/person/spraak")).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
                    .withBody(result),
            ),
        )
    }

    fun stubHentVedtak() {
        WireMock.stubFor(
            WireMock.get(WireMock.urlMatching("/kodeverk/.*")).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBodyFile("testdata/api/landkoder.json"),
            ),
        )
    }

    fun stubHentAdresse(
        ident: String? = null,
        postAdresse: PersonAdresseDto? = createPostAdresseResponse(),
        status: HttpStatus = HttpStatus.OK,
    ) {
        WireMock.stubFor(
            WireMock
                .post(WireMock.urlMatching("/person/adresse/post"))
                .withRequestBody(
                    if (ident.isNullOrEmpty()) AnythingPattern() else ContainsPattern(ident),
                ).willReturn(
                    aClosedJsonResponse()
                        .withStatus(status.value())
                        .withBody(convertObjectToString(postAdresse)),
                ),
        )
    }

    fun stubHentBehandling(response: BehandlingDetaljerDtoV2 = opprettBehandlingDetaljer()) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlMatching("/behandling/.*")).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBody(convertObjectToString(response)),
            ),
        )
    }

    fun stubHentVedtak(vedtakFileName: String) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlMatching("/vedtak/.*")).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBody(convertObjectToString(lagVedtaksdata("vedtak/$vedtakFileName"))),
            ),
        )
    }

    fun stubHentSak(sak: BidragssakDto = createSakResponse()) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlMatching("/sak/.*")).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBody(convertObjectToString(sak)),
            ),
        )
    }

    fun stubOpprettJournalpost(
        response: OpprettJournalpostResponse = createOpprettJournalpostResponse(),
        status: HttpStatus = HttpStatus.OK,
    ) {
        WireMock.stubFor(
            WireMock.post(WireMock.urlMatching("/dokument/journalpost/BIDRAG")).willReturn(
                aClosedJsonResponse()
                    .withStatus(status.value())
                    .withBody(convertObjectToString(response)),
            ),
        )
    }

    fun stubEnhetInfo(response: EnhetInfo = EnhetInfo("4806", "Nav Drammen")) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlMatching("/organisasjon/enhet/info/.*")).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBody(convertObjectToString(response)),
            ),
        )
    }

    fun stubEnhetKontaktInfo(response: EnhetKontaktInfoDto = createEnhetKontaktInformasjon()) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlMatching("/organisasjon/enhet/kontaktinfo/.*")).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBody(convertObjectToString(response)),
            ),
        )
    }

    fun stubHentSaksbehandlerInfo(
        response: SaksbehandlerInfoResponse =
            SaksbehandlerInfoResponse(
                SAKSBEHANDLER_IDENT,
                SAKSBEHANDLER_NAVN,
            ),
    ) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlMatching("/organisasjon/saksbehandler/info/.*")).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBody(convertObjectToString(response)),
            ),
        )
    }

    inner class Verify {
        fun verifyHentPersonCalled(fnr: String?) {
            val verify =
                WireMock
                    .postRequestedFor(
                        WireMock.urlMatching("/person/informasjon"),
                    ).withRequestBody(ContainsPattern(fnr))
            WireMock.verify(verify)
        }

        fun verifyBehandlingKalt(
            vararg contains: String,
        ) {
            val verify =
                WireMock.getRequestedFor(
                    WireMock.urlMatching("/behandling/api/v2/behandling/detaljer/(.*)"),
                )
            verifyContains(verify, *contains)
        }

        fun verifyHentEnhetKontaktInfoCalledWith(
            spraak: String? = "NB",
            vararg contains: String,
        ) {
            val verify =
                WireMock.getRequestedFor(
                    WireMock.urlMatching("/organisasjon/enhet/kontaktinfo/.*/$spraak"),
                )
            verifyContains(verify, *contains)
        }

        fun verifyOpprettJournalpostCalledWith(vararg contains: String) {
            val verify =
                WireMock.postRequestedFor(
                    WireMock.urlMatching("/dokument/journalpost/BIDRAG"),
                )
            verifyContains(verify, *contains)
        }

        private fun verifyContains(
            verify: RequestPatternBuilder,
            vararg contains: String,
        ) {
            Arrays.stream(contains).forEach { verify.withRequestBody(ContainsPattern(it)) }
            WireMock.verify(verify)
        }
    }

    fun <T> convertObjectToString(o: T): String =
        try {
            objectMapper.writeValueAsString(o)
        } catch (e: JsonProcessingException) {
            Assert.fail(e.message)
            ""
        }
}
