package no.nav.bidrag.regnskap.consumer

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer

class PersonApiWireMock {

    companion object {
        private const val PORT = 8099
    }

    val nyIdent = genererFødselsnummer()

    private val mock = WireMockServer(PORT)

    init {
        mock.start()
    }

    internal fun personidentMedGyldigResponse() {
        mock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/personidenter")).willReturn(
                WireMock.aResponse().withHeader("Content-Type", "application/json").withStatus(200).withBody(
                    """
            [
                {
                    "ident": "$nyIdent",
                    "historisk": false,
                    "gruppe": "FOLKEREGISTERIDENT"
                }
            ]
               """,
                ),
            ),
        )
    }

    internal fun personidentMedNoBody() {
        mock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/personidenter")).willReturn(
                WireMock.aResponse().withHeader("Content-Type", "application/json").withStatus(204),
            ),
        )
    }
}
