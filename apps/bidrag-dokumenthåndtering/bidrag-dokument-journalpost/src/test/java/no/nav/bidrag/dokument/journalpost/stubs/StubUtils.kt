package no.nav.bidrag.dokument.journalpost.stubs

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import com.github.tomakehurst.wiremock.matching.RegexPattern
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import no.nav.bidrag.dokument.journalpost.dto.OpprettOppgaveResponse
import no.nav.bidrag.dokument.journalpost.dto.Saksbehandler
import no.nav.bidrag.dokument.journalpost.model.Enhet
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.PersonDto
import org.junit.Assert
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.util.Arrays

class StubUtils {
    fun stubHentPerson(
        fnr: String = ".*",
        personResponse: PersonDto =
            PersonDto(
                ident = Personident(fnr),
                aktørId = "",
                navn = "Etternavn, Fornavn Mellomnavn",
            ),
    ) {
        WireMock.stubFor(
            WireMock
                .post(
                    WireMock.urlMatching("/person/bidrag-person/informasjon"),
                ).withRequestBody(RegexPattern(".+$fnr.+"))
                .willReturn(
                    aClosedJsonResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody(convertObjectToString(personResponse)),
                ),
        )
    }

    fun stubHentEnhet(
        response: Enhet = Enhet("NAV Familie- og pensjonsytelser Drammen", "4806"),
        status: HttpStatus = HttpStatus.OK,
    ) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlMatching("/norg2/enhet/.*")).willReturn(
                aClosedJsonResponse()
                    .withStatus(status.value())
                    .withBody(convertObjectToString(response)),
            ),
        )
    }

    fun stubHentSaksbehandler(response: Saksbehandler = Saksbehandler("z123233", "Saksbehandler Navnesen")) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlMatching("/organisasjon/saksbehandler/info/.*")).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBody(convertObjectToString(response)),
            ),
        )
    }

    fun stubOpprettOppgave() {
        WireMock.stubFor(
            WireMock.post(WireMock.urlMatching("/oppgave")).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBody(convertObjectToString(OpprettOppgaveResponse(1L))),
            ),
        )
    }

    fun <T> convertObjectToString(o: T): String = try {
        ObjectMapper().findAndRegisterModules().writeValueAsString(o)
    } catch (e: JsonProcessingException) {
        Assert.fail(e.message)
        ""
    }

    inner class Verify {
        private fun verifyContains(
            verify: RequestPatternBuilder,
            vararg contains: String,
        ) {
            Arrays.stream(contains).forEach { verify.withRequestBody(ContainsPattern(it)) }
            WireMock.verify(verify)
        }

        fun verifyHentSaksbehandlerCalledWith(saksbehandlerIdent: String) {
            WireMock.getRequestedFor(WireMock.urlMatching("/organisasjon/saksbehandler/info/$saksbehandlerIdent"))
        }

        fun verifyHentEnhetCalledWith(enhet: String) {
            WireMock.getRequestedFor(WireMock.urlMatching("/norg2/enhet/$enhet"))
        }
    }

    companion object {
        fun aClosedJsonResponse(): ResponseDefinitionBuilder = aResponse()
            .withHeader(HttpHeaders.CONNECTION, "close")
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
    }
}
