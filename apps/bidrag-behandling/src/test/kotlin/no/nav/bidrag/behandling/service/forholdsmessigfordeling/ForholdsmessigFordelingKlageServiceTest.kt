package no.nav.bidrag.behandling.service.forholdsmessigfordeling

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
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
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.behandling.beregning.felles.OpprettSøknadRequest
import no.nav.bidrag.transport.behandling.beregning.felles.OpprettSøknadResponse
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
        val søknadService = ForholdsmessigFordelingSøknadService(bbmConsumer, mockk(), mockk(), mockk(), mockk())
        service =
            ForholdsmessigFordelingKlageService(
                bbmConsumer = bbmConsumer,
                behandlingService = behandlingService,
                grunnlagService = mockk(),
                underholdService = mockk(),
                virkningstidspunktService = mockk(),
                kravhaverService = mockk(),
                søknadService = søknadService,
                overføringService = mockk(),
            )
        behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG, andreBarn = listOf(testdataBarn2))
        behandling.soknadsid = HOVEDSØKNADSID
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

    private fun Rolle.søknadStatus(søknadsid: Long) = forholdsmessigFordeling!!.søknader.find { it.søknadsid == søknadsid }?.status

    @Test
    fun `skal feilregistrere FF-klagesøknad når det opprettes ny søknad for barnet etter hovedsøknad`() {
        leggTilFFKlagesøknad(barn1)
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))

        service.feilregistrerFFKlagesøknaderErstattetAvOpprettetSøknad(
            behandling,
            hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!)),
            HOVEDSØKNADSID,
        )

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

        service.feilregistrerFFKlagesøknaderErstattetAvOpprettetSøknad(
            behandling,
            hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!)),
            HOVEDSØKNADSID,
        )

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

        service.feilregistrerFFKlagesøknaderErstattetAvOpprettetSøknad(
            behandling,
            hentSøknad(HOVEDSØKNADSID, listOf(barn1.ident!!)),
            HOVEDSØKNADSID,
        )

        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(any()) }
        verify(exactly = 0) { bbmConsumer.feilregistrerSøknadsbarn(any()) }
        barn1.søknadStatus(FF_KLAGESØKNADSID) shouldBe Behandlingstatus.UNDER_BEHANDLING
    }

    @Test
    fun `skal ikke feilregistrere FF-klagesøknad når opprettet søknad er avbrutt eller selv er FF-søknad`() {
        leggTilFFKlagesøknad(barn1)
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))

        service.feilregistrerFFKlagesøknaderErstattetAvOpprettetSøknad(
            behandling,
            hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!), status = BehandlingStatusType.AVBRUTT),
            HOVEDSØKNADSID,
        )
        service.feilregistrerFFKlagesøknaderErstattetAvOpprettetSøknad(
            behandling,
            hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!), behandlingstype = Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE),
            HOVEDSØKNADSID,
        )

        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(any()) }
        verify(exactly = 0) { bbmConsumer.feilregistrerSøknadsbarn(any()) }
        barn1.finnSøknad(OPPRETTET_SØKNADSID)!!.erstatterFFKlagesøknadsid.shouldBeNull()
    }

    @Test
    fun `skal ikke feilregistrere FF-klagesøknad for barn som ikke er del av opprettet søknad`() {
        leggTilFFKlagesøknad(barn2)
        barn1.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))

        service.feilregistrerFFKlagesøknaderErstattetAvOpprettetSøknad(
            behandling,
            hentSøknad(OPPRETTET_SØKNADSID, listOf(barn1.ident!!)),
            HOVEDSØKNADSID,
        )

        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(any()) }
        verify(exactly = 0) { bbmConsumer.feilregistrerSøknadsbarn(any()) }
        barn2.søknadStatus(FF_KLAGESØKNADSID) shouldBe Behandlingstatus.UNDER_BEHANDLING
    }

    @Test
    fun `skal gjenopprette FF-klagesøknad når søknad som erstattet den slettes`() {
        leggTilSøknadForRoller(
            søknad(FF_KLAGESØKNADSID, Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE, omgjørSøknadsid = PÅKLAGET_FF_SØKNADSID)
                .copy(status = Behandlingstatus.FEILREGISTRERT),
            barn1,
        )
        barn1.leggTilSøknad(
            søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true, erstatterFFKlagesøknadsid = FF_KLAGESØKNADSID),
        )
        val request = slot<OpprettSøknadRequest>()
        every { bbmConsumer.opprettSøknader(capture(request)) } returns OpprettSøknadResponse(GJENOPPRETTET_FF_KLAGESØKNADSID)

        service.slettEllerGjennopprettKlageSøknader(behandling, OPPRETTET_SØKNADSID)

        assertSoftlyRequest(request.captured)
        verify(exactly = 1) { bbmConsumer.fjernSammenknytning(OPPRETTET_SØKNADSID) }
        verify(exactly = 0) { bbmConsumer.hentSøknad(any()) }
        barn1.søknadStatus(OPPRETTET_SØKNADSID) shouldBe Behandlingstatus.FEILREGISTRERT
        listOf(barn1, behandling.bidragsmottaker!!, behandling.bidragspliktig!!).forEach {
            val gjenopprettet = it.finnSøknad(GJENOPPRETTET_FF_KLAGESØKNADSID)
            gjenopprettet.shouldNotBeNull()
            gjenopprettet.behandlingstype shouldBe Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE
            gjenopprettet.omgjørSøknadsid shouldBe PÅKLAGET_FF_SØKNADSID
            gjenopprettet.erstatterFFKlagesøknadsid.shouldBeNull()
        }
        barn2.finnSøknad(GJENOPPRETTET_FF_KLAGESØKNADSID).shouldBeNull()
    }

    private fun assertSoftlyRequest(request: OpprettSøknadRequest) {
        request.behandlingstype shouldBe Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE
        request.refSøknadsid shouldBe PÅKLAGET_FF_SØKNADSID
        request.refVedtaksid shouldBe OMGJØR_VEDTAKSID
        request.hovedsøknadsid shouldBe HOVEDSØKNADSID
        request.behandlingsid shouldBe behandling.id
        request.barnListe shouldHaveSize 1
        request.barnListe.first().personident shouldBe barn1.ident
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
    fun `skal gjenopprette FF-klagesøknad når søknad som ble gjort til hovedsøknad slettes`() {
        leggTilSøknadForRoller(
            søknad(FF_KLAGESØKNADSID, Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE, omgjørSøknadsid = PÅKLAGET_FF_SØKNADSID)
                .copy(status = Behandlingstatus.FEILREGISTRERT),
            barn1,
        )
        barn1.leggTilSøknad(
            søknad(HOVEDSØKNADSID, opprettetEtterHovedsøknad = true, erstatterFFKlagesøknadsid = FF_KLAGESØKNADSID),
        )
        barn2.leggTilSøknad(søknad(OPPRETTET_SØKNADSID, opprettetEtterHovedsøknad = true))
        every { bbmConsumer.finnSammenknytningerHovedsøknad(HOVEDSØKNADSID, any()) } returns
            FinnSammenknytningerHovedsøknadResponse(
                søknader =
                listOf(
                    hentSøknad(HOVEDSØKNADSID, listOf(barn1.ident!!)),
                    hentSøknad(OPPRETTET_SØKNADSID, listOf(barn2.ident!!), refSøknadsid = 9999L),
                ),
            )
        val request = slot<OpprettSøknadRequest>()
        every { bbmConsumer.opprettSøknader(capture(request)) } returns OpprettSøknadResponse(GJENOPPRETTET_FF_KLAGESØKNADSID)

        service.slettEllerGjennopprettKlageSøknader(behandling, HOVEDSØKNADSID)

        behandling.soknadsid shouldBe OPPRETTET_SØKNADSID
        request.captured.hovedsøknadsid shouldBe OPPRETTET_SØKNADSID
        request.captured.barnListe.map { it.personident } shouldBe listOf(barn1.ident)
        barn1.finnSøknad(GJENOPPRETTET_FF_KLAGESØKNADSID).shouldNotBeNull()
    }
}
