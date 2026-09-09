package no.nav.bidrag.henvendelse.consumer

import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.ident.Personident
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.net.URI

/**
 * Vi ber bidrag-person om AKTORID uten historikk, men filtrerer likevel selv. Grunnen er at
 * `grupper` og `inkludereHistoriske` er noe den andre tjenesten avgjør hva den gjør med, og
 * en aktørid vi ikke skulle hatt gir oppslag på feil person i henvendelsesløsningen.
 */
class BidragPersonConsumerTest {
    private val restTemplate = RestTemplate()
    private val mockServer = MockRestServiceServer.bindTo(restTemplate).build()
    private val consumer = BidragPersonConsumer(URI.create("http://bidrag-person"), restTemplate)

    @Test
    fun `skal hente den gjeldende aktøriden`() {
        stub(
            """
            [
              { "ident": "$FNR", "historisk": false, "gruppe": "FOLKEREGISTERIDENT" },
              { "ident": "1000000000000", "historisk": true, "gruppe": "AKTORID" },
              { "ident": "$AKTØRID", "historisk": false, "gruppe": "AKTORID" }
            ]
            """.trimIndent(),
        )

        consumer.hentAktørid(Personident(FNR)) shouldBe AKTØRID
    }

    @Test
    fun `skal gi null når personen bare har historiske aktørider`() {
        stub("""[ { "ident": "1000000000000", "historisk": true, "gruppe": "AKTORID" } ]""")

        consumer.hentAktørid(Personident(FNR)) shouldBe null
    }

    @Test
    fun `skal gi null når personen ikke har noen identer`() {
        stub("[]")

        consumer.hentAktørid(Personident(FNR)) shouldBe null
    }

    private fun stub(respons: String) {
        mockServer
            .expect(requestTo("http://bidrag-person/personidenter"))
            .andRespond(withSuccess(respons, MediaType.APPLICATION_JSON))
    }

    companion object {
        private const val FNR = "17490123474"
        private const val AKTØRID = "2000012345678"
    }
}
