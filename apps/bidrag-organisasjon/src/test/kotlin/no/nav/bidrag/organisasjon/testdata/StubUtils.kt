package no.nav.bidrag.organisasjon.testdata

import com.fasterxml.jackson.core.JsonProcessingException
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.organisasjon.consumer.dto.EnhetInfoResponse
import no.nav.bidrag.organisasjon.consumer.dto.EnhetKontakinformasjonResponse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.junit.Assert
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class StubUtils {

    companion object {
        fun aClosedJsonResponse(): ResponseDefinitionBuilder = aResponse()
            .withHeader(HttpHeaders.CONNECTION, "close")
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
    }

    fun stubEnhetInfo(response: EnhetInfoResponse = EnhetInfoResponse(enhetNr = Enhetsnummer("4806"), navn = "Nav Drammen")) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlMatching("/norg2/enhet/${response.enhetNr}")).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBody(convertObjectToString(response)),
            ),
        )
    }

    fun stubEnhetKontaktinfo(response: EnhetKontakinformasjonResponse = createEnhetKontaktinfoResponse()) {
        WireMock.stubFor(
            WireMock.get(WireMock.urlMatching("/norg2/enhet/(.*)/kontaktinformasjon")).willReturn(
                aClosedJsonResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBody(convertObjectToString(response)),
            ),
        )
    }

    fun <T> convertObjectToString(o: T): String = try {
        commonObjectmapper.writeValueAsString(o)
    } catch (e: JsonProcessingException) {
        Assert.fail(e.message)
        ""
    }
}
