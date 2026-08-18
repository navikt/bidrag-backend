package no.nav.bidrag.kodeverk.utils

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import no.nav.bidrag.commons.service.KodeverkKoderBetydningerResponse
import org.junit.Assert
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

@Component
class StubUtils {
    companion object {
        fun aClosedJsonResponse(): ResponseDefinitionBuilder = aResponse()
            .withHeader(HttpHeaders.CONNECTION, "close")
            .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
    }

    fun stubKodeverkResponse(response: KodeverkKoderBetydningerResponse) {
        try {
            WireMock.stubFor(
                WireMock.get(urlMatching("/kodeverk/(.*)")).willReturn(
                    aClosedJsonResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody(
                            JsonMapper
                                .builder()
                                .addModule(kotlinModule())
                                .build()
                                .writeValueAsString(response),
                        ),
                ),
            )
        } catch (e: Exception) {
            Assert.fail(e.message)
        }
    }
}
