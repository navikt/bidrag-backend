package no.nav.bidrag.behandling.service.forholdsmessigfordeling

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.behandling.consumer.BidragBBMConsumer
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.Rolle
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordeling
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordelingRolle
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordelingSøknadBarn
import no.nav.bidrag.behandling.database.datamodell.json.Omgjøringsdetaljer
import no.nav.bidrag.behandling.service.BehandlingService
import no.nav.bidrag.behandling.utils.testdata.opprettGyldigBehandlingForBeregningOgVedtak
import no.nav.bidrag.behandling.utils.testdata.testdataBarn1
import no.nav.bidrag.behandling.utils.testdata.testdataBarn2
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknadResponse
import no.nav.bidrag.transport.behandling.beregning.felles.OpprettSøknadResponse
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknaderForBehandlingResponse
import no.nav.bidrag.transport.behandling.beregning.felles.OpprettSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.PartISøknad
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import no.nav.bidrag.transport.søknad.FinnSammenknytningerHovedsøknadResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ForholdsmessigFordelingKlageServiceTest {
    @MockK(relaxed = true)
    lateinit var bbmConsumer: BidragBBMConsumer

    @MockK(relaxed = true)
    lateinit var behandlingService: BehandlingService

    @MockK
    lateinit var kravhaverService: ForholdsmessigFordelingKravhaverService

    private lateinit var service: ForholdsmessigFordelingKlageService

    private lateinit var behandling: Behandling

    companion object {
        private const val HOVEDSØKNADSID = 1000L
        private const val OPPRETTET_SØKNADSID = 2000L
        private const val FF_KLAGESØKNADSID = 3000L
        private const val PÅKLAGET_FF_SØKNADSID = 4000L
        private const val GJENOPPRETTET_FF_KLAGESØKNADSID = 5000L
        private const val OMGJØR_VEDTAKSID = 123
    }

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        every { bbmConsumer.hentÅpneSøknaderForBehandling(any()) } returns HentSøknaderForBehandlingResponse(emptyList())
        every { behandlingService.behandlingFinnes(any()) } returns true
        every { kravhaverService.hentAlleRelevanteKravhavere(any()) } returns emptySet()
        every { kravhaverService.finnEnhetForBarnIBehandling(any(), any()) } returns "4806"
        every { bbmConsumer.finnSammenknytningerHovedsøknad(PÅKLAGET_FF_SØKNADSID, any()) } returns
            FinnSammenknytningerHovedsøknadResponse(søknader = emptyList())
        val søknadService = ForholdsmessigFordelingSøknadService(bbmConsumer, mockk(), mockk(), kravhaverService, mockk())
        service =
            ForholdsmessigFordelingKlageService(
                bbmConsumer = bbmConsumer,
                behandlingService = behandlingService,
                grunnlagService = mockk(),
                underholdService = mockk(),
                virkningstidspunktService = mockk(),
                kravhaverService = kravhaverService,
                søknadService = søknadService,
                overføringService = mockk(),
            )
        behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG, andreBarn = listOf(testdataBarn2))
        behandling.soknadsid = HOVEDSØKNADSID
        behandling.omgjøringsdetaljer = Omgjøringsdetaljer(soknadRefId = PÅKLAGET_FF_SØKNADSID, omgjørVedtakId = OMGJØR_VEDTAKSID)
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(erHovedbehandling = true)
        behandling.roller.forEach {
            if (it.rolletype == Rolletype.BARN) it.stønadstype = Stønadstype.BIDRAG
            it.forholdsmessigFordeling =
                ForholdsmessigFordelingRolle(
                    tilhørerSak = behandling.saksnummer,
                    behandlerenhet = behandling.behandlerEnhet,
                    delAvOpprinneligBehandling = true,
                    erRevurdering = false,
                    bidragsmottaker = behandling.bidragsmottaker?.ident,
                )
        }
    }

    private val barn1 get() = behandling.søknadsbarn.find { it.ident == testdataBarn1.ident }!!
    private val barn2 get() = behandling.søknadsbarn.find { it.ident == testdataBarn2.ident }!!

    private fun søknad(
        søknadsid: Long,
        behandlingstype: Behandlingstype = Behandlingstype.KLAGE,
        opprettetEtterHovedsøknad: Boolean = false,
        erstatterFFKlagesøknadsid: Long? = null,
        omgjørSøknadsid: Long? = null,
    ) = ForholdsmessigFordelingSøknadBarn(
        søknadsid = søknadsid,
        mottattDato = LocalDate.parse("2024-01-10"),
        søknadFomDato = LocalDate.parse("2024-01-01"),
        søktAvType = SøktAvType.NAV_BIDRAG,
        behandlingstype = behandlingstype,
        behandlingstema = Behandlingstema.BIDRAG,
        omgjørSøknadsid = omgjørSøknadsid,
        omgjørVedtaksid = OMGJØR_VEDTAKSID,
        innkreving = true,
        status = Behandlingstatus.UNDER_BEHANDLING,
        saksnummer = behandling.saksnummer,
        enhet = "4806",
        opprettetEtterHovedsøknad = opprettetEtterHovedsøknad,
        erstatterFFKlagesøknadsid = erstatterFFKlagesøknadsid,
    )

    private fun Rolle.leggTilSøknad(søknad: ForholdsmessigFordelingSøknadBarn) {
        forholdsmessigFordeling!!.søknader.add(søknad)
    }

    private fun leggTilSøknadForRoller(
        søknad: ForholdsmessigFordelingSøknadBarn,
        vararg barn: Rolle,
    ) {
        barn.forEach { it.leggTilSøknad(søknad.copy()) }
        behandling.bidragsmottaker!!.leggTilSøknad(søknad.copy())
        behandling.bidragspliktig!!.leggTilSøknad(søknad.copy())
    }

    private fun leggTilFFKlagesøknad(vararg barn: Rolle) = leggTilSøknadForRoller(
        søknad(FF_KLAGESØKNADSID, Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE, omgjørSøknadsid = PÅKLAGET_FF_SØKNADSID),
        *barn,
    )

    private fun hentSøknad(
        søknadsid: Long,
        barn: List<String>,
        behandlingstype: Behandlingstype = Behandlingstype.KLAGE,
        refSøknadsid: Long? = null,
        status: BehandlingStatusType = BehandlingStatusType.UNDER_BEHANDLING,
    ) = HentSøknad(
        søknadsid = søknadsid,
        søknadMottattDato = LocalDate.parse("2024-01-10"),
        søknadFomDato = LocalDate.parse("2024-01-01"),
        behandlingstema = Behandlingstema.BIDRAG,
        behandlingstype = behandlingstype,
        saksnummer = behandling.saksnummer,
        innkreving = true,
        søktAvType = SøktAvType.BIDRAGSMOTTAKER,
        refSøknadsid = refSøknadsid,
        behandlingStatusType = status,
        partISøknadListe =
        barn.map { PartISøknad(personident = it, rolletype = Rolletype.BARN, behandlingstatus = Behandlingstatus.UNDER_BEHANDLING) },
    )

    private fun mockTilknyttedeSøknader(vararg søknader: HentSøknad) {
        every { bbmConsumer.finnSammenknytningerHovedsøknad(HOVEDSØKNADSID, any()) } returns
            FinnSammenknytningerHovedsøknadResponse(søknader = søknader.toList())
        every { bbmConsumer.hentÅpneSøknaderForBehandling(behandling.id!!) } returns HentSøknaderForBehandlingResponse(søknader.toList())
    }

    private fun Rolle.søknadStatus(søknadsid: Long) = forholdsmessigFordeling!!.søknader.find { it.søknadsid == søknadsid }?.status

    @Test
    fun `skal feilregistrere FF-klagesøknad når det opprettes ny søknad for barnet etter hovedsøknad`() {
        leggTilFFKlagesøknad(barn1)
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))

        mockTilknyttedeSøknader(
            hentSøknad(FF_KLAGESØKNADSID, listOf(barn1.ident!!), behandlingstype = Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE),
            hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!)),
        )
        service.feilregistrerFFKlagesøknaderErstattetAvOpprettetSøknad(behandling)

        verify(exactly = 1) { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == FF_KLAGESØKNADSID }) }
        verify(exactly = 1) { bbmConsumer.fjernSammenknytning(FF_KLAGESØKNADSID) }
        verify(exactly = 0) { bbmConsumer.feilregistrerSøknadsbarn(any()) }
        barn1.søknadStatus(FF_KLAGESØKNADSID) shouldBe Behandlingstatus.FEILREGISTRERT
        behandling.bidragsmottaker!!.søknadStatus(FF_KLAGESØKNADSID) shouldBe Behandlingstatus.FEILREGISTRERT
        behandling.bidragspliktig!!.søknadStatus(FF_KLAGESØKNADSID) shouldBe Behandlingstatus.FEILREGISTRERT
        barn1.søknadStatus(OPPRETTET_SØKNADSID) shouldBe Behandlingstatus.UNDER_BEHANDLING
        barn1.finnSøknad(OPPRETTET_SØKNADSID)!!.erstatterFFKlagesøknadsid shouldBe FF_KLAGESØKNADSID
    }

    @Test
    fun `skal kun feilregistrere barnet fra FF-klagesøknad når andre barn fortsatt er i FF-klagesøknaden`() {
        leggTilFFKlagesøknad(barn1, barn2)
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))

        mockTilknyttedeSøknader(
            hentSøknad(FF_KLAGESØKNADSID, listOf(barn1.ident!!, barn2.ident!!), behandlingstype = Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE),
            hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!)),
        )
        service.feilregistrerFFKlagesøknaderErstattetAvOpprettetSøknad(behandling)

        verify(exactly = 1) {
            bbmConsumer.feilregistrerSøknadsbarn(match { it.søknadsid == FF_KLAGESØKNADSID && it.personidentBarn == barn1.ident })
        }
        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(any()) }
        barn1.søknadStatus(FF_KLAGESØKNADSID) shouldBe Behandlingstatus.FEILREGISTRERT
        barn2.søknadStatus(FF_KLAGESØKNADSID) shouldBe Behandlingstatus.UNDER_BEHANDLING
        behandling.bidragsmottaker!!.søknadStatus(FF_KLAGESØKNADSID) shouldBe Behandlingstatus.UNDER_BEHANDLING
        barn1.finnSøknad(OPPRETTET_SØKNADSID)!!.erstatterFFKlagesøknadsid shouldBe FF_KLAGESØKNADSID
    }

    @Test
    fun `skal ikke feilregistrere FF-klagesøknad når opprettet søknad er hovedsøknad`() {
        leggTilFFKlagesøknad(barn1)
        barn1.leggTilSøknad(søknad(HOVEDSØKNADSID, opprettetEtterHovedsøknad = true))

        mockTilknyttedeSøknader(
            hentSøknad(FF_KLAGESØKNADSID, listOf(barn1.ident!!), behandlingstype = Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE),
            hentSøknad(HOVEDSØKNADSID, listOf(barn1.ident!!)),
        )
        service.feilregistrerFFKlagesøknaderErstattetAvOpprettetSøknad(behandling)

        verify(exactly = 1) { bbmConsumer.feilregistrerSøknad(any()) }
        barn1.søknadStatus(FF_KLAGESØKNADSID) shouldBe Behandlingstatus.FEILREGISTRERT
    }

    @Test
    fun `skal ikke feilregistrere FF-klagesøknad når tilknyttet søknad selv er FF-søknad`() {
        leggTilFFKlagesøknad(barn1)
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))

        mockTilknyttedeSøknader(
            hentSøknad(FF_KLAGESØKNADSID, listOf(barn1.ident!!), behandlingstype = Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE),
            hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!), behandlingstype = Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE),
        )
        service.feilregistrerFFKlagesøknaderErstattetAvOpprettetSøknad(behandling)

        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(any()) }
        verify(exactly = 0) { bbmConsumer.feilregistrerSøknadsbarn(any()) }
        barn1.finnSøknad(OPPRETTET_SØKNADSID)!!.erstatterFFKlagesøknadsid.shouldBeNull()
    }

    @Test
    fun `skal ikke feilregistrere FF-klagesøknad for barn som ikke er del av opprettet søknad`() {
        leggTilFFKlagesøknad(barn2)
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))

        mockTilknyttedeSøknader(
            hentSøknad(FF_KLAGESØKNADSID, listOf(barn2.ident!!), behandlingstype = Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE),
            hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!)),
        )
        service.feilregistrerFFKlagesøknaderErstattetAvOpprettetSøknad(behandling)

        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(any()) }
        verify(exactly = 0) { bbmConsumer.feilregistrerSøknadsbarn(any()) }
        barn2.søknadStatus(FF_KLAGESØKNADSID) shouldBe Behandlingstatus.UNDER_BEHANDLING
    }

    @Test
    fun `skal opprette revurderingssøknad for gjenværende relevante kravhavere når søknad som erstattet FF-søknad uten påklaget søknad slettes`() {
        val søknadServiceMock = mockk<ForholdsmessigFordelingSøknadService>(relaxed = true)
        val service =
            ForholdsmessigFordelingKlageService(
                bbmConsumer = bbmConsumer,
                behandlingService = behandlingService,
                grunnlagService = mockk(),
                underholdService = mockk(),
                virkningstidspunktService = mockk(),
                kravhaverService = kravhaverService,
                søknadService = søknadServiceMock,
                overføringService = mockk(),
            )
        behandling.omgjøringsdetaljer = Omgjøringsdetaljer(soknadRefId = PÅKLAGET_FF_SØKNADSID, omgjørVedtakId = OMGJØR_VEDTAKSID)
        listOf(barn1, barn2).forEach { barn ->
            leggTilSøknadForRoller(
                søknad(FF_KLAGESØKNADSID, Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE).copy(status = Behandlingstatus.FEILREGISTRERT),
                barn,
            )
            barn.leggTilSøknad(
                søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true, erstatterFFKlagesøknadsid = FF_KLAGESØKNADSID),
            )
        }
        every { kravhaverService.hentAlleRelevanteKravhavere(behandling) } returns
            setOf(SakKravhaver(behandling.saksnummer, barn1.ident!!, stønadstype = Stønadstype.BIDRAG))
        every { bbmConsumer.finnSammenknytningerHovedsøknad(PÅKLAGET_FF_SØKNADSID, any()) } returns
            FinnSammenknytningerHovedsøknadResponse(søknader = emptyList())

        service.slettEllerGjennopprettKlageSøknader(behandling, OPPRETTET_SØKNADSID)

        verify(exactly = 0) { bbmConsumer.opprettSøknader(any()) }
        verify(exactly = 1) {
            søknadServiceMock.opprettRollerOgRevurderingssøknadForSak(
                behandling,
                behandling.saksnummer,
                match { kravhavere -> kravhavere.map { it.kravhaver } == listOf(barn1.ident) },
                behandling.behandlerEnhet,
                Stønadstype.BIDRAG,
                any(),
                true,
                any(),
            )
        }
    }

    private fun serviceMedMocketSøknadService(): Pair<ForholdsmessigFordelingKlageService, ForholdsmessigFordelingSøknadService> {
        val søknadServiceMock = mockk<ForholdsmessigFordelingSøknadService>(relaxed = true)
        return ForholdsmessigFordelingKlageService(
            bbmConsumer = bbmConsumer,
            behandlingService = behandlingService,
            grunnlagService = mockk(),
            underholdService = mockk(),
            virkningstidspunktService = mockk(),
            kravhaverService = kravhaverService,
            søknadService = søknadServiceMock,
            overføringService = mockk(),
        ) to søknadServiceMock
    }

    private fun relevanteKravhavere(vararg barn: Rolle) {
        every { kravhaverService.hentAlleRelevanteKravhavere(behandling) } returns
            barn.map { SakKravhaver(behandling.saksnummer, it.ident!!, stønadstype = Stønadstype.BIDRAG) }.toSet()
    }

    private fun ForholdsmessigFordelingSøknadService.verifiserRevurderingssøknadOpprettetFor(vararg barn: Rolle) {
        verify(exactly = 1) {
            opprettRollerOgRevurderingssøknadForSak(
                behandling,
                behandling.saksnummer,
                match { kravhavere -> kravhavere.map { it.kravhaver } == barn.map { it.ident } },
                any(),
                Stønadstype.BIDRAG,
                any(),
                true,
                any(),
            )
        }
    }

    private fun ForholdsmessigFordelingSøknadService.verifiserIngenRevurderingssøknadOpprettet() {
        verify(exactly = 0) { opprettRollerOgRevurderingssøknadForSak(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `skal opprette revurderingssøknad for barn som ikke lenger har åpen søknad når søknad som erstattet FF-klagesøknad slettes`() {
        val (service, søknadServiceMock) = serviceMedMocketSøknadService()
        leggTilFFKlagesøknad(barn1)
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true, erstatterFFKlagesøknadsid = FF_KLAGESØKNADSID))
        every { bbmConsumer.hentSøknad(OPPRETTET_SØKNADSID) } returns
            mockk { every { søknad } returns hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!), refSøknadsid = HOVEDSØKNADSID) }
        relevanteKravhavere(barn1)
        mockTilknyttedeSøknader(hentSøknad(HOVEDSØKNADSID, listOf(barn2.ident!!)))

        service.slettEllerGjennopprettKlageSøknader(behandling, OPPRETTET_SØKNADSID)

        verify(exactly = 0) { bbmConsumer.opprettSøknader(any()) }
        søknadServiceMock.verifiserRevurderingssøknadOpprettetFor(barn1)
        barn1.søknadStatus(OPPRETTET_SØKNADSID) shouldBe Behandlingstatus.FEILREGISTRERT
    }

    @Test
    fun `skal ikke opprette revurderingssøknad når barnet fortsatt har en annen åpen søknad`() {
        val (service, søknadServiceMock) = serviceMedMocketSøknadService()
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true, erstatterFFKlagesøknadsid = FF_KLAGESØKNADSID))
        barn1.leggTilSøknad(søknad(GJENOPPRETTET_FF_KLAGESØKNADSID, opprettetEtterHovedsøknad = true))
        every { bbmConsumer.hentSøknad(OPPRETTET_SØKNADSID) } returns
            mockk { every { søknad } returns hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!), refSøknadsid = HOVEDSØKNADSID) }
        relevanteKravhavere(barn1)
        mockTilknyttedeSøknader(hentSøknad(GJENOPPRETTET_FF_KLAGESØKNADSID, listOf(barn1.ident!!)))

        service.slettEllerGjennopprettKlageSøknader(behandling, OPPRETTET_SØKNADSID)

        verify(exactly = 0) { bbmConsumer.opprettSøknader(any()) }
        søknadServiceMock.verifiserIngenRevurderingssøknadOpprettet()
        barn1.søknadStatus(OPPRETTET_SØKNADSID) shouldBe Behandlingstatus.FEILREGISTRERT
    }

    @Test
    fun `skal opprette revurderingssøknad for barn i slettet hovedsøknad når søknad opprettet etter hovedsøknad blir ny hovedsøknad`() {
        val (service, søknadServiceMock) = serviceMedMocketSøknadService()
        leggTilSøknadForRoller(søknad(HOVEDSØKNADSID, opprettetEtterHovedsøknad = true), barn1)
        barn2.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))
        every { bbmConsumer.finnSammenknytningerHovedsøknad(HOVEDSØKNADSID, any()) } returns
            FinnSammenknytningerHovedsøknadResponse(
                søknader = listOf(hentSøknad(HOVEDSØKNADSID, listOf(barn1.ident!!)), hentSøknad(OPPRETTET_SØKNADSID, listOf(barn2.ident!!), refSøknadsid = 9999L)),
            )
        every { bbmConsumer.finnSammenknytningerHovedsøknad(OPPRETTET_SØKNADSID, any()) } returns
            FinnSammenknytningerHovedsøknadResponse(søknader = listOf(hentSøknad(OPPRETTET_SØKNADSID, listOf(barn2.ident!!))))
        relevanteKravhavere(barn1, barn2)

        service.slettEllerGjennopprettKlageSøknader(behandling, HOVEDSØKNADSID)

        behandling.soknadsid shouldBe OPPRETTET_SØKNADSID
        verify(exactly = 0) { bbmConsumer.opprettSøknader(any()) }
        søknadServiceMock.verifiserRevurderingssøknadOpprettetFor(barn1)
        barn1.søknadStatus(HOVEDSØKNADSID) shouldBe Behandlingstatus.FEILREGISTRERT
    }

    @Test
    fun `synkronisering skal opprette revurderingssøknad for relevant kravhaver uten åpen søknad`() {
        val (service, søknadServiceMock) = serviceMedMocketSøknadService()
        barn1.leggTilSøknad(
            søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true, erstatterFFKlagesøknadsid = FF_KLAGESØKNADSID)
                .copy(status = Behandlingstatus.FEILREGISTRERT),
        )
        relevanteKravhavere(barn1)
        mockTilknyttedeSøknader()

        service.korrigerFFKlagesøknaderForSøknaderOpprettetEtterHovedsøknad(behandling)

        søknadServiceMock.verifiserRevurderingssøknadOpprettetFor(barn1)
        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(any()) }
    }

    @Test
    fun `skal ikke gjenopprette FF-klagesøknad når slettet søknad ikke erstattet en FF-klagesøknad`() {
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))
        every { bbmConsumer.hentSøknad(OPPRETTET_SØKNADSID) } returns
            mockk { every { søknad } returns hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!), refSøknadsid = HOVEDSØKNADSID) }

        service.slettEllerGjennopprettKlageSøknader(behandling, OPPRETTET_SØKNADSID)

        verify(exactly = 0) { bbmConsumer.opprettSøknader(any()) }
        barn1.søknadStatus(OPPRETTET_SØKNADSID) shouldBe Behandlingstatus.FEILREGISTRERT
    }

    @Test
    fun `skal gjøre søknad opprettet etter hovedsøknad til ny hovedsøknad når hovedsøknad slettes`() {
        leggTilSøknadForRoller(søknad(HOVEDSØKNADSID, opprettetEtterHovedsøknad = true), barn2)
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))
        every { bbmConsumer.finnSammenknytningerHovedsøknad(HOVEDSØKNADSID, any()) } returns
            FinnSammenknytningerHovedsøknadResponse(
                søknader =
                listOf(
                    hentSøknad(HOVEDSØKNADSID, listOf(barn2.ident!!)),
                    hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!), refSøknadsid = 9999L),
                ),
            )

        service.slettEllerGjennopprettKlageSøknader(behandling, HOVEDSØKNADSID)

        behandling.soknadsid shouldBe OPPRETTET_SØKNADSID
        verify(exactly = 1) { bbmConsumer.fjernSammeknytningHovedsøknad(HOVEDSØKNADSID, OPPRETTET_SØKNADSID) }
        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(any()) }
        verify(exactly = 0) { behandlingService.logiskSlettBehandling(any()) }
        barn2.søknadStatus(HOVEDSØKNADSID) shouldBe Behandlingstatus.FEILREGISTRERT
        barn1.søknadStatus(OPPRETTET_SØKNADSID) shouldBe Behandlingstatus.UNDER_BEHANDLING
    }

    @Test
    fun `skal ikke gjøre søknad til hovedsøknad hvis den ikke er opprettet etter hovedsøknad`() {
        leggTilSøknadForRoller(søknad(HOVEDSØKNADSID, opprettetEtterHovedsøknad = true), barn2)
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = false))
        every { bbmConsumer.finnSammenknytningerHovedsøknad(HOVEDSØKNADSID, any()) } returns
            FinnSammenknytningerHovedsøknadResponse(
                søknader =
                listOf(
                    hentSøknad(HOVEDSØKNADSID, listOf(barn2.ident!!)),
                    hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!), refSøknadsid = 9999L),
                ),
            )

        service.slettEllerGjennopprettKlageSøknader(behandling, HOVEDSØKNADSID)

        behandling.soknadsid shouldBe HOVEDSØKNADSID
        verify(exactly = 1) { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == OPPRETTET_SØKNADSID }) }
        verify(exactly = 1) { bbmConsumer.fjernSammeknytningHovedsøknad(HOVEDSØKNADSID, null) }
        verify(exactly = 1) { behandlingService.logiskSlettBehandling(behandling) }
    }

    @Test
    fun `synkronisering skal feilregistrere FF-klagesøknad for barn med åpen søknad opprettet etter hovedsøknad`() {
        leggTilFFKlagesøknad(barn1)
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))
        mockTilknyttedeSøknader(
            hentSøknad(FF_KLAGESØKNADSID, listOf(barn1.ident!!), behandlingstype = Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE),
            hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!)),
        )

        service.korrigerFFKlagesøknaderForSøknaderOpprettetEtterHovedsøknad(behandling)

        verify(exactly = 1) { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == FF_KLAGESØKNADSID }) }
        barn1.søknadStatus(FF_KLAGESØKNADSID) shouldBe Behandlingstatus.FEILREGISTRERT
        barn1.finnSøknad(OPPRETTET_SØKNADSID)!!.erstatterFFKlagesøknadsid shouldBe FF_KLAGESØKNADSID
    }

    @Test
    fun `synkronisering skal ikke gjøre noe når barn ikke har FF-klagesøknad eller søknad ikke er opprettet etter hovedsøknad`() {
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))
        leggTilFFKlagesøknad(barn2)
        barn2.leggTilSøknad(søknad(OPPRETTET_SØKNADSID + 1, opprettetEtterHovedsøknad = false))

        service.korrigerFFKlagesøknaderForSøknaderOpprettetEtterHovedsøknad(behandling)

        verify(exactly = 0) { bbmConsumer.hentSøknad(any()) }
        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(any()) }
        verify(exactly = 0) { bbmConsumer.opprettSøknader(any()) }
    }

    @Test
    fun `skal gjøre søknad opprettet etter hovedsøknad til hovedsøknad når hovedsøknad er avbrutt`() {
        leggTilSøknadForRoller(søknad(HOVEDSØKNADSID, opprettetEtterHovedsøknad = true), barn2)
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))

        val nyHovedsøknadsid =
            service.håndterSlettetHovedsøknad(
                hentSøknad(HOVEDSØKNADSID, listOf(barn2.ident!!), status = BehandlingStatusType.AVBRUTT),
                behandling,
                emptyList(),
                HOVEDSØKNADSID,
                HOVEDSØKNADSID,
            )

        nyHovedsøknadsid shouldBe OPPRETTET_SØKNADSID
        behandling.soknadsid shouldBe OPPRETTET_SØKNADSID
        verify(exactly = 1) { bbmConsumer.fjernSammeknytningHovedsøknad(HOVEDSØKNADSID, OPPRETTET_SØKNADSID) }
        verify(exactly = 0) { bbmConsumer.opprettSøknader(any()) }
    }

    @Test
    fun `håndterSlettetHovedsøknad skal returnere gjeldende hovedsøknad når opprettet søknad ikke er avbrutt`() {
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))

        val hovedsøknadsid =
            service.håndterSlettetHovedsøknad(
                hentSøknad(HOVEDSØKNADSID, listOf(barn2.ident!!), status = BehandlingStatusType.UNDER_BEHANDLING),
                behandling,
                emptyList(),
                HOVEDSØKNADSID,
                HOVEDSØKNADSID,
            )

        hovedsøknadsid shouldBe HOVEDSØKNADSID
        behandling.soknadsid shouldBe HOVEDSØKNADSID
        verify(exactly = 0) { bbmConsumer.hentSøknad(any()) }
        verify(exactly = 0) { bbmConsumer.fjernSammeknytningHovedsøknad(any(), any()) }
        verify(exactly = 0) { bbmConsumer.opprettSøknader(any()) }
    }

    @Test
    fun `håndterSlettetHovedsøknad skal returnere gjeldende hovedsøknad når hovedsøknaden fortsatt er åpen`() {
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))
        every { bbmConsumer.hentSøknad(HOVEDSØKNADSID) } returns
            HentSøknadResponse(hentSøknad(HOVEDSØKNADSID, listOf(barn2.ident!!), status = BehandlingStatusType.UNDER_BEHANDLING))

        val hovedsøknadsid =
            service.håndterSlettetHovedsøknad(
                hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!), status = BehandlingStatusType.AVBRUTT),
                behandling,
                emptyList(),
                HOVEDSØKNADSID,
                OPPRETTET_SØKNADSID,
            )

        hovedsøknadsid shouldBe HOVEDSØKNADSID
        behandling.soknadsid shouldBe HOVEDSØKNADSID
        verify(exactly = 0) { bbmConsumer.fjernSammeknytningHovedsøknad(any(), any()) }
        verify(exactly = 0) { bbmConsumer.opprettSøknader(any()) }
    }

    @Test
    fun `skal gjenopprette klagesøknad med mottatt dato fra slettet søknad når søknad fra påklaget vedtak slettes`() {
        behandling.søknadstype = Behandlingstype.KLAGE
        val mottattDatoSlettetSøknad = LocalDate.parse("2023-05-05")
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = false))
        every { bbmConsumer.hentSøknad(OPPRETTET_SØKNADSID) } returns
            HentSøknadResponse(
                hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!), refSøknadsid = PÅKLAGET_FF_SØKNADSID)
                    .copy(søknadMottattDato = mottattDatoSlettetSøknad),
            )
        every { bbmConsumer.hentSøknad(HOVEDSØKNADSID) } returns null
        every { kravhaverService.hentSisteLøpendeStønader(any(), any()) } returns emptyList()
        val request = slot<OpprettSøknadRequest>()
        every { bbmConsumer.opprettSøknader(capture(request)) } returns OpprettSøknadResponse(GJENOPPRETTET_FF_KLAGESØKNADSID)
        mockTilknyttedeSøknader()

        service.slettEllerGjennopprettKlageSøknader(behandling, OPPRETTET_SØKNADSID)

        verify(exactly = 1) { bbmConsumer.opprettSøknader(any()) }
        verify(exactly = 1) { bbmConsumer.fjernSammenknytning(OPPRETTET_SØKNADSID) }
        request.captured.søknadMottattDato shouldBe mottattDatoSlettetSøknad
        request.captured.refSøknadsid shouldBe OPPRETTET_SØKNADSID
        request.captured.hovedsøknadsid shouldBe HOVEDSØKNADSID
        request.captured.barnListe.map { it.personident } shouldBe listOf(barn1.ident)
        barn1.søknadStatus(OPPRETTET_SØKNADSID) shouldBe Behandlingstatus.FEILREGISTRERT
        barn1.søknadStatus(GJENOPPRETTET_FF_KLAGESØKNADSID) shouldBe Behandlingstatus.UNDER_BEHANDLING
        verify(exactly = 0) { behandlingService.logiskSlettBehandling(any()) }
    }

    @Test
    fun `skal ikke gjenopprette klagesøknad når slettet søknad er opprettet etter hovedsøknad`() {
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))
        every { bbmConsumer.hentSøknad(OPPRETTET_SØKNADSID) } returns
            HentSøknadResponse(hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!), refSøknadsid = PÅKLAGET_FF_SØKNADSID))
        mockTilknyttedeSøknader()

        service.slettEllerGjennopprettKlageSøknader(behandling, OPPRETTET_SØKNADSID)

        verify(exactly = 0) { bbmConsumer.opprettSøknader(any()) }
        verify(exactly = 0) { bbmConsumer.fjernSammenknytning(any()) }
        barn1.søknadStatus(OPPRETTET_SØKNADSID) shouldBe Behandlingstatus.FEILREGISTRERT
    }

    @Test
    fun `skal gjenopprette klagesøknad for slettet hovedsøknad når den var hovedsøknad i påklaget søknad og ny hovedsøknad finnes`() {
        behandling.søknadstype = Behandlingstype.KLAGE
        val mottattDatoSlettetSøknad = LocalDate.parse("2023-05-05")
        leggTilSøknadForRoller(søknad(HOVEDSØKNADSID, opprettetEtterHovedsøknad = true), barn2)
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))
        every { bbmConsumer.finnSammenknytningerHovedsøknad(HOVEDSØKNADSID, any()) } returns
            FinnSammenknytningerHovedsøknadResponse(
                søknader =
                listOf(
                    hentSøknad(HOVEDSØKNADSID, listOf(barn2.ident!!)),
                    hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!), refSøknadsid = 9999L),
                ),
            )
        every { bbmConsumer.finnSammenknytningerHovedsøknad(OPPRETTET_SØKNADSID, any()) } returns
            FinnSammenknytningerHovedsøknadResponse(søknader = emptyList())
        every { bbmConsumer.hentSøknad(HOVEDSØKNADSID) } returns
            HentSøknadResponse(
                hentSøknad(HOVEDSØKNADSID, listOf(barn2.ident!!), refSøknadsid = PÅKLAGET_FF_SØKNADSID)
                    .copy(søknadMottattDato = mottattDatoSlettetSøknad),
            )
        every { bbmConsumer.hentSøknad(OPPRETTET_SØKNADSID) } returns null
        every { bbmConsumer.finnSammenknytningerHovedsøknad(PÅKLAGET_FF_SØKNADSID, any()) } returns
            FinnSammenknytningerHovedsøknadResponse(hovedsøknadsid = PÅKLAGET_FF_SØKNADSID, søknader = emptyList())
        every { kravhaverService.hentSisteLøpendeStønader(any(), any()) } returns emptyList()
        val request = slot<OpprettSøknadRequest>()
        every { bbmConsumer.opprettSøknader(capture(request)) } returns OpprettSøknadResponse(GJENOPPRETTET_FF_KLAGESØKNADSID)

        service.slettEllerGjennopprettKlageSøknader(behandling, HOVEDSØKNADSID)

        behandling.soknadsid shouldBe OPPRETTET_SØKNADSID
        verify(exactly = 1) { bbmConsumer.fjernSammeknytningHovedsøknad(HOVEDSØKNADSID, OPPRETTET_SØKNADSID) }
        verify(exactly = 1) { bbmConsumer.opprettSøknader(any()) }
        request.captured.refSøknadsid shouldBe HOVEDSØKNADSID
        request.captured.hovedsøknadsid shouldBe OPPRETTET_SØKNADSID
        request.captured.søknadMottattDato shouldBe mottattDatoSlettetSøknad
        request.captured.barnListe.map { it.personident } shouldBe listOf(barn2.ident)
        barn2.søknadStatus(HOVEDSØKNADSID) shouldBe Behandlingstatus.FEILREGISTRERT
        barn2.søknadStatus(GJENOPPRETTET_FF_KLAGESØKNADSID) shouldBe Behandlingstatus.UNDER_BEHANDLING
        verify(exactly = 0) { behandlingService.logiskSlettBehandling(any()) }
    }

    @Test
    fun `skal ikke gjenopprette klagesøknad for slettet hovedsøknad når den ikke var hovedsøknad i påklaget søknad`() {
        leggTilSøknadForRoller(søknad(HOVEDSØKNADSID, opprettetEtterHovedsøknad = true), barn2)
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))
        every { bbmConsumer.finnSammenknytningerHovedsøknad(HOVEDSØKNADSID, any()) } returns
            FinnSammenknytningerHovedsøknadResponse(
                søknader =
                listOf(
                    hentSøknad(HOVEDSØKNADSID, listOf(barn2.ident!!)),
                    hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!), refSøknadsid = 9999L),
                ),
            )
        every { bbmConsumer.finnSammenknytningerHovedsøknad(OPPRETTET_SØKNADSID, any()) } returns
            FinnSammenknytningerHovedsøknadResponse(søknader = emptyList())
        every { bbmConsumer.hentSøknad(HOVEDSØKNADSID) } returns
            HentSøknadResponse(hentSøknad(HOVEDSØKNADSID, listOf(barn2.ident!!), refSøknadsid = 8888L))
        every { bbmConsumer.finnSammenknytningerHovedsøknad(PÅKLAGET_FF_SØKNADSID, any()) } returns
            FinnSammenknytningerHovedsøknadResponse(hovedsøknadsid = PÅKLAGET_FF_SØKNADSID, søknader = emptyList())

        service.slettEllerGjennopprettKlageSøknader(behandling, HOVEDSØKNADSID)

        behandling.soknadsid shouldBe OPPRETTET_SØKNADSID
        verify(exactly = 0) { bbmConsumer.opprettSøknader(any()) }
        barn2.søknadStatus(HOVEDSØKNADSID) shouldBe Behandlingstatus.FEILREGISTRERT
    }

    @Test
    fun `skal feilregistrere alle åpne søknader knyttet til behandlingen og ikke gjenopprette søknader når behandlingen slettes`() {
        leggTilSøknadForRoller(søknad(HOVEDSØKNADSID), barn1)
        every { bbmConsumer.finnSammenknytningerHovedsøknad(HOVEDSØKNADSID, any()) } returns
            FinnSammenknytningerHovedsøknadResponse(søknader = listOf(hentSøknad(HOVEDSØKNADSID, listOf(barn1.ident!!))))
        every { bbmConsumer.hentÅpneSøknaderForBehandling(behandling.id!!) } returns
            HentSøknaderForBehandlingResponse(
                listOf(
                    hentSøknad(FF_KLAGESØKNADSID, listOf(barn1.ident!!), status = BehandlingStatusType.UNDER_BEHANDLING),
                    hentSøknad(OPPRETTET_SØKNADSID, listOf(barn2.ident!!), status = BehandlingStatusType.ÅPEN),
                    hentSøknad(GJENOPPRETTET_FF_KLAGESØKNADSID, listOf(barn2.ident!!), status = BehandlingStatusType.VEDTAK_FATTET),
                ),
            )

        service.slettEllerGjennopprettKlageSøknader(behandling, HOVEDSØKNADSID)

        verify(exactly = 1) { behandlingService.logiskSlettBehandling(behandling) }
        verify(exactly = 1) { bbmConsumer.fjernSammeknytningHovedsøknad(HOVEDSØKNADSID, null) }
        verify(exactly = 1) { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == HOVEDSØKNADSID }) }
        verify(exactly = 1) { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == FF_KLAGESØKNADSID }) }
        verify(exactly = 1) { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == OPPRETTET_SØKNADSID }) }
        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == GJENOPPRETTET_FF_KLAGESØKNADSID }) }
        verify(exactly = 0) { kravhaverService.hentAlleRelevanteKravhavere(any()) }
        verify(exactly = 0) { bbmConsumer.opprettSøknader(any()) }
    }

    @Test
    fun `kanEndreSøknadStatus skal returnere true når søknaden ikke tilhører noen behandling`() {
        every { behandlingService.hentEksisterendeBehandling(OPPRETTET_SØKNADSID) } returns null
        every { bbmConsumer.hentSøknad(OPPRETTET_SØKNADSID) } returns null

        service.kanEndreSøknadStatus(OPPRETTET_SØKNADSID) shouldBe true
    }

    @Test
    fun `kanEndreSøknadStatus skal returnere true når behandlingen ikke er klage eller omgjøring`() {
        behandling.omgjøringsdetaljer = null
        behandling.vedtakstype = Vedtakstype.ENDRING
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = false))
        every { behandlingService.hentEksisterendeBehandling(OPPRETTET_SØKNADSID) } returns behandling

        service.kanEndreSøknadStatus(OPPRETTET_SØKNADSID) shouldBe true
    }

    @Test
    fun `kanEndreSøknadStatus skal returnere false for klagesøknad som ikke er opprettet etter hovedsøknad`() {
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = false))
        every { behandlingService.hentEksisterendeBehandling(OPPRETTET_SØKNADSID) } returns behandling

        service.kanEndreSøknadStatus(OPPRETTET_SØKNADSID) shouldBe false
    }

    @Test
    fun `kanEndreSøknadStatus skal returnere true for klagesøknad opprettet etter hovedsøknad`() {
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))
        every { behandlingService.hentEksisterendeBehandling(OPPRETTET_SØKNADSID) } returns behandling

        service.kanEndreSøknadStatus(OPPRETTET_SØKNADSID) shouldBe true
    }

    @Test
    fun `kanEndreSøknadStatus skal finne behandling via behandlingsid på søknaden når søknaden ikke er hovedsøknad`() {
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = false))
        every { behandlingService.hentEksisterendeBehandling(OPPRETTET_SØKNADSID) } returns null
        every { bbmConsumer.hentSøknad(OPPRETTET_SØKNADSID) } returns
            HentSøknadResponse(hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!)).copy(behandlingsid = behandling.id))
        every { behandlingService.hentBehandlingById(behandling.id!!) } returns behandling

        service.kanEndreSøknadStatus(OPPRETTET_SØKNADSID) shouldBe false
    }
}
