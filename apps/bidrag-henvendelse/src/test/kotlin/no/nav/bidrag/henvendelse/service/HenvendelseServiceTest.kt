package no.nav.bidrag.henvendelse.service

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.henvendelse.consumer.BidragPersonConsumer
import no.nav.bidrag.henvendelse.consumer.HenvendelseConsumer
import no.nav.bidrag.henvendelse.dto.Henvendelsestype
import org.hamcrest.CoreMatchers.startsWith
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.OffsetDateTime

/**
 * Enhetstester for mappingen. `X-Correlation-ID` og feilhåndtering dekkes i
 * [no.nav.bidrag.henvendelse.HenvendelseIntegrasjonTest], siden begge avhenger av
 * produksjonsoppsettet av RestTemplaten og Spring sin exception-resolver.
 */
class HenvendelseServiceTest {
    private val personident = Personident(SYNTETISK_FNR)

    private val bidragPersonConsumer = mockk<BidragPersonConsumer>()
    private val restTemplate = RestTemplate()
    private val mockServer = MockRestServiceServer.bindTo(restTemplate).build()
    private val service = HenvendelseService(
        bidragPersonConsumer,
        HenvendelseConsumer(URI.create(BASE_URL), restTemplate),
    )

    @Test
    fun `skal hente henvendelser og mappe til bidrag-dto`() {
        every { bidragPersonConsumer.hentAktørid(personident) } returns SYNTETISK_AKTØRID
        stubHenvendelseliste(
            """
            [
              {
                "henvendelseType": "SAMTALEREFERAT",
                "kjedeId": "a0J3N000004dUBJUA2",
                "gjeldendeTemagruppe": "FMLI",
                "gjeldendeTema": "BID",
                "meldinger": [
                  { "sendtDato": "2026-06-27T12:00:00.000Z" },
                  { "sendtDato": "2026-06-28T09:30:00.000Z" }
                ]
              }
            ]
            """.trimIndent(),
        )

        val henvendelser = service.hentHenvendelser(personident).henvendelser

        henvendelser shouldHaveSize 1
        with(henvendelser.first()) {
            kjedeId shouldBe "a0J3N000004dUBJUA2"
            henvendelsestype shouldBe Henvendelsestype.SAMTALEREFERAT
            tema shouldBe "BID"
            temagruppe shouldBe "FMLI"
            sisteMeldingSendt shouldBe OffsetDateTime.parse("2026-06-28T09:30:00Z")
        }
        mockServer.verify()
    }

    @Test
    fun `skal bruke seneste sendtDato uavhengig av rekkefølgen i meldingslista`() {
        every { bidragPersonConsumer.hentAktørid(personident) } returns SYNTETISK_AKTØRID
        stubHenvendelseliste(
            """
            [
              {
                "henvendelseType": "CHAT",
                "kjedeId": "a0J3N000004dUBJUA2",
                "meldinger": [
                  { "sendtDato": "2026-06-28T09:30:00.000Z" },
                  { "sendtDato": "2026-01-02T08:00:00.000Z" },
                  { "sendtDato": "2026-03-15T11:00:00.000Z" }
                ]
              }
            ]
            """.trimIndent(),
        )

        val henvendelse = service.hentHenvendelser(personident).henvendelser.single()

        henvendelse.sisteMeldingSendt shouldBe OffsetDateTime.parse("2026-06-28T09:30:00Z")
    }

    @Test
    fun `skal gi tom sisteMeldingSendt når kjeden ikke har meldinger`() {
        every { bidragPersonConsumer.hentAktørid(personident) } returns SYNTETISK_AKTØRID
        stubHenvendelseliste(
            """
            [ { "henvendelseType": "MELDINGSKJEDE", "kjedeId": "a0J3N000004dUBJUA2", "meldinger": [] } ]
            """.trimIndent(),
        )

        val henvendelse = service.hentHenvendelser(personident).henvendelser.single()

        henvendelse.henvendelsestype shouldBe Henvendelsestype.MELDINGSKJEDE
        henvendelse.sisteMeldingSendt shouldBe null
    }

    @Test
    fun `skal takle at svaret er pakket i en data-konvolutt`() {
        every { bidragPersonConsumer.hentAktørid(personident) } returns SYNTETISK_AKTØRID
        stubHenvendelseliste(
            """
            {
              "data": [ { "henvendelseType": "CHAT", "kjedeId": "a0J3N000004dUBJUA2", "meldinger": [] } ],
              "currentPage": 1,
              "pageSize": 100,
              "totalPages": 3,
              "hasNextPage": true
            }
            """.trimIndent(),
        )

        val henvendelse = service.hentHenvendelser(personident).henvendelser.single()

        henvendelse.kjedeId shouldBe "a0J3N000004dUBJUA2"
        henvendelse.henvendelsestype shouldBe Henvendelsestype.CHAT
    }

    @Test
    fun `skal returnere tom liste når personen ikke har henvendelser`() {
        every { bidragPersonConsumer.hentAktørid(personident) } returns SYNTETISK_AKTØRID
        stubHenvendelseliste("[]")

        service.hentHenvendelser(personident).henvendelser.shouldBeEmpty()

        mockServer.verify()
    }

    @Test
    fun `skal mappe ukjent henvendelsestype til UKJENT og hoppe over henvendelser uten kjedeId`() {
        every { bidragPersonConsumer.hentAktørid(personident) } returns SYNTETISK_AKTØRID
        stubHenvendelseliste(
            """
            [
              { "henvendelseType": "NOE_HELT_NYTT", "kjedeId": "a0J3N000004dUBJUA2", "meldinger": [] },
              { "henvendelseType": "CHAT", "meldinger": [] }
            ]
            """.trimIndent(),
        )

        val henvendelser = service.hentHenvendelser(personident).henvendelser

        henvendelser shouldHaveSize 1
        henvendelser.single().henvendelsestype shouldBe Henvendelsestype.UKJENT
    }

    @Test
    fun `skal returnere tom liste uten å kalle henvendelsestjenesten når personen mangler aktørid`() {
        every { bidragPersonConsumer.hentAktørid(personident) } returns null

        service.hentHenvendelser(personident).henvendelser.shouldBeEmpty()

        verify(exactly = 1) { bidragPersonConsumer.hentAktørid(personident) }
        // Ingen forventninger satt på mockServer - verify feiler om det likevel ble gjort et kall.
        mockServer.verify()
    }

    private fun stubHenvendelseliste(respons: String) {
        mockServer
            .expect(requestTo(startsWith("$BASE_URL/henvendelseinfo/henvendelseliste?")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("aktorid", SYNTETISK_AKTØRID))
            // Kilden har default pageSize 50; vi setter den selv.
            .andExpect(queryParam("pageSize", "100"))
            .andRespond(withSuccess(respons, MediaType.APPLICATION_JSON))
    }

    companion object {
        private const val BASE_URL = "http://sf-henvendelse"

        /** Syntetisk fødselsnummer - måned er lagt til 40, slik Dolly-identer er. */
        private const val SYNTETISK_FNR = "17490123474"
        private const val SYNTETISK_AKTØRID = "2000012345678"
    }
}
