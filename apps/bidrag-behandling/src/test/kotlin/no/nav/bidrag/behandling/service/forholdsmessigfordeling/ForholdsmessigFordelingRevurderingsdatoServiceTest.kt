package no.nav.bidrag.behandling.service.forholdsmessigfordeling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import no.nav.bidrag.behandling.consumer.BidragBBMConsumer
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.Rolle
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordeling
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordelingRolle
import no.nav.bidrag.behandling.database.datamodell.json.ForholdsmessigFordelingSøknadBarn
import no.nav.bidrag.behandling.dto.v2.forholdsmessigfordeling.OppdaterRevurderingsdatoFFRequest
import no.nav.bidrag.behandling.service.VirkningstidspunktService
import no.nav.bidrag.behandling.utils.testdata.opprettGyldigBehandlingForBeregningOgVedtak
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
import no.nav.bidrag.transport.behandling.beregning.felles.OppdaterSøknadFomDatoRequest
import no.nav.bidrag.transport.behandling.beregning.felles.PartISøknad
import no.nav.bidrag.transport.behandling.hendelse.BehandlingStatusType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate

class ForholdsmessigFordelingRevurderingsdatoServiceTest {
    @MockK(relaxed = true)
    lateinit var bbmConsumer: BidragBBMConsumer

    @MockK(relaxed = true)
    lateinit var søknadService: ForholdsmessigFordelingSøknadService

    @MockK(relaxed = true)
    lateinit var virkningstidspunktService: VirkningstidspunktService

    private lateinit var service: ForholdsmessigFordelingRevurderingsdatoService
    private lateinit var behandling: Behandling
    private lateinit var barn1: Rolle
    private lateinit var barn2: Rolle
    private lateinit var tidligsteDato: LocalDate
    private lateinit var opprinneligDato: LocalDate

    companion object {
        private const val FF_SØKNADSID = 100L
        private const val NY_SØKNADSID = 200L
    }

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        service = ForholdsmessigFordelingRevurderingsdatoService(bbmConsumer, søknadService, virkningstidspunktService)
        behandling =
            opprettGyldigBehandlingForBeregningOgVedtak(
                generateId = true,
                typeBehandling = TypeBehandling.BIDRAG,
                andreBarn = listOf(testdataBarn2),
            )
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(erHovedbehandling = true)
        tidligsteDato = behandling.søktFomDato.withDayOfMonth(1)
        opprinneligDato = tidligsteDato.plusMonths(6)
        barn1 = behandling.søknadsbarn[0]
        barn2 = behandling.søknadsbarn[1]
        listOf(barn1, barn2).forEach {
            it.stønadstype = Stønadstype.BIDRAG
            it.forholdsmessigFordeling = opprettFFRolle(ffSøknad(FF_SØKNADSID, opprinneligDato))
        }
        behandling.bidragspliktig!!.forholdsmessigFordeling = opprettFFRolle(ffSøknad(FF_SØKNADSID, opprinneligDato))
        behandling.bidragsmottaker!!.forholdsmessigFordeling = opprettFFRolle(ffSøknad(FF_SØKNADSID, opprinneligDato))

        every { bbmConsumer.hentSøknad(FF_SØKNADSID) } returns HentSøknadResponse(hentSøknad(listOf(barn1, barn2)))
        every { søknadService.feilregistrerBarnFraSøknad(any(), any()) } returns FF_SØKNADSID
        every {
            søknadService.opprettSøknad(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers { ffSøknad(NY_SØKNADSID, arg(6)) }
    }

    @Test
    fun `skal oppdatere søkt fra dato i søknaden når alle barn i søknaden får samme nye dato`() {
        // Given
        val nyDato = opprinneligDato.plusMonths(2)

        // When
        service.oppdaterRevurderingsdato(behandling, request(barn1 to nyDato, barn2 to nyDato.plusDays(10)))

        // Then
        verify(exactly = 1) { bbmConsumer.oppdaterSøknadFomDato(OppdaterSøknadFomDatoRequest(FF_SØKNADSID, nyDato)) }
        verify(exactly = 0) { søknadService.opprettSøknad(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { søknadService.feilregistrerBarnFraSøknad(any(), any()) }
        behandling.roller.forEach { rolle ->
            rolle.forholdsmessigFordeling!!.søknader.single().søknadFomDato shouldBe nyDato
        }
        listOf(barn1, barn2).forEach { barn ->
            barn.forholdsmessigFordeling!!.revurderingsdatoVedOpprettelseAvFF shouldBe nyDato
            verify(exactly = 1) { virkningstidspunktService.oppdaterVirkningstidspunkt(barn.id, nyDato, behandling, true) }
        }
        verify(exactly = 1) { søknadService.knyttSammenManglendeSøknadsknytningerIBehandling(behandling) }
    }

    @Test
    fun `skal flytte barnet til ny søknad når bare ett av flere barn i søknaden endres`() {
        // Given
        val nyDato = opprinneligDato.plusMonths(3)

        // When
        service.oppdaterRevurderingsdato(behandling, request(barn1 to nyDato))

        // Then
        verify(exactly = 0) { bbmConsumer.oppdaterSøknadFomDato(any()) }
        verify(exactly = 1) {
            søknadService.opprettSøknad(
                behandling.bidragspliktig!!.ident!!,
                match { it.map { k -> k.kravhaver } == listOf(barn1.ident) },
                behandling.saksnummer,
                behandling,
                any(),
                any(),
                nyDato,
                true,
                behandling.bidragsmottaker!!.ident!!,
                any(),
            )
        }
        verify(exactly = 1) { søknadService.feilregistrerBarnFraSøknad(barn1, FF_SØKNADSID) }
        barn1.forholdsmessigFordeling!!.søknader.mapNotNull { it.søknadsid } shouldContainExactlyInAnyOrder listOf(FF_SØKNADSID, NY_SØKNADSID)
        barn2.forholdsmessigFordeling!!.søknader.mapNotNull { it.søknadsid } shouldContainExactlyInAnyOrder listOf(FF_SØKNADSID)
        behandling.bidragspliktig!!.forholdsmessigFordeling!!.søknader.mapNotNull { it.søknadsid } shouldContainExactlyInAnyOrder
            listOf(FF_SØKNADSID, NY_SØKNADSID)
        behandling.bidragsmottaker!!.forholdsmessigFordeling!!.søknader.mapNotNull { it.søknadsid } shouldContainExactlyInAnyOrder
            listOf(FF_SØKNADSID, NY_SØKNADSID)
        verify(exactly = 1) { virkningstidspunktService.oppdaterVirkningstidspunkt(barn1.id, nyDato, behandling, true) }
        verify(exactly = 0) { virkningstidspunktService.oppdaterVirkningstidspunkt(barn2.id, any(), any(), any()) }
    }

    @Test
    fun `skal oppdatere søknaden for ett barn og flytte det andre når alle barn får ulike datoer`() {
        // Given
        val nyDatoBarn1 = opprinneligDato.plusMonths(1)
        val nyDatoBarn2 = opprinneligDato.plusMonths(4)

        // When
        service.oppdaterRevurderingsdato(behandling, request(barn1 to nyDatoBarn1, barn2 to nyDatoBarn2))

        // Then
        verify(exactly = 1) { bbmConsumer.oppdaterSøknadFomDato(OppdaterSøknadFomDatoRequest(FF_SØKNADSID, nyDatoBarn1)) }
        verify(exactly = 1) {
            søknadService.opprettSøknad(any(), match { it.single().kravhaver == barn2.ident }, any(), any(), any(), any(), nyDatoBarn2, any(), any(), any())
        }
        verify(exactly = 1) { søknadService.feilregistrerBarnFraSøknad(barn2, FF_SØKNADSID) }
        verify(exactly = 0) { søknadService.feilregistrerBarnFraSøknad(barn1, any()) }
    }

    @Test
    fun `skal ikke gjøre noe når revurderingsdatoen ikke er endret`() {
        // When
        service.oppdaterRevurderingsdato(behandling, request(barn1 to opprinneligDato.plusDays(14)))

        // Then
        verify(exactly = 0) { bbmConsumer.hentSøknad(any()) }
        verify(exactly = 0) { bbmConsumer.oppdaterSøknadFomDato(any()) }
        verify(exactly = 0) { virkningstidspunktService.oppdaterVirkningstidspunkt(any(), any(), any(), any()) }
    }

    @Test
    fun `skal sette revurderingsdato til tidligste tillatte dato når valgt dato er tidligere`() {
        // When
        service.oppdaterRevurderingsdato(behandling, request(barn1 to tidligsteDato.minusYears(1), barn2 to tidligsteDato.minusMonths(1)))

        // Then
        verify(exactly = 1) { bbmConsumer.oppdaterSøknadFomDato(OppdaterSøknadFomDatoRequest(FF_SØKNADSID, tidligsteDato)) }
        barn1.forholdsmessigFordeling!!.revurderingsdatoVedOpprettelseAvFF shouldBe tidligsteDato
    }

    @Test
    fun `skal feile når behandlingen er klage`() {
        behandling.vedtakstype = Vedtakstype.KLAGE

        shouldThrow<HttpClientErrorException> {
            service.oppdaterRevurderingsdato(behandling, request(barn1 to opprinneligDato.plusMonths(1)))
        }
        verify(exactly = 0) { bbmConsumer.oppdaterSøknadFomDato(any()) }
    }

    @Test
    fun `skal feile når vedtak er fattet`() {
        behandling.vedtaksid = 1

        shouldThrow<HttpClientErrorException> {
            service.oppdaterRevurderingsdato(behandling, request(barn1 to opprinneligDato.plusMonths(1)))
        }
    }

    @Test
    fun `skal feile når barnet ikke er revurderingsbarn`() {
        barn1.forholdsmessigFordeling!!.erRevurdering = false

        shouldThrow<HttpClientErrorException> {
            service.oppdaterRevurderingsdato(behandling, request(barn1 to opprinneligDato.plusMonths(1)))
        }
    }

    @Test
    fun `skal feile når barnet ikke har systemopprettet FF-søknad`() {
        barn1.forholdsmessigFordeling!!.søknader.single().søktAvType = SøktAvType.BIDRAGSMOTTAKER

        shouldThrow<HttpClientErrorException> {
            service.oppdaterRevurderingsdato(behandling, request(barn1 to opprinneligDato.plusMonths(1)))
        }
    }

    private fun request(vararg barn: Pair<Rolle, LocalDate>) = OppdaterRevurderingsdatoFFRequest(
        barn.map { (rolle, dato) -> OppdaterRevurderingsdatoFFRequest.OppdaterRevurderingsdatoBarn(rolle.ident!!, rolle.stønadstype!!, dato) },
    )

    private fun opprettFFRolle(søknad: ForholdsmessigFordelingSøknadBarn) = ForholdsmessigFordelingRolle(
        tilhørerSak = behandling.saksnummer,
        behandlerenhet = behandling.behandlerEnhet,
        delAvOpprinneligBehandling = false,
        erRevurdering = true,
        bidragsmottaker = behandling.bidragsmottaker!!.ident,
        søknader = mutableSetOf(søknad),
    )

    private fun ffSøknad(
        søknadsid: Long,
        søknadFomDato: LocalDate,
    ) = ForholdsmessigFordelingSøknadBarn(
        mottattDato = LocalDate.parse("2024-01-01"),
        søknadFomDato = søknadFomDato,
        søktAvType = SøktAvType.AUTOMATISK,
        søknadsid = søknadsid,
        behandlingstype = Behandlingstype.FORHOLDSMESSIG_FORDELING,
        behandlingstema = Behandlingstema.BIDRAG,
        saksnummer = behandling.saksnummer,
        status = Behandlingstatus.UNDER_BEHANDLING,
    )

    private fun hentSøknad(barn: List<Rolle>) = HentSøknad(
        søknadsid = FF_SØKNADSID,
        søknadMottattDato = LocalDate.parse("2024-01-01"),
        søknadFomDato = opprinneligDato,
        behandlingstema = Behandlingstema.BIDRAG,
        behandlingstype = Behandlingstype.FORHOLDSMESSIG_FORDELING,
        saksnummer = behandling.saksnummer,
        innkreving = true,
        søktAvType = SøktAvType.AUTOMATISK,
        behandlingStatusType = BehandlingStatusType.UNDER_BEHANDLING,
        partISøknadListe =
        barn.map { PartISøknad(personident = it.ident, rolletype = Rolletype.BARN, behandlingstatus = Behandlingstatus.UNDER_BEHANDLING) } +
            PartISøknad(personident = behandling.bidragsmottaker!!.ident, rolletype = Rolletype.BIDRAGSMOTTAKER),
    )
}
