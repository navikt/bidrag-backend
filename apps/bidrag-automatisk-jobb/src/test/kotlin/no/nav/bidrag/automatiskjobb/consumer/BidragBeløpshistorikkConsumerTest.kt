package no.nav.bidrag.automatiskjobb.consumer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentEngangsbeløpRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadHistoriskRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.LocalDateTime

class BidragBeløpshistorikkConsumerTest {
    private val restTemplate = RestTemplate()
    private val server = MockRestServiceServer.bindTo(restTemplate).build()
    private val consumer = BidragBeløpshistorikkConsumer(URI("http://belopshistorikk"), restTemplate)
    private val stønadRequest = HentStønadRequest(
        type = Stønadstype.BIDRAG,
        sak = Saksnummer("SAK-001"),
        skyldner = Personident("12345678901"),
        kravhaver = Personident("10987654321"),
    )

    @Test
    fun `returnerer null når stønad ikke finnes`() {
        forventStatus("hent-stonad/", HttpStatus.NOT_FOUND)

        consumer.hentLøpendeStønad(stønadRequest) shouldBe null
        server.verify()
    }

    @Test
    fun `returnerer null når historisk stønad ikke finnes`() {
        forventStatus("hent-stonad-historisk/", HttpStatus.NOT_FOUND)

        consumer.hentHistoriskeStønader(
            HentStønadHistoriskRequest(
                type = stønadRequest.type,
                sak = stønadRequest.sak,
                skyldner = stønadRequest.skyldner,
                kravhaver = stønadRequest.kravhaver,
                gyldigTidspunkt = LocalDateTime.parse("2020-12-31T23:00:00"),
            ),
        ) shouldBe null
        server.verify()
    }

    @Test
    fun `returnerer null når engangsbeløp ikke finnes`() {
        forventStatus("hent-engangsbelop", HttpStatus.NOT_FOUND)

        consumer.hentEngangsbeløp(
            HentEngangsbeløpRequest(
                type = Engangsbeløptype.SÆRBIDRAG,
                sak = stønadRequest.sak,
                skyldner = stønadRequest.skyldner,
                kravhaver = stønadRequest.kravhaver,
                referanse = "referanse",
            ),
        ) shouldBe null
        server.verify()
    }

    @Test
    fun `lar andre HTTP-feil propagere`() {
        forventStatus("hent-stonad/", HttpStatus.SERVICE_UNAVAILABLE)

        shouldThrow<HttpStatusCodeException> { consumer.hentLøpendeStønad(stønadRequest) }
        server.verify()
    }

    @Test
    fun `beholder tom liste for listeoppslag`() {
        server.expect(requestTo("http://belopshistorikk/engangsbelop/SAK-001"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON))

        consumer.hentEngangsbeløpForSak(stønadRequest.sak) shouldBe emptyList()
        server.verify()
    }

    private fun forventStatus(path: String, status: HttpStatus) {
        server.expect(requestTo("http://belopshistorikk/$path"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(status))
    }
}
