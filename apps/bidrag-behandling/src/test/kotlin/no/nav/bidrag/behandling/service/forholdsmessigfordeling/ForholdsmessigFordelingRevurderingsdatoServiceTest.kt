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
import no.nav.bidrag.behandling.utils.testdata.TestDataPerson
import no.nav.bidrag.behandling.utils.testdata.opprettGyldigBehandlingForBeregningOgVedtak
import no.nav.bidrag.behandling.utils.testdata.testdataBarn2
import no.nav.bidrag.behandling.utils.testdata.testdataBarnBm
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
import no.nav.bidrag.transport.behandling.beregning.felles.LeggTilBarnIFFSøknadRequest
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
        private const val EKSISTERENDE_SØKNADSID = 300L
        private const val ANNEN_SAK_SØKNADSID = 400L
        private const val ANNEN_SAK = "2400000"
        private const val BM2_IDENT = "01018012345"
    }

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        service = ForholdsmessigFordelingRevurderingsdatoService(bbmConsumer, søknadService, virkningstidspunktService)
        settOppBehandling(testdataBarn2)
        every { søknadService.feilregistrerBarnFraSøknad(any(), any()) } returns FF_SØKNADSID
        every {
            søknadService.opprettSøknad(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers { ffSøknad(NY_SØKNADSID, arg(6)) }
    }

    /** Alle søknadsbarn, BP og BM er i samme FF-søknad med [opprinneligDato] */
    private fun settOppBehandling(vararg andreBarn: TestDataPerson) {
        behandling =
            opprettGyldigBehandlingForBeregningOgVedtak(
                generateId = true,
                typeBehandling = TypeBehandling.BIDRAG,
                andreBarn = andreBarn.toList(),
            )
        behandling.forholdsmessigFordeling = ForholdsmessigFordeling(erHovedbehandling = true)
        tidligsteDato = behandling.søktFomDato.withDayOfMonth(1)
        opprinneligDato = tidligsteDato.plusMonths(6)
        barn1 = behandling.søknadsbarn[0]
        barn2 = behandling.søknadsbarn[1]
        behandling.søknadsbarn.forEach {
            it.stønadstype = Stønadstype.BIDRAG
            it.forholdsmessigFordeling = opprettFFRolle(ffSøknad(FF_SØKNADSID, opprinneligDato))
        }
        behandling.bidragspliktig!!.forholdsmessigFordeling = opprettFFRolle(ffSøknad(FF_SØKNADSID, opprinneligDato))
        behandling.bidragsmottaker!!.forholdsmessigFordeling = opprettFFRolle(ffSøknad(FF_SØKNADSID, opprinneligDato))

        every { bbmConsumer.hentSøknad(FF_SØKNADSID) } returns HentSøknadResponse(hentSøknad(behandling.søknadsbarn))
    }

    /**
     * Barn 1 og barn 2 er i søknad [FF_SØKNADSID] i behandlingens sak med BM 1.
     * Barn 3 er i søknad [ANNEN_SAK_SØKNADSID] i [ANNEN_SAK] med BM 2.
     */
    private fun settOppBehandlingMedToSaker(): Pair<Rolle, Rolle> {
        settOppBehandling(testdataBarn2, testdataBarnBm)
        val barn3 = behandling.søknadsbarn[2]
        barn3.forholdsmessigFordeling = opprettFFRolle(ffSøknad(ANNEN_SAK_SØKNADSID, opprinneligDato, ANNEN_SAK), ANNEN_SAK, BM2_IDENT)
        val bm2 =
            Rolle(
                behandling = behandling,
                rolletype = Rolletype.BIDRAGSMOTTAKER,
                ident = BM2_IDENT,
                fødselsdato = LocalDate.parse("1980-01-01"),
                id = 999,
                forholdsmessigFordeling = opprettFFRolle(ffSøknad(ANNEN_SAK_SØKNADSID, opprinneligDato, ANNEN_SAK), ANNEN_SAK, null),
            )
        behandling.roller = (behandling.roller + bm2).toMutableSet()
        behandling.bidragspliktig!!.forholdsmessigFordeling!!.søknader.add(ffSøknad(ANNEN_SAK_SØKNADSID, opprinneligDato, ANNEN_SAK))

        every { bbmConsumer.hentSøknad(FF_SØKNADSID) } returns HentSøknadResponse(hentSøknad(listOf(barn1, barn2)))
        every { bbmConsumer.hentSøknad(ANNEN_SAK_SØKNADSID) } returns
            HentSøknadResponse(hentSøknad(listOf(barn3), ANNEN_SAK_SØKNADSID, ANNEN_SAK, BM2_IDENT))
        return barn3 to bm2
    }

    private fun Rolle.ffSøknadsider() = forholdsmessigFordeling!!.søknader.mapNotNull { it.søknadsid }

    @Test
    fun `skal oppdatere søkt fra dato i søknaden når alle barn i søknaden får samme revurderingsdato`() {
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
    fun `skal flytte barnet til ny søknad når bare ett av barna i søknaden endres når søknaden har flere barn`() {
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
    fun `skal flytte barnet til eksisterende FF søknad med samme søkt fra dato i stedet for å opprette ny søknad`() {
        // Given
        val nyDato = opprinneligDato.plusMonths(3)
        val eksisterendeSøknad = ffSøknad(EKSISTERENDE_SØKNADSID, nyDato)
        listOf(barn2, behandling.bidragspliktig!!, behandling.bidragsmottaker!!).forEach {
            it.forholdsmessigFordeling!!.søknader.add(eksisterendeSøknad.copy())
        }

        // When
        service.oppdaterRevurderingsdato(behandling, request(barn1 to nyDato))

        // Then
        verify(exactly = 0) { søknadService.opprettSøknad(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 1) { bbmConsumer.leggTilBarnISøknad(LeggTilBarnIFFSøknadRequest(EKSISTERENDE_SØKNADSID, barn1.ident!!)) }
        verify(exactly = 1) { søknadService.feilregistrerBarnFraSøknad(barn1, FF_SØKNADSID) }
        barn1.forholdsmessigFordeling!!.søknader.mapNotNull { it.søknadsid } shouldContainExactlyInAnyOrder listOf(FF_SØKNADSID, EKSISTERENDE_SØKNADSID)
        behandling.bidragspliktig!!.forholdsmessigFordeling!!.søknader.mapNotNull { it.søknadsid } shouldContainExactlyInAnyOrder
            listOf(FF_SØKNADSID, EKSISTERENDE_SØKNADSID)
    }

    @Test
    fun `skal oppdatere søknaden for ett barn og flytte de andre til ny søknad når alle barn får ulike datoer`() {
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
    fun `skal flytte to barn til samme nye søknad når to av tre barn får samme revurderingsdato`() {
        // Given
        settOppBehandling(testdataBarn2, testdataBarnBm)
        val barn3 = behandling.søknadsbarn[2]
        val nyDato = opprinneligDato.plusMonths(3)

        // When
        service.oppdaterRevurderingsdato(behandling, request(barn1 to nyDato, barn2 to nyDato))

        // Then
        verify(exactly = 0) { bbmConsumer.oppdaterSøknadFomDato(any()) }
        verify(exactly = 1) {
            søknadService.opprettSøknad(
                any(),
                match { kravhavere -> kravhavere.map { it.kravhaver }.toSet() == setOf(barn1.ident, barn2.ident) },
                any(),
                any(),
                any(),
                any(),
                nyDato,
                any(),
                any(),
                any(),
            )
        }
        verify(exactly = 1) { søknadService.feilregistrerBarnFraSøknad(barn1, FF_SØKNADSID) }
        verify(exactly = 1) { søknadService.feilregistrerBarnFraSøknad(barn2, FF_SØKNADSID) }
        verify(exactly = 0) { søknadService.feilregistrerBarnFraSøknad(barn3, any()) }
        barn1.ffSøknadsider() shouldContainExactlyInAnyOrder listOf(FF_SØKNADSID, NY_SØKNADSID)
        barn2.ffSøknadsider() shouldContainExactlyInAnyOrder listOf(FF_SØKNADSID, NY_SØKNADSID)
        barn3.ffSøknadsider() shouldContainExactlyInAnyOrder listOf(FF_SØKNADSID)
        barn3.forholdsmessigFordeling!!.søknader.single().søknadFomDato shouldBe opprinneligDato
        verify(exactly = 0) { virkningstidspunktService.oppdaterVirkningstidspunkt(barn3.id, any(), any(), any()) }
    }

    @Test
    fun `skal flytte to barn til eksisterende søknad med samme dato når to av tre barn får samme revurderingsdato`() {
        // Given
        settOppBehandling(testdataBarn2, testdataBarnBm)
        val barn3 = behandling.søknadsbarn[2]
        val nyDato = opprinneligDato.plusMonths(3)
        val eksisterendeSøknad = ffSøknad(EKSISTERENDE_SØKNADSID, nyDato)
        listOf(behandling.bidragspliktig!!, behandling.bidragsmottaker!!).forEach {
            it.forholdsmessigFordeling!!.søknader.add(eksisterendeSøknad.copy())
        }

        // When
        service.oppdaterRevurderingsdato(behandling, request(barn1 to nyDato, barn2 to nyDato))

        // Then
        verify(exactly = 0) { søknadService.opprettSøknad(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 1) { bbmConsumer.leggTilBarnISøknad(LeggTilBarnIFFSøknadRequest(EKSISTERENDE_SØKNADSID, barn1.ident!!)) }
        verify(exactly = 1) { bbmConsumer.leggTilBarnISøknad(LeggTilBarnIFFSøknadRequest(EKSISTERENDE_SØKNADSID, barn2.ident!!)) }
        barn1.ffSøknadsider() shouldContainExactlyInAnyOrder listOf(FF_SØKNADSID, EKSISTERENDE_SØKNADSID)
        barn2.ffSøknadsider() shouldContainExactlyInAnyOrder listOf(FF_SØKNADSID, EKSISTERENDE_SØKNADSID)
        barn3.ffSøknadsider() shouldContainExactlyInAnyOrder listOf(FF_SØKNADSID)
    }

    @Test
    fun `skal oppdatere søknaden og flytte ett barn når alle tre barn endres og to får samme revurderingsdato`() {
        // Given
        settOppBehandling(testdataBarn2, testdataBarnBm)
        val barn3 = behandling.søknadsbarn[2]
        val datoFlestBarn = opprinneligDato.plusMonths(2)
        val datoBarn3 = opprinneligDato.plusMonths(5)

        // When
        service.oppdaterRevurderingsdato(behandling, request(barn1 to datoFlestBarn, barn2 to datoFlestBarn, barn3 to datoBarn3))

        // Then
        verify(exactly = 1) { bbmConsumer.oppdaterSøknadFomDato(OppdaterSøknadFomDatoRequest(FF_SØKNADSID, datoFlestBarn)) }
        verify(exactly = 1) {
            søknadService.opprettSøknad(any(), match { it.single().kravhaver == barn3.ident }, any(), any(), any(), any(), datoBarn3, any(), any(), any())
        }
        verify(exactly = 1) { søknadService.feilregistrerBarnFraSøknad(barn3, FF_SØKNADSID) }
        verify(exactly = 0) { søknadService.feilregistrerBarnFraSøknad(barn1, any()) }
        verify(exactly = 0) { søknadService.feilregistrerBarnFraSøknad(barn2, any()) }
        barn1.ffSøknadsider() shouldContainExactlyInAnyOrder listOf(FF_SØKNADSID)
        barn2.ffSøknadsider() shouldContainExactlyInAnyOrder listOf(FF_SØKNADSID)
    }

    @Test
    fun `skal ikke flytte barnet til søknad med samme dato i en annen sak`() {
        // Given
        val (_, bm2) = settOppBehandlingMedToSaker()
        val nyDato = opprinneligDato.plusMonths(3)
        val søknadIAnnenSak = ffSøknad(EKSISTERENDE_SØKNADSID, nyDato, ANNEN_SAK)
        listOf(behandling.bidragspliktig!!, bm2).forEach { it.forholdsmessigFordeling!!.søknader.add(søknadIAnnenSak.copy()) }

        // When
        service.oppdaterRevurderingsdato(behandling, request(barn1 to nyDato))

        // Then
        verify(exactly = 0) { bbmConsumer.leggTilBarnISøknad(any()) }
        verify(exactly = 1) {
            søknadService.opprettSøknad(
                any(),
                match { kravhavere -> kravhavere.single().let { it.kravhaver == barn1.ident && it.saksnummer == behandling.saksnummer } },
                behandling.saksnummer,
                any(),
                any(),
                any(),
                nyDato,
                any(),
                behandling.bidragsmottaker!!.ident!!,
                any(),
            )
        }
        behandling.bidragsmottaker!!.ffSøknadsider() shouldContainExactlyInAnyOrder listOf(FF_SØKNADSID, NY_SØKNADSID)
        bm2.ffSøknadsider() shouldContainExactlyInAnyOrder listOf(ANNEN_SAK_SØKNADSID, EKSISTERENDE_SØKNADSID)
    }

    @Test
    fun `skal behandle søknaden i hver sak for seg når barn i ulike saker får samme revurderingsdato`() {
        // Given
        val (barn3, bm2) = settOppBehandlingMedToSaker()
        val nyDato = opprinneligDato.plusMonths(3)

        // When
        service.oppdaterRevurderingsdato(behandling, request(barn3 to nyDato, barn1 to nyDato))

        // Then
        verify(exactly = 1) { bbmConsumer.oppdaterSøknadFomDato(OppdaterSøknadFomDatoRequest(ANNEN_SAK_SØKNADSID, nyDato)) }
        verify(exactly = 0) { bbmConsumer.oppdaterSøknadFomDato(match { it.søknadsid == FF_SØKNADSID }) }
        verify(exactly = 0) { bbmConsumer.leggTilBarnISøknad(any()) }
        verify(exactly = 1) {
            søknadService.opprettSøknad(
                any(),
                match { it.single().kravhaver == barn1.ident },
                behandling.saksnummer,
                any(),
                any(),
                any(),
                nyDato,
                any(),
                behandling.bidragsmottaker!!.ident!!,
                any(),
            )
        }
        verify(exactly = 1) { søknadService.feilregistrerBarnFraSøknad(barn1, FF_SØKNADSID) }
        verify(exactly = 0) { søknadService.feilregistrerBarnFraSøknad(barn3, any()) }
        barn3.ffSøknadsider() shouldContainExactlyInAnyOrder listOf(ANNEN_SAK_SØKNADSID)
        bm2.ffSøknadsider() shouldContainExactlyInAnyOrder listOf(ANNEN_SAK_SØKNADSID)
        bm2.forholdsmessigFordeling!!.søknader.single().søknadFomDato shouldBe nyDato
        behandling.bidragsmottaker!!.ffSøknadsider() shouldContainExactlyInAnyOrder listOf(FF_SØKNADSID, NY_SØKNADSID)
    }

    @Test
    fun `skal feile når barnets FF-søknad tilhører en annen sak enn barnet`() {
        // Given
        barn1.forholdsmessigFordeling!!.søknader.single().let { barn1.forholdsmessigFordeling!!.søknader.remove(it) }
        barn1.forholdsmessigFordeling!!.søknader.add(ffSøknad(ANNEN_SAK_SØKNADSID, opprinneligDato, ANNEN_SAK))

        // When / Then
        shouldThrow<HttpClientErrorException> {
            service.oppdaterRevurderingsdato(behandling, request(barn1 to opprinneligDato.plusMonths(1)))
        }
        verify(exactly = 0) { bbmConsumer.hentSøknad(any()) }
    }

    @Test
    fun `skal ikke gjøre noe endringer når revurderingsdatoen ikke er endret`() {
        // When
        service.oppdaterRevurderingsdato(behandling, request(barn1 to opprinneligDato.plusDays(14)))

        // Then
        verify(exactly = 0) { bbmConsumer.hentSøknad(any()) }
        verify(exactly = 0) { bbmConsumer.oppdaterSøknadFomDato(any()) }
        verify(exactly = 0) { virkningstidspunktService.oppdaterVirkningstidspunkt(any(), any(), any(), any()) }
    }

    @Test
    fun `skal feile hvis når ny revurderingsdato settes til tidligere tidspunkt enn tidligste tillatte revurderingsdato`() {
        // When
        shouldThrow<HttpClientErrorException> {
            service.oppdaterRevurderingsdato(behandling, request(barn1 to tidligsteDato.minusYears(1), barn2 to tidligsteDato.minusMonths(1)))
        }
    }

    @Test
    fun `skal ikke kunne endre revurderingsdato hvis vedtak er fattet`() {
        behandling.vedtaksid = 1

        shouldThrow<HttpClientErrorException> {
            service.oppdaterRevurderingsdato(behandling, request(barn1 to opprinneligDato.plusMonths(1)))
        }
    }

    @Test
    fun `skal ikke kunne endre revurderingdsato for søknadsbarn`() {
        barn1.forholdsmessigFordeling!!.erRevurdering = false

        shouldThrow<HttpClientErrorException> {
            service.oppdaterRevurderingsdato(behandling, request(barn1 to opprinneligDato.plusMonths(1)))
        }
    }

    @Test
    fun `skal feile når barnet ikke har FF-søknad`() {
        barn1.forholdsmessigFordeling!!.søknader.single().behandlingstype = Behandlingstype.ENDRING
        shouldThrow<HttpClientErrorException> {
            service.oppdaterRevurderingsdato(behandling, request(barn1 to opprinneligDato.plusMonths(1)))
        }
    }

    private fun request(vararg barn: Pair<Rolle, LocalDate>) = OppdaterRevurderingsdatoFFRequest(
        barn.map { (rolle, dato) -> OppdaterRevurderingsdatoFFRequest.OppdaterRevurderingsdatoBarn(rolle.ident!!, rolle.stønadstype!!, dato) },
    )

    private fun opprettFFRolle(
        søknad: ForholdsmessigFordelingSøknadBarn,
        tilhørerSak: String = behandling.saksnummer,
        bidragsmottaker: String? = behandling.bidragsmottaker!!.ident,
    ) = ForholdsmessigFordelingRolle(
        tilhørerSak = tilhørerSak,
        behandlerenhet = behandling.behandlerEnhet,
        delAvOpprinneligBehandling = false,
        erRevurdering = true,
        bidragsmottaker = bidragsmottaker,
        søknader = mutableSetOf(søknad),
    )

    private fun ffSøknad(
        søknadsid: Long,
        søknadFomDato: LocalDate,
        saksnummer: String = behandling.saksnummer,
    ) = ForholdsmessigFordelingSøknadBarn(
        mottattDato = LocalDate.parse("2024-01-01"),
        søknadFomDato = søknadFomDato,
        søktAvType = SøktAvType.AUTOMATISK,
        søknadsid = søknadsid,
        behandlingstype = Behandlingstype.FORHOLDSMESSIG_FORDELING,
        behandlingstema = Behandlingstema.BIDRAG,
        saksnummer = saksnummer,
        status = Behandlingstatus.UNDER_BEHANDLING,
    )

    private fun hentSøknad(
        barn: List<Rolle>,
        søknadsid: Long = FF_SØKNADSID,
        saksnummer: String = behandling.saksnummer,
        bmIdent: String? = behandling.bidragsmottaker!!.ident,
    ) = HentSøknad(
        søknadsid = søknadsid,
        søknadMottattDato = LocalDate.parse("2024-01-01"),
        søknadFomDato = opprinneligDato,
        behandlingstema = Behandlingstema.BIDRAG,
        behandlingstype = Behandlingstype.FORHOLDSMESSIG_FORDELING,
        saksnummer = saksnummer,
        innkreving = true,
        søktAvType = SøktAvType.AUTOMATISK,
        behandlingStatusType = BehandlingStatusType.UNDER_BEHANDLING,
        partISøknadListe =
        barn.map { PartISøknad(personident = it.ident, rolletype = Rolletype.BARN, behandlingstatus = Behandlingstatus.UNDER_BEHANDLING) } +
            PartISøknad(personident = bmIdent, rolletype = Rolletype.BIDRAGSMOTTAKER),
    )
}
