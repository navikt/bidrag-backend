package no.nav.bidrag.behandling.service

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.behandling.consumer.BidragBBMConsumer
import no.nav.bidrag.behandling.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.behandling.consumer.BidragSakConsumer
import no.nav.bidrag.behandling.consumer.BidragTilgangskontrollConsumer
import no.nav.bidrag.behandling.consumer.BidragVedtakConsumer
import no.nav.bidrag.behandling.database.datamodell.GebyrRolle
import no.nav.bidrag.behandling.database.datamodell.GebyrRolleSøknad
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordeling
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordelingRolle
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordelingSøknadBarn
import no.nav.bidrag.behandling.database.datamodell.leggTilGebyr
import no.nav.bidrag.behandling.database.repository.BehandlingRepository
import no.nav.bidrag.behandling.dto.v2.behandling.Grunnlagsdatatype
import no.nav.bidrag.behandling.dto.v2.validering.GrunnlagFeilDto
import no.nav.bidrag.behandling.service.forholdsmessigfordeling.ForholdsmessigFordelingService
import no.nav.bidrag.behandling.utils.stubPersonConsumer
import no.nav.bidrag.behandling.utils.testdata.opprettGyldigBehandlingForBeregningOgVedtak
import no.nav.bidrag.domene.enums.behandling.Behandlingstatus
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.domene.enums.behandling.Behandlingstype
import no.nav.bidrag.domene.enums.behandling.TypeBehandling
import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.enums.rolle.SøktAvType
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidragPeriodeResponse
import no.nav.bidrag.transport.behandling.beregning.felles.HentBPsÅpneSøknaderResponse
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknadResponse
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknaderForBehandlingResponse
import no.nav.bidrag.transport.behandling.beregning.felles.PartISøknad
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import no.nav.bidrag.transport.søknad.FinnSammenknytningerHovedsøknadResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ForholdsmessigFordelingServiceSynkroniseringTest {
    @MockK
    lateinit var vedtakConsumer: BidragVedtakConsumer

    @MockK
    lateinit var sakConsumer: BidragSakConsumer

    @MockK
    lateinit var behandlingRepository: BehandlingRepository

    @MockK
    lateinit var behandlingService: BehandlingService

    @MockK
    lateinit var beløpshistorikkConsumer: BidragBeløpshistorikkConsumer

    @MockK
    lateinit var grunnlagService: GrunnlagService

    @MockK
    lateinit var bbmConsumer: BidragBBMConsumer

    @MockK
    lateinit var forsendelseService: ForsendelseService

    @MockK
    lateinit var beregningService: BeregningService

    @MockK
    lateinit var virkningstidspunktService: VirkningstidspunktService

    @MockK
    lateinit var underholdService: UnderholdService

    @MockK
    lateinit var tilgangskontrollConsumer: BidragTilgangskontrollConsumer

    private lateinit var service: ForholdsmessigFordelingService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        service =
            ForholdsmessigFordelingService(
                sakConsumer,
                behandlingRepository,
                behandlingService,
                beløpshistorikkConsumer,
                grunnlagService,
                bbmConsumer,
                vedtakConsumer,

                forsendelseService,
                beregningService,
                virkningstidspunktService,
                underholdService,
            )
        service.grenseSynkroniserFF = "60"

        every { grunnlagService.lagreBeløpshistorikkGrunnlag(any()) } returns emptyMap<Grunnlagsdatatype, GrunnlagFeilDto>()
        every { grunnlagService.lagreBeløpshistorikkFraOpprinneligVedtakstidspunktGrunnlag(any()) } returns
            emptyMap<Grunnlagsdatatype, GrunnlagFeilDto>()
        every { bbmConsumer.hentÅpneSøknaderForBp(any()) } returns
            mockk<HentBPsÅpneSøknaderResponse> {
                every { åpneSøknader } returns emptyList()
            }
        every { bbmConsumer.hentSøknad(any()) } returns null
        every { bbmConsumer.hentÅpneSøknaderForBehandling(any()) } returns HentSøknaderForBehandlingResponse(emptyList())
        every { bbmConsumer.feilregistrerSøknad(any()) } returns Unit
        every { bbmConsumer.fjernSammenknytning(any()) } returns Unit
        every { bbmConsumer.endreSammenknytningSøknad(any(), any()) } returns null
        every { bbmConsumer.sammeknyttSøknader(any(), any()) } returns null
        every { beløpshistorikkConsumer.hentAlleLøpendeStønaderIPeriode(any()) } returns
            mockk<LøpendeBidragPeriodeResponse> {
                every { bidragListe } returns emptyList()
            }
        every { underholdService.oppretteUnderholdskostnad(any(), any(), any()) } returns mockk(relaxed = true)
        every { virkningstidspunktService.oppdaterVirkningstidspunkt(any(), any(), any(), any(), any(), any()) } returns Unit
    }

    @Test
    fun `skal fjerne gamle sammenknytninger og legge til manglende hovedsoknad for ff behandling`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(erHovedbehandling = true)

        val annenSøknad = mockk<HentSøknad> { every { søknadsid } returns 999L }
        every { bbmConsumer.finnSammenknytningerHovedsøknad(behandling.soknadsid!!) } returns
            FinnSammenknytningerHovedsøknadResponse(søknader = listOf(annenSøknad))

        service.synkroniserSøknadsbarnOgRevurderingsbarnForFFBehandling(behandling, ignorerSynkTimer = true)

        verify(exactly = 1) { grunnlagService.lagreBeløpshistorikkGrunnlag(behandling) }
        verify(exactly = 1) { grunnlagService.lagreBeløpshistorikkFraOpprinneligVedtakstidspunktGrunnlag(behandling) }
        verify(exactly = 1) { bbmConsumer.fjernSammenknytning(999L) }
        verify(exactly = 1) { bbmConsumer.sammeknyttSøknader(behandling.soknadsid!!, behandling.soknadsid!!) }
        verify(exactly = 0) { bbmConsumer.endreSammenknytningSøknad(any(), any()) }
    }

    @Test
    fun `skal ikke synkronisere sammenknytninger nar behandling ikke er ff`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.forholdsmessigFordeling = null

        service.synkroniserSøknadsbarnOgRevurderingsbarnForFFBehandling(behandling)

        verify(exactly = 0) { bbmConsumer.finnSammenknytningerHovedsøknad(any()) }
        verify(exactly = 0) { bbmConsumer.fjernSammenknytning(any()) }
        verify(exactly = 0) { bbmConsumer.endreSammenknytningSøknad(any(), any()) }
    }

    @Test
    fun `skal ikke endre hovedsoknadsknytning nar hovedsoknad finnes`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(erHovedbehandling = true)

        val hovedSøknad = mockk<HentSøknad> { every { søknadsid } returns behandling.soknadsid!! }
        every { bbmConsumer.finnSammenknytningerHovedsøknad(behandling.soknadsid!!) } returns
            FinnSammenknytningerHovedsøknadResponse(søknader = listOf(hovedSøknad))

        service.synkroniserSøknadsbarnOgRevurderingsbarnForFFBehandling(behandling)

        verify(exactly = 0) { bbmConsumer.endreSammenknytningSøknad(any(), any()) }
        verify(exactly = 0) { bbmConsumer.fjernSammenknytning(any()) }
        behandling.soknadsid shouldBe behandling.soknadsid
    }

    @Test
    fun `skal feilregistrere duplicate apne ff soknader og beholde eldste`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        val rolle = behandling.søknadsbarn.first()

        val eldste =
            opprettSøknad(
                søknadsid = 1001L,
                søknadFomDato = LocalDate.parse("2024-01-01"),
                mottattDato = LocalDate.parse("2024-01-10"),
                behandlingstype = behandling.behandlingstypeForFF,
            )
        val duplikat1 =
            opprettSøknad(
                søknadsid = 1002L,
                søknadFomDato = LocalDate.parse("2024-02-01"),
                mottattDato = LocalDate.parse("2024-02-10"),
                behandlingstype = behandling.behandlingstypeForFF,
            )
        val duplikat2 =
            opprettSøknad(
                søknadsid = 1003L,
                søknadFomDato = LocalDate.parse("2024-03-01"),
                mottattDato = LocalDate.parse("2024-03-10"),
                behandlingstype = behandling.behandlingstypeForFF,
            )

        rolle.forholdsmessigFordeling =
            ForholdsmessigFordelingRolle(
                tilhørerSak = behandling.saksnummer,
                behandlerenhet = behandling.behandlerEnhet,
                delAvOpprinneligBehandling = true,
                erRevurdering = true,
                bidragsmottaker = behandling.bidragsmottaker?.ident,
                søknader = mutableSetOf(eldste, duplikat1, duplikat2),
            )

        service.slettDuplikatForholdsmessigFordelingSøknader(behandling)

        verify(exactly = 1) { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == 1002L }) }
        verify(exactly = 1) { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == 1003L }) }
        verify(exactly = 1) { bbmConsumer.fjernSammenknytning(1002L) }
        verify(exactly = 1) { bbmConsumer.fjernSammenknytning(1003L) }

        eldste.status shouldBe Behandlingstatus.UNDER_BEHANDLING
        duplikat1.status shouldBe Behandlingstatus.FEILREGISTRERT
        duplikat2.status shouldBe Behandlingstatus.FEILREGISTRERT
    }

    @Test
    fun `skal ikke feilregistrere nar det bare finnes en apen ff soknad`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        val rolle = behandling.søknadsbarn.first()

        rolle.forholdsmessigFordeling =
            ForholdsmessigFordelingRolle(
                tilhørerSak = behandling.saksnummer,
                behandlerenhet = behandling.behandlerEnhet,
                delAvOpprinneligBehandling = true,
                erRevurdering = true,
                bidragsmottaker = behandling.bidragsmottaker?.ident,
                søknader =
                mutableSetOf(
                    opprettSøknad(
                        søknadsid = 1001L,
                        søknadFomDato = LocalDate.parse("2024-01-01"),
                        mottattDato = LocalDate.parse("2024-01-10"),
                        behandlingstype = behandling.behandlingstypeForFF,
                    ),
                    opprettSøknad(
                        søknadsid = 2001L,
                        søknadFomDato = LocalDate.parse("2024-01-01"),
                        mottattDato = LocalDate.parse("2024-01-10"),
                        behandlingstype = Behandlingstype.ALDERSJUSTERING,
                    ),
                    opprettSøknad(
                        søknadsid = 2002L,
                        søknadFomDato = LocalDate.parse("2024-01-01"),
                        mottattDato = LocalDate.parse("2024-01-10"),
                        behandlingstype = behandling.behandlingstypeForFF,
                        status = Behandlingstatus.FEILREGISTRERT,
                    ),
                ),
            )

        service.slettDuplikatForholdsmessigFordelingSøknader(behandling)

        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(any()) }
        verify(exactly = 0) { bbmConsumer.fjernSammenknytning(any()) }
    }

    @Test
    fun `skal beholde apen status for duplikat nar feilregistrering i bbm feiler`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        val rolle = behandling.søknadsbarn.first()

        val eldste =
            opprettSøknad(
                søknadsid = 1001L,
                søknadFomDato = LocalDate.parse("2024-01-01"),
                mottattDato = LocalDate.parse("2024-01-10"),
                behandlingstype = behandling.behandlingstypeForFF,
            )
        val vellykketDuplikat =
            opprettSøknad(
                søknadsid = 1002L,
                søknadFomDato = LocalDate.parse("2024-02-01"),
                mottattDato = LocalDate.parse("2024-02-10"),
                behandlingstype = behandling.behandlingstypeForFF,
            )
        val feiletDuplikat =
            opprettSøknad(
                søknadsid = 1003L,
                søknadFomDato = LocalDate.parse("2024-03-01"),
                mottattDato = LocalDate.parse("2024-03-10"),
                behandlingstype = behandling.behandlingstypeForFF,
            )

        rolle.forholdsmessigFordeling =
            ForholdsmessigFordelingRolle(
                tilhørerSak = behandling.saksnummer,
                behandlerenhet = behandling.behandlerEnhet,
                delAvOpprinneligBehandling = true,
                erRevurdering = true,
                bidragsmottaker = behandling.bidragsmottaker?.ident,
                søknader = mutableSetOf(eldste, vellykketDuplikat, feiletDuplikat),
            )

        every { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == 1003L }) } throws RuntimeException("BBM feil")

        service.slettDuplikatForholdsmessigFordelingSøknader(behandling)

        verify(exactly = 1) { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == 1002L }) }
        verify(exactly = 1) { bbmConsumer.feilregistrerSøknad(match { it.søknadsid == 1003L }) }
        verify(exactly = 1) { bbmConsumer.fjernSammenknytning(1002L) }
        verify(exactly = 0) { bbmConsumer.fjernSammenknytning(1003L) }

        eldste.status shouldBe Behandlingstatus.UNDER_BEHANDLING
        vellykketDuplikat.status shouldBe Behandlingstatus.FEILREGISTRERT
        feiletDuplikat.status shouldBe Behandlingstatus.UNDER_BEHANDLING
    }

    @Test
    fun `skal bare feilregistrere barnet fra duplikat ff soknad nar soknaden har andre barn under behandling`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        val rolle = behandling.søknadsbarn.first()

        val eldste =
            opprettSøknad(
                søknadsid = 1001L,
                søknadFomDato = LocalDate.parse("2024-01-01"),
                mottattDato = LocalDate.parse("2024-01-10"),
                behandlingstype = behandling.behandlingstypeForFF,
            )
        val duplikatMedSøsken =
            opprettSøknad(
                søknadsid = 1002L,
                søknadFomDato = LocalDate.parse("2024-02-01"),
                mottattDato = LocalDate.parse("2024-02-10"),
                behandlingstype = behandling.behandlingstypeForFF,
            )
        rolle.forholdsmessigFordeling =
            ForholdsmessigFordelingRolle(
                tilhørerSak = behandling.saksnummer,
                behandlerenhet = behandling.behandlerEnhet,
                delAvOpprinneligBehandling = true,
                erRevurdering = true,
                bidragsmottaker = behandling.bidragsmottaker?.ident,
                søknader = mutableSetOf(eldste, duplikatMedSøsken),
            )

        every { bbmConsumer.hentSøknad(1002L) } returns
            HentSøknadResponse(
                søknad =
                mockk<HentSøknad> {
                    every { partISøknadListe } returns
                        listOf(
                            PartISøknad(personident = rolle.ident, rolletype = Rolletype.BARN, behandlingstatus = Behandlingstatus.UNDER_BEHANDLING),
                            PartISøknad(personident = "søsken", rolletype = Rolletype.BARN, behandlingstatus = Behandlingstatus.UNDER_BEHANDLING),
                        )
                },
            )
        every { bbmConsumer.feilregistrerSøknadsbarn(any()) } returns Unit

        service.slettDuplikatForholdsmessigFordelingSøknader(behandling)

        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(any()) }
        verify(exactly = 1) { bbmConsumer.feilregistrerSøknadsbarn(match { it.søknadsid == 1002L && it.personidentBarn == rolle.ident }) }
        eldste.status shouldBe Behandlingstatus.UNDER_BEHANDLING
        duplikatMedSøsken.status shouldBe Behandlingstatus.FEILREGISTRERT
    }

    @Test
    fun `skal bare feilregistrere barnet fra duplikat ff soknad nar oppslag mot bbm feiler`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        val rolle = behandling.søknadsbarn.first()

        val eldste =
            opprettSøknad(
                søknadsid = 1001L,
                søknadFomDato = LocalDate.parse("2024-01-01"),
                mottattDato = LocalDate.parse("2024-01-10"),
                behandlingstype = behandling.behandlingstypeForFF,
            )
        val duplikat =
            opprettSøknad(
                søknadsid = 1002L,
                søknadFomDato = LocalDate.parse("2024-02-01"),
                mottattDato = LocalDate.parse("2024-02-10"),
                behandlingstype = behandling.behandlingstypeForFF,
            )
        rolle.forholdsmessigFordeling =
            ForholdsmessigFordelingRolle(
                tilhørerSak = behandling.saksnummer,
                behandlerenhet = behandling.behandlerEnhet,
                delAvOpprinneligBehandling = true,
                erRevurdering = true,
                bidragsmottaker = behandling.bidragsmottaker?.ident,
                søknader = mutableSetOf(eldste, duplikat),
            )

        every { bbmConsumer.hentSøknad(1002L) } throws RuntimeException("BBM nede")
        every { bbmConsumer.feilregistrerSøknadsbarn(any()) } returns Unit

        service.slettDuplikatForholdsmessigFordelingSøknader(behandling)

        verify(exactly = 0) { bbmConsumer.feilregistrerSøknad(any()) }
        verify(exactly = 1) { bbmConsumer.feilregistrerSøknadsbarn(match { it.søknadsid == 1002L }) }
    }

    @Test
    fun `skal legge til manglende revurderingsbarn fra ff soknad med samme sokt fom dato`() {
        stubPersonConsumer()
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(erHovedbehandling = true)
        val antallBarnFør = behandling.søknadsbarn.size
        val ffSøknad = opprettFFSøknad(behandling, behandling.behandlingstypeForFF)
        every { bbmConsumer.hentÅpneSøknaderForBehandling(behandling.id!!) } returns HentSøknaderForBehandlingResponse(listOf(ffSøknad))

        service.leggTilManglendeBarnFraSøknaderIBehandling(behandling, emptyList())

        assertRevurderingsbarnLagtTil(behandling, antallBarnFør, ffSøknad)
    }

    @Test
    fun `skal legge til manglende revurderingsbarn fra ff klagesoknad i klagebehandling`() {
        stubPersonConsumer()
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.vedtakstype = Vedtakstype.KLAGE
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(erHovedbehandling = true)
        val antallBarnFør = behandling.søknadsbarn.size
        val ffSøknad = opprettFFSøknad(behandling, Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE)
        every { bbmConsumer.hentÅpneSøknaderForBehandling(behandling.id!!) } returns HentSøknaderForBehandlingResponse(listOf(ffSøknad))

        service.leggTilManglendeBarnFraSøknaderIBehandling(behandling, emptyList())

        assertRevurderingsbarnLagtTil(behandling, antallBarnFør, ffSøknad)
    }

    @Test
    fun `skal ikke legge til revurderingsbarn fra ff soknad nar barnet allerede finnes i behandlingen`() {
        stubPersonConsumer()
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(erHovedbehandling = true)
        val eksisterendeBarn = behandling.søknadsbarn.first()
        val antallRollerFør = behandling.roller.size
        val ffSøknad =
            opprettFFSøknad(behandling, behandling.behandlingstypeForFF, barnIdent = eksisterendeBarn.ident!!)
        every { bbmConsumer.hentÅpneSøknaderForBehandling(behandling.id!!) } returns HentSøknaderForBehandlingResponse(listOf(ffSøknad))

        service.leggTilManglendeBarnFraSøknaderIBehandling(behandling, emptyList())

        behandling.roller shouldHaveSize antallRollerFør
        verify(exactly = 0) { underholdService.oppretteUnderholdskostnad(any(), any(), any()) }
        verify(exactly = 0) { virkningstidspunktService.oppdaterVirkningstidspunkt(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `skal legge til manglende revurderingsbarn fra ff soknad som nettopp er knyttet til behandlingen`() {
        stubPersonConsumer()
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.vedtakstype = Vedtakstype.KLAGE
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(erHovedbehandling = true)
        val antallBarnFør = behandling.søknadsbarn.size
        val ffSøknad = opprettFFSøknad(behandling, Behandlingstype.FORHOLDSMESSIG_FORDELING_KLAGE).copy(behandlingsid = null)

        service.leggTilManglendeBarnFraSøknaderIBehandling(behandling, emptyList(), listOf(ffSøknad))

        assertRevurderingsbarnLagtTil(behandling, antallBarnFør, ffSøknad)
    }

    @Test
    fun `skal legge til manglende barn fra ikke ff soknad knyttet til behandlingen som soknadsbarn`() {
        stubPersonConsumer()
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.vedtakstype = Vedtakstype.KLAGE
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(erHovedbehandling = true)
        val antallBarnFør = behandling.søknadsbarn.size
        val søknad = opprettFFSøknad(behandling, Behandlingstype.KLAGE).copy(behandlingsid = null)
        every { sakConsumer.hentSakerPerson(any()) } returns emptyList()
        every { behandlingRepository.finnÅpneBidragsbehandlingerForBp(any(), any()) } returns emptyList()
        every { behandlingService.sendOppdatertHendelse(any(), any()) } returns Unit
        every { behandlingService.lagreBehandling(any()) } answers { firstArg() }

        service.leggTilManglendeBarnFraSøknaderIBehandling(behandling, emptyList(), listOf(søknad))

        behandling.søknadsbarn shouldHaveSize antallBarnFør + 1
        val nyttBarn = behandling.søknadsbarn.find { it.ident == NYTT_BARN_IDENT }!!
        nyttBarn.forholdsmessigFordeling!!.erRevurdering shouldBe false
        nyttBarn.forholdsmessigFordeling!!.søknader.any { it.søknadsid == søknad.søknadsid } shouldBe true
    }

    @Test
    fun `skal fjerne gebyrsoknad for bp og bm nar soknad er avbrutt i bbm`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        val bp = behandling.bidragspliktig!!
        val bm = behandling.bidragsmottaker!!
        bp.gebyr = GebyrRolle(gebyrSøknader = mutableSetOf(gebyrSøknad(behandling, 5555L), gebyrSøknad(behandling, 6666L)))
        bm.gebyr = GebyrRolle(gebyrSøknader = mutableSetOf(gebyrSøknad(behandling, 5555L)))
        bm.harGebyrsøknad = true
        val slettetSøknad = opprettFFSøknad(behandling, behandling.behandlingstypeForFF).copy(behandlingStatusType = BehandlingStatusType.AVBRUTT)
        val åpenSøknad = opprettFFSøknad(behandling, behandling.behandlingstypeForFF).copy(søknadsid = 6666L)
        every { bbmConsumer.hentSøknad(5555L) } returns HentSøknadResponse(slettetSøknad)
        every { bbmConsumer.hentSøknad(6666L) } returns HentSøknadResponse(åpenSøknad)

        service.fjernGebyrForFeilregistrerteSøknader(behandling)

        bp.gebyrSøknader.map { it.søknadsid } shouldBe listOf(6666L)
        bm.gebyrSøknader shouldHaveSize 0
        bm.harGebyrsøknad shouldBe false
    }

    @Test
    fun `skal fjerne gebyrsoknad nar alle barn i soknaden er feilregistrert`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        val bm = behandling.bidragsmottaker!!
        bm.gebyr = GebyrRolle(gebyrSøknader = mutableSetOf(gebyrSøknad(behandling, 5555L)))
        val søknad = opprettFFSøknad(behandling, behandling.behandlingstypeForFF)
        every { bbmConsumer.hentSøknad(5555L) } returns
            HentSøknadResponse(
                søknad.copy(
                    partISøknadListe =
                    søknad.partISøknadListe.map {
                        if (it.rolletype == Rolletype.BARN) it.copy(behandlingstatus = Behandlingstatus.FEILREGISTRERT) else it
                    },
                ),
            )

        service.fjernGebyrForFeilregistrerteSøknader(behandling)

        bm.gebyrSøknader.none { it.søknadsid == 5555L } shouldBe true
    }

    @Test
    fun `skal ikke fjerne gebyrsoknad nar soknad er apen eller ikke kan hentes fra bbm`() {
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        val bm = behandling.bidragsmottaker!!
        bm.gebyr = GebyrRolle(gebyrSøknader = mutableSetOf(gebyrSøknad(behandling, 5555L), gebyrSøknad(behandling, 6666L)))
        every { bbmConsumer.hentSøknad(5555L) } returns HentSøknadResponse(opprettFFSøknad(behandling, behandling.behandlingstypeForFF))
        every { bbmConsumer.hentSøknad(6666L) } returns null

        service.fjernGebyrForFeilregistrerteSøknader(behandling)

        bm.gebyrSøknader.map { it.søknadsid }.toSet() shouldBe setOf(5555L, 6666L)
    }

    private fun gebyrSøknad(
        behandling: no.nav.bidrag.behandling.database.datamodell.Behandling,
        søknadsid: Long,
    ) = GebyrRolleSøknad(saksnummer = behandling.saksnummer, søknadsid = søknadsid)

    private fun assertRevurderingsbarnLagtTil(
        behandling: no.nav.bidrag.behandling.database.datamodell.Behandling,
        antallBarnFør: Int,
        ffSøknad: HentSøknad,
    ) {
        behandling.søknadsbarn shouldHaveSize antallBarnFør + 1
        val nyttBarn = behandling.søknadsbarn.find { it.ident == NYTT_BARN_IDENT }
        nyttBarn shouldNotBe null
        nyttBarn!!.virkningstidspunkt shouldBe ffSøknad.søknadFomDato
        nyttBarn.forholdsmessigFordeling!!.erRevurdering shouldBe true
        nyttBarn.forholdsmessigFordeling!!.delAvOpprinneligBehandling shouldBe false
        nyttBarn.forholdsmessigFordeling!!.tilhørerSak shouldBe ffSøknad.saksnummer
        nyttBarn.forholdsmessigFordeling!!.revurderingsdatoVedOpprettelseAvFF shouldBe ffSøknad.søknadFomDato
        nyttBarn.forholdsmessigFordeling!!.søknader.any { it.søknadsid == ffSøknad.søknadsid } shouldBe true
        behandling.bidragspliktig!!.forholdsmessigFordeling!!.søknader.any { it.søknadsid == ffSøknad.søknadsid } shouldBe true

        behandling.samvær.any { it.rolle.ident == NYTT_BARN_IDENT } shouldBe true
        behandling.husstandsmedlem.any { it.ident == NYTT_BARN_IDENT } shouldBe true
        verify(exactly = 1) {
            underholdService.oppretteUnderholdskostnad(behandling, match { it.personident?.verdi == NYTT_BARN_IDENT }, any())
        }
        verify(exactly = 1) { virkningstidspunktService.oppdaterVirkningstidspunkt(null, null, behandling, true, any(), any()) }
    }

    @Test
    fun `skal bevare gebyr og innbetalt belop fra bbm for gjenopprettede roller fra ff soknad`() {
        stubPersonConsumer()
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(erHovedbehandling = true)
        val ffSøknad = opprettFFSøknadMedGebyr(behandling)
        every { bbmConsumer.hentÅpneSøknaderForBehandling(behandling.id!!) } returns HentSøknaderForBehandlingResponse(listOf(ffSøknad))

        service.leggTilManglendeBarnFraSøknaderIBehandling(behandling, emptyList())

        val nyttBarn = behandling.søknadsbarn.find { it.ident == NYTT_BARN_IDENT }!!
        assertSoftly<Unit> {
            nyttBarn.harGebyrsøknad shouldBe true
            nyttBarn.innbetaltBeløp shouldBe BigDecimal(1000)
            nyttBarn.gebyrSøknader.find { it.søknadsid == ffSøknad.søknadsid }!!.referanse shouldBe "gebyr_barn"
            behandling.bidragsmottaker!!.harGebyrsøknad shouldBe true
            behandling.bidragsmottaker!!.gebyrSøknader.find { it.søknadsid == ffSøknad.søknadsid }!!.referanse shouldBe "gebyr_bm"
            behandling.bidragspliktig!!.harGebyrsøknad shouldBe true
            behandling.bidragspliktig!!.gebyrSøknader.find { it.søknadsid == ffSøknad.søknadsid }!!.referanse shouldBe "gebyr_bp"
        }
    }

    @Test
    fun `skal ikke overskrive eksisterende gebyr for soknad ved gjenoppretting av roller fra ff soknad`() {
        stubPersonConsumer()
        val behandling = opprettGyldigBehandlingForBeregningOgVedtak(generateId = true, typeBehandling = TypeBehandling.BIDRAG)
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(erHovedbehandling = true)
        val ffSøknad = opprettFFSøknadMedGebyr(behandling)
        behandling.bidragspliktig!!.leggTilGebyr(
            listOf(GebyrRolleSøknad(saksnummer = behandling.saksnummer, søknadsid = ffSøknad.søknadsid, referanse = "eksisterende")),
        )
        every { bbmConsumer.hentÅpneSøknaderForBehandling(behandling.id!!) } returns HentSøknaderForBehandlingResponse(listOf(ffSøknad))

        service.leggTilManglendeBarnFraSøknaderIBehandling(behandling, emptyList())

        val gebyrBp = behandling.bidragspliktig!!.gebyrSøknader.filter { it.søknadsid == ffSøknad.søknadsid }
        gebyrBp shouldHaveSize 1
        gebyrBp.first().referanse shouldBe "eksisterende"
    }

    private fun opprettFFSøknadMedGebyr(behandling: no.nav.bidrag.behandling.database.datamodell.Behandling) = opprettFFSøknad(behandling, behandling.behandlingstypeForFF).copy(
        partISøknadListe =
        listOf(
            PartISøknad(
                personident = behandling.bidragspliktig!!.ident,
                rolletype = Rolletype.BIDRAGSPLIKTIG,
                gebyr = true,
                referanseGebyr = "gebyr_bp",
            ),
            PartISøknad(
                personident = behandling.bidragsmottaker!!.ident,
                rolletype = Rolletype.BIDRAGSMOTTAKER,
                behandlingstatus = Behandlingstatus.UNDER_BEHANDLING,
                gebyr = true,
                referanseGebyr = "gebyr_bm",
            ),
            PartISøknad(
                personident = NYTT_BARN_IDENT,
                rolletype = Rolletype.BARN,
                behandlingstatus = Behandlingstatus.UNDER_BEHANDLING,
                gebyr = true,
                referanseGebyr = "gebyr_barn",
                innbetaltBeløp = BigDecimal(1000),
            ),
        ),
    )

    private fun opprettFFSøknad(
        behandling: no.nav.bidrag.behandling.database.datamodell.Behandling,
        behandlingstype: Behandlingstype,
        barnIdent: String = NYTT_BARN_IDENT,
    ) = HentSøknad(
        søknadsid = 5555L,
        søknadMottattDato = LocalDate.parse("2024-03-10"),
        søknadFomDato = LocalDate.parse("2024-03-01"),
        behandlingstema = Behandlingstema.BIDRAG,
        behandlingstype = behandlingstype,
        behandlerenhet = behandling.behandlerEnhet,
        saksnummer = behandling.saksnummer,
        behandlingsid = behandling.id,
        innkreving = true,
        søktAvType = SøktAvType.NAV_BIDRAG,
        behandlingStatusType = BehandlingStatusType.UNDER_BEHANDLING,
        partISøknadListe =
        listOf(
            PartISøknad(personident = behandling.bidragsmottaker!!.ident, rolletype = Rolletype.BIDRAGSMOTTAKER, behandlingstatus = Behandlingstatus.UNDER_BEHANDLING),
            PartISøknad(personident = barnIdent, rolletype = Rolletype.BARN, behandlingstatus = Behandlingstatus.UNDER_BEHANDLING),
        ),
    )

    private fun opprettSøknad(
        søknadsid: Long,
        søknadFomDato: LocalDate,
        mottattDato: LocalDate,
        behandlingstype: Behandlingstype,
        status: Behandlingstatus = Behandlingstatus.UNDER_BEHANDLING,
    ) = ForholdsmessigFordelingSøknadBarn(
        mottattDato = mottattDato,
        søknadFomDato = søknadFomDato,
        søktAvType = SøktAvType.NAV_BIDRAG,
        søknadsid = søknadsid,
        behandlingstype = behandlingstype,
        behandlingstema = Behandlingstema.BIDRAG,
        innkreving = true,
        status = status,
        saksnummer = "",
    )

    companion object {
        private const val NYTT_BARN_IDENT = "01011512345"
    }
}
