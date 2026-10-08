package no.nav.bidrag.behandling.service.forholdsmessigfordeling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.bidrag.behandling.consumer.BidragBBMConsumer
import no.nav.bidrag.behandling.consumer.SporedeBBMEndringer
import no.nav.bidrag.behandling.database.repository.BehandlingRepository
import no.nav.bidrag.behandling.utils.testdata.opprettGyldigBehandlingForBeregningOgVedtak
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknaderForBehandlingResponse
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterBehandlerenhetRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterBehandlingsidRequest
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.interceptor.TransactionAspectSupport
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDate
import java.util.Optional

class ForholdsmessigFordelingServiceTilbakerullingTest {
    @MockK(relaxed = true)
    lateinit var bbmConsumer: BidragBBMConsumer

    @MockK(relaxed = true)
    lateinit var behandlingRepository: BehandlingRepository

    private val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
    private val behandlingId get() = behandling.id!!

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @AfterEach
    fun tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization()
        }
        unmockkStatic(TransactionAspectSupport::class)
    }

    private fun opprettService(transactionManager: PlatformTransactionManager? = null) = ForholdsmessigFordelingService(
        sakConsumer = mockk(),
        behandlingRepository = behandlingRepository,
        behandlingService = mockk(relaxed = true),
        beløpshistorikkConsumer = mockk(),
        grunnlagService = mockk(),
        bbmConsumer = bbmConsumer,
        forsendelseService = mockk(),
        beregningService = mockk(),
        virkningstidspunktService = mockk(),
        underholdService = mockk(),
        transactionManager = transactionManager,
    )

    private fun hentSøknad(
        søknadsid: Long,
        behandlingstype: Behandlingstype,
    ) = HentSøknad(
        søknadsid = søknadsid,
        søknadMottattDato = LocalDate.parse("2024-01-10"),
        søknadFomDato = LocalDate.parse("2024-01-01"),
        behandlingstema = Behandlingstema.BIDRAG,
        behandlingstype = behandlingstype,
        saksnummer = behandling.saksnummer,
        innkreving = true,
        søktAvType = SøktAvType.BIDRAGSMOTTAKER,
        behandlingStatusType = BehandlingStatusType.UNDER_BEHANDLING,
    )

    private fun mockFeiletOpprettelseMedSporedeEndringer(endringer: SporedeBBMEndringer): RuntimeException {
        val feil = RuntimeException("Feil ved opprettelse av FF")
        every { behandlingRepository.findBehandlingById(behandlingId) } throws feil andThen Optional.of(behandling)
        every { bbmConsumer.stoppSporingAvEndringer() } returns endringer andThen SporedeBBMEndringer()
        return feil
    }

    @Test
    fun `skal tilbakestille endringer i BBM og markere at opprettelse av FF feilet når opprettelse feiler`() {
        val endringer =
            SporedeBBMEndringer(
                behandlingsid =
                mutableListOf(
                    OppdaterBehandlingsidRequest(søknadsid = 1L, eksisterendeBehandlingsid = 10L, nyBehandlingsid = behandlingId),
                    OppdaterBehandlingsidRequest(søknadsid = 2L, eksisterendeBehandlingsid = null, nyBehandlingsid = behandlingId),
                    OppdaterBehandlingsidRequest(søknadsid = 3L, eksisterendeBehandlingsid = 30L, nyBehandlingsid = behandlingId),
                ),
                behandlerenhet = linkedMapOf(1L to "4806", 2L to null),
            )
        val feil = mockFeiletOpprettelseMedSporedeEndringer(endringer)

        val exception = shouldThrow<RuntimeException> { opprettService().opprettEllerOppdaterForholdsmessigFordeling(behandlingId) }

        exception shouldBe feil
        verifyOrder {
            bbmConsumer.startSporingAvEndringer()
            bbmConsumer.lagreBehandlingsid(OppdaterBehandlingsidRequest(3L, eksisterendeBehandlingsid = behandlingId, nyBehandlingsid = 30L))
            bbmConsumer.lagreBehandlingsid(OppdaterBehandlingsidRequest(1L, eksisterendeBehandlingsid = behandlingId, nyBehandlingsid = 10L))
            bbmConsumer.lagreBehandlerEnhet(OppdaterBehandlerenhetRequest(1L, "4806"))
            bbmConsumer.fjernSammeknytningHovedsøknad(behandling.soknadsid!!)
            behandlingRepository.markerOpprettelseAvFFFeilet(behandlingId)
        }
        verify(exactly = 2) { bbmConsumer.lagreBehandlingsid(any()) }
        verify(exactly = 0) { bbmConsumer.lagreBehandlingsid(match { it.søknadsid == 2L }) }
        verify(exactly = 1) { bbmConsumer.lagreBehandlerEnhet(any()) }
        verify(exactly = 2) { bbmConsumer.stoppSporingAvEndringer() }
    }

    @Test
    fun `skal fortsette tilbakerulling selv om tilbakestilling av behandlerenhet og sammenknytning feiler`() {
        val feil =
            mockFeiletOpprettelseMedSporedeEndringer(
                SporedeBBMEndringer(behandlerenhet = linkedMapOf(1L to "4806", 2L to "4812")),
            )
        every { bbmConsumer.lagreBehandlerEnhet(match { it.søknadsid == 1L }) } throws RuntimeException("BBM feilet")
        every { bbmConsumer.fjernSammeknytningHovedsøknad(any(), any()) } throws RuntimeException("BBM feilet")

        val exception = shouldThrow<RuntimeException> { opprettService().opprettEllerOppdaterForholdsmessigFordeling(behandlingId) }

        exception shouldBe feil
        verify(exactly = 1) { bbmConsumer.lagreBehandlerEnhet(OppdaterBehandlerenhetRequest(2L, "4812")) }
        verify(exactly = 1) { behandlingRepository.markerOpprettelseAvFFFeilet(behandlingId) }
    }

    @Test
    fun `skal markere rollback og lagre at opprettelse av FF feilet i ny transaksjon etter at transaksjonen er fullført`() {
        mockFeiletOpprettelseMedSporedeEndringer(SporedeBBMEndringer())
        val transactionManager = mockk<PlatformTransactionManager>(relaxed = true)
        val gjeldendeTransaksjon = mockk<TransactionStatus>(relaxed = true)
        mockkStatic(TransactionAspectSupport::class)
        every { TransactionAspectSupport.currentTransactionStatus() } returns gjeldendeTransaksjon
        TransactionSynchronizationManager.initSynchronization()

        shouldThrow<RuntimeException> { opprettService(transactionManager).opprettEllerOppdaterForholdsmessigFordeling(behandlingId) }

        verify(exactly = 1) { gjeldendeTransaksjon.setRollbackOnly() }
        verify(exactly = 0) { behandlingRepository.markerOpprettelseAvFFFeilet(any()) }

        val synkroniseringer = TransactionSynchronizationManager.getSynchronizations()
        synkroniseringer.size shouldBe 1
        synkroniseringer.first().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)

        verify(exactly = 1) { transactionManager.getTransaction(any()) }
        verify(exactly = 1) { behandlingRepository.markerOpprettelseAvFFFeilet(behandlingId) }
        verify(exactly = 1) { transactionManager.commit(any()) }
    }

    @Test
    fun `skal ikke feilregistrere FF-søknader når behandlingen ikke er slettet`() {
        every { behandlingRepository.erBehandlingSlettet(behandlingId) } returns false

        opprettService().feilregistrerNyeFFSøknader(behandlingId)

        verify(exactly = 0) { bbmConsumer.hentÅpneSøknaderForBehandling(any()) }
        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(any()) }
    }

    @Test
    fun `skal feilregistrere nye FF-søknader for slettet behandling og beholde eksisterende og andre søknader`() {
        every { behandlingRepository.erBehandlingSlettet(behandlingId) } returns true
        every { bbmConsumer.hentÅpneSøknaderForBehandling(behandlingId) } returns
            HentSøknaderForBehandlingResponse(
                listOf(
                    hentSøknad(1L, Behandlingstype.FORHOLDSMESSIG_FORDELING),
                    hentSøknad(2L, Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE),
                    hentSøknad(3L, Behandlingstype.FORHOLDSMESSIG_FORDELING),
                    hentSøknad(4L, Behandlingstype.KLAGE),
                ),
            )
        every { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == 1L }) } throws RuntimeException("BBM feilet")

        opprettService().feilregistrerNyeFFSøknader(behandlingId, eksisterendeFFSøknadsider = setOf(3L))

        verify(exactly = 1) { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == 1L }) }
        verify(exactly = 1) { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == 2L }) }
        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == 3L }) }
        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == 4L }) }
    }

    @Test
    fun `skal ikke kaste feil når henting av søknader for behandling feiler`() {
        every { behandlingRepository.erBehandlingSlettet(behandlingId) } returns null
        every { bbmConsumer.hentÅpneSøknaderForBehandling(behandlingId) } throws RuntimeException("BBM feilet")

        opprettService().feilregistrerNyeFFSøknader(behandlingId)

        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(any()) }
    }
}
