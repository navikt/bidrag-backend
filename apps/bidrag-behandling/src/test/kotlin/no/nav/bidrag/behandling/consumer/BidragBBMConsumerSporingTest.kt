package no.nav.bidrag.behandling.consumer

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknadResponse
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterBehandlerenhetRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterBehandlingsidRequest
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.LocalDate

class BidragBBMConsumerSporingTest {
    private lateinit var server: MockRestServiceServer
    private lateinit var consumer: BidragBBMConsumer

    private val settBehandlingsidUrl = "http://bbm/api/beregning/settbehandlingsid"
    private val oppdaterBehandlerenhetUrl = "http://bbm/api/beregning/oppdaterbehandlerenhet"

    @BeforeEach
    fun setUp() {
        val restTemplate = RestTemplate()
        server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build()
        consumer = spyk(BidragBBMConsumer(URI("http://bbm"), restTemplate, false))
    }

    @AfterEach
    fun tearDown() {
        consumer.stoppSporingAvEndringer()
    }

    private fun mockHentSøknad(
        søknadsid: Long,
        behandlerenhet: String?,
    ) {
        every { consumer.hentSøknad(søknadsid) } returns
            HentSøknadResponse(
                HentSøknad(
                    søknadsid = søknadsid,
                    søknadMottattDato = LocalDate.parse("2024-01-10"),
                    behandlingstema = Behandlingstema.BIDRAG,
                    behandlerenhet = behandlerenhet,
                    saksnummer = "123",
                    innkreving = true,
                    søktAvType = SøktAvType.BIDRAGSMOTTAKER,
                    behandlingStatusType = BehandlingStatusType.UNDER_BEHANDLING,
                ),
            )
    }

    @Test
    fun `skal spore vellykkede endringer av behandlingsid når sporing er startet`() {
        server.expect(ExpectedCount.times(2), requestTo(settBehandlingsidUrl)).andExpect(method(HttpMethod.POST)).andRespond(withSuccess())
        val endring1 = OppdaterBehandlingsidRequest(søknadsid = 1L, eksisterendeBehandlingsid = 10L, nyBehandlingsid = 20L)
        val endring2 = OppdaterBehandlingsidRequest(søknadsid = 2L, nyBehandlingsid = 20L)

        consumer.startSporingAvEndringer()
        consumer.lagreBehandlingsid(endring1)
        consumer.lagreBehandlingsid(endring2)
        val endringer = consumer.stoppSporingAvEndringer()

        server.verify()
        endringer.behandlingsid shouldContainExactly listOf(endring1, endring2)
    }

    @Test
    fun `skal ikke spore endring av behandlingsid som feilet`() {
        server.expect(requestTo(settBehandlingsidUrl)).andRespond(withStatus(HttpStatus.BAD_REQUEST))

        consumer.startSporingAvEndringer()
        consumer.lagreBehandlingsid(OppdaterBehandlingsidRequest(søknadsid = 1L, nyBehandlingsid = 20L))
        val endringer = consumer.stoppSporingAvEndringer()

        endringer.behandlingsid.shouldBeEmpty()
    }

    @Test
    fun `skal ikke spore endringer når sporing ikke er startet`() {
        server.expect(requestTo(settBehandlingsidUrl)).andRespond(withSuccess())
        server.expect(requestTo(oppdaterBehandlerenhetUrl)).andRespond(withSuccess())

        consumer.lagreBehandlingsid(OppdaterBehandlingsidRequest(søknadsid = 1L, nyBehandlingsid = 20L))
        consumer.lagreBehandlerEnhet(OppdaterBehandlerenhetRequest(1L, "4812"))
        val endringer = consumer.stoppSporingAvEndringer()

        server.verify()
        endringer.behandlingsid.shouldBeEmpty()
        endringer.behandlerenhet.shouldBeEmpty()
        verify(exactly = 0) { consumer.hentSøknad(any()) }
    }

    @Test
    fun `skal spore opprinnelig behandlerenhet kun ved første endring av søknaden`() {
        server.expect(ExpectedCount.times(3), requestTo(oppdaterBehandlerenhetUrl)).andRespond(withSuccess())
        mockHentSøknad(1L, "4806")
        mockHentSøknad(2L, null)

        consumer.startSporingAvEndringer()
        consumer.lagreBehandlerEnhet(OppdaterBehandlerenhetRequest(1L, "4812"))
        consumer.lagreBehandlerEnhet(OppdaterBehandlerenhetRequest(1L, "4817"))
        consumer.lagreBehandlerEnhet(OppdaterBehandlerenhetRequest(2L, "4812"))
        val endringer = consumer.stoppSporingAvEndringer()

        server.verify()
        endringer.behandlerenhet shouldBe linkedMapOf(1L to "4806", 2L to null)
        verify(exactly = 1) { consumer.hentSøknad(1L) }
        verify(exactly = 1) { consumer.hentSøknad(2L) }
    }

    @Test
    fun `stoppSporingAvEndringer skal nullstille sporing`() {
        server.expect(requestTo(settBehandlingsidUrl)).andRespond(withSuccess())

        consumer.startSporingAvEndringer()
        consumer.stoppSporingAvEndringer()
        consumer.lagreBehandlingsid(OppdaterBehandlingsidRequest(søknadsid = 1L, nyBehandlingsid = 20L))

        consumer.stoppSporingAvEndringer().behandlingsid.shouldBeEmpty()
    }
}
