package no.nav.bidrag.regnskap.consumer

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import wiremock.com.google.common.net.HttpHeaders

class ReskontroApiWireMock {

    companion object {
        private const val PORT = 8101
    }

    private val mock = WireMockServer(PORT)

    init {
        mock.start()
    }

    internal fun stop() {
        mock.stop()
    }

    internal fun reskontroIngenResponse() {
        mock.stubFor(
            WireMock.post(WireMock.anyUrl()).willReturn(
                WireMock.aResponse().withHeader(HttpHeaders.CONTENT_TYPE, "application/json").withStatus(200).withBody(
                    """
             "transaksjoner": []
        """,
                ),
            ),
        )
    }

    internal fun endreRmForSakMedGyldigResponse() {
        mock.stubFor(
            WireMock.patch(WireMock.urlPathEqualTo("/endreRmForSak"))
                .willReturn(WireMock.aResponse().withStatus(200)),
        )
    }

    internal fun nullstillForespørsler() {
        mock.resetRequests()
    }

    internal fun verifiserEndreRmForSak(saksnummer: String, barn: String, nyMottaker: String) {
        mock.verify(
            1,
            WireMock.patchRequestedFor(WireMock.urlPathEqualTo("/endreRmForSak"))
                .withRequestBody(
                    WireMock.equalToJson(
                        """{"saksnummer":"$saksnummer","barn":"$barn","nyttFødselsnummer":"$nyMottaker"}""",
                    ),
                ),
        )
    }
}
