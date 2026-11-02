package no.nav.bidrag.behandling.service.forholdsmessigfordeling

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import no.nav.bidrag.behandling.consumer.BidragBBMConsumer
import no.nav.bidrag.behandling.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.behandling.consumer.BidragSakConsumer
import no.nav.bidrag.behandling.database.datamodell.Behandling
import no.nav.bidrag.behandling.database.datamodell.PrivatAvtale
import no.nav.bidrag.behandling.database.datamodell.Rolle
import no.nav.bidrag.behandling.database.repository.BehandlingRepository
import no.nav.bidrag.domene.enums.sak.Bidragssakstatus
import no.nav.bidrag.domene.enums.sak.Sakskategori
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.beregning.felles.HentSøknad
import no.nav.bidrag.transport.sak.BidragssakDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

class ForholdsmessigFordelingKravhaverServiceTest {
    @MockK
    lateinit var sakConsumer: BidragSakConsumer

    @MockK
    lateinit var behandlingRepository: BehandlingRepository

    @MockK
    lateinit var beløpshistorikkConsumer: BidragBeløpshistorikkConsumer

    @MockK
    lateinit var bbmConsumer: BidragBBMConsumer

    private lateinit var service: ForholdsmessigFordelingKravhaverService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        service =
            spyk(
                ForholdsmessigFordelingKravhaverService(
                    sakConsumer = sakConsumer,
                    behandlingRepository = behandlingRepository,
                    beløpshistorikkConsumer = beløpshistorikkConsumer,
                    bbmConsumer = bbmConsumer,
                ),
            )
    }

    @Test
    fun `hentAlleRelevanteKravhavere skal inkludere resultat fra andre oppslag`() {
        val behandling = mockk<Behandling>()

        val førsteOppslag =
            setOf(
                sakKravhaver("barn1", Stønadstype.BIDRAG),
                sakKravhaver("barn2", Stønadstype.BIDRAG18AAR),
            )
        val utenLøpende = listOf(sakKravhaver("barn3", Stønadstype.BIDRAG))
        val mellomresultat = førsteOppslag + utenLøpende
        val andreOppslag = setOf(sakKravhaver("barn4", Stønadstype.BIDRAG))

        every {
            service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, null)
        } returns førsteOppslag
        every {
            service.hentBarnUtenLøpendeBidrag(behandling, førsteOppslag)
        } returns utenLøpende
        every {
            service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, mellomresultat)
        } returns andreOppslag

        val resultat = service.hentAlleRelevanteKravhavere(behandling)

        resultat.shouldContainExactlyInAnyOrder(førsteOppslag + utenLøpende + andreOppslag)
        verify(exactly = 1) { service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, null) }
        verify(exactly = 1) { service.hentBarnUtenLøpendeBidrag(behandling, førsteOppslag) }
        verify(exactly = 1) { service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, mellomresultat) }
    }

    @Test
    fun `hentAlleRelevanteKravhavere skal deduplisere når andre oppslag returnerer eksisterende kravhaver`() {
        val behandling = mockk<Behandling>()

        val kravhaverFraFørsteOppslag = sakKravhaver("barn1", Stønadstype.BIDRAG)
        val førsteOppslag = setOf(kravhaverFraFørsteOppslag)
        val utenLøpende = listOf(sakKravhaver("barn2", Stønadstype.BIDRAG18AAR))
        val mellomresultat = førsteOppslag + utenLøpende
        val andreOppslag = setOf(kravhaverFraFørsteOppslag)

        every {
            service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, null)
        } returns førsteOppslag
        every {
            service.hentBarnUtenLøpendeBidrag(behandling, førsteOppslag)
        } returns utenLøpende
        every {
            service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, mellomresultat)
        } returns andreOppslag

        val resultat = service.hentAlleRelevanteKravhavere(behandling)

        resultat.shouldContainExactlyInAnyOrder(førsteOppslag + utenLøpende)
        verify(exactly = 1) { service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, mellomresultat) }
    }

    @Test
    fun `hentAlleRelevanteKravhavere skal inkludere kravhavere fra åpne behandlinger`() {
        val behandling = mockk<Behandling>()
        val behandlingMedBarn1 =
            sakKravhaverMedÅpneBehandlinger(
                "barn1",
                Stønadstype.BIDRAG,
                åpneBehandlinger = setOf(mockk()),
            )

        val førsteOppslag = setOf(behandlingMedBarn1)
        val utenLøpende = emptyList<SakKravhaver>()
        val mellomresultat = førsteOppslag + utenLøpende
        val andreOppslag = emptySet<SakKravhaver>()

        every {
            service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, null)
        } returns førsteOppslag
        every {
            service.hentBarnUtenLøpendeBidrag(behandling, førsteOppslag)
        } returns utenLøpende
        every {
            service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, mellomresultat)
        } returns andreOppslag

        val resultat = service.hentAlleRelevanteKravhavere(behandling)

        resultat.size shouldBe 1
        resultat.first().kravhaver shouldBe "barn1"
        resultat.first().åpneBehandlinger.size shouldBe 1
    }

    @Test
    fun `hentAlleRelevanteKravhavere skal inkludere kravhavere fra åpne søknader`() {
        val behandling = mockk<Behandling>()
        val behandlingMedBarn2 =
            sakKravhaverMedÅpneSøknader(
                "barn2",
                Stønadstype.BIDRAG18AAR,
                åpneSøknader = setOf(mockk<HentSøknad>()),
            )

        val førsteOppslag = setOf(behandlingMedBarn2)
        val utenLøpende = emptyList<SakKravhaver>()
        val mellomresultat = førsteOppslag + utenLøpende
        val andreOppslag = emptySet<SakKravhaver>()

        every {
            service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, null)
        } returns førsteOppslag
        every {
            service.hentBarnUtenLøpendeBidrag(behandling, førsteOppslag)
        } returns utenLøpende
        every {
            service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, mellomresultat)
        } returns andreOppslag

        val resultat = service.hentAlleRelevanteKravhavere(behandling)

        resultat.size shouldBe 1
        resultat.first().kravhaver shouldBe "barn2"
        resultat.first().åpneSøknader.size shouldBe 1
    }

    @Test
    fun `hentAlleRelevanteKravhavere skal inkludere kravhavere fra privat avtale`() {
        val behandling = mockk<Behandling>()
        val privatAvtale = mockk<PrivatAvtale>()
        val behandlingMedPrivatAvtale =
            sakKravhaverMedPrivatAvtale("barn3", Stønadstype.BIDRAG, privatAvtale)

        val førsteOppslag = emptySet<SakKravhaver>()
        val utenLøpende = listOf(behandlingMedPrivatAvtale)
        val mellomresultat = førsteOppslag + utenLøpende
        val andreOppslag = emptySet<SakKravhaver>()

        every {
            service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, null)
        } returns førsteOppslag
        every {
            service.hentBarnUtenLøpendeBidrag(behandling, førsteOppslag)
        } returns utenLøpende
        every {
            service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, mellomresultat)
        } returns andreOppslag

        val resultat = service.hentAlleRelevanteKravhavere(behandling)

        resultat.size shouldBe 1
        resultat.first().kravhaver shouldBe "barn3"
        resultat.first().privatAvtale shouldBe privatAvtale
    }

    @Test
    fun `hentAlleRelevanteKravhavere skal håndtere blanding av behandling, søknad og privat avtale`() {
        val behandling = mockk<Behandling>()
        val privatAvtale = mockk<PrivatAvtale>()

        val fraBehandling =
            sakKravhaverMedÅpneBehandlinger("barn1", Stønadstype.BIDRAG, setOf(mockk()))
        val fraSøknad =
            sakKravhaverMedÅpneSøknader("barn2", Stønadstype.BIDRAG18AAR, setOf(mockk<HentSøknad>()))

        val førsteOppslag = setOf(fraBehandling, fraSøknad)
        val fraPrivatAvtale = sakKravhaverMedPrivatAvtale("barn3", Stønadstype.BIDRAG, privatAvtale)
        val utenLøpende = listOf(fraPrivatAvtale)
        val mellomresultat = førsteOppslag + utenLøpende
        val andreOppslag = emptySet<SakKravhaver>()

        every {
            service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, null)
        } returns førsteOppslag
        every {
            service.hentBarnUtenLøpendeBidrag(behandling, førsteOppslag)
        } returns utenLøpende
        every {
            service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, mellomresultat)
        } returns andreOppslag

        val resultat = service.hentAlleRelevanteKravhavere(behandling)

        resultat.size shouldBe 3
        resultat.map { it.kravhaver }.shouldContainExactlyInAnyOrder("barn1", "barn2", "barn3")
        resultat.find { it.kravhaver == "barn1" }?.åpneBehandlinger?.size shouldBe 1
        resultat.find { it.kravhaver == "barn2" }?.åpneSøknader?.size shouldBe 1
        resultat.find { it.kravhaver == "barn3" }?.privatAvtale shouldBe privatAvtale
    }

    @Test
    fun `hentAlleRelevanteKravhavere skal ikke inkludere duplikater når samme barn og stønadstype kommer fra andre oppslag`() {
        val behandling = mockk<Behandling>()

        val fraBehandling =
            sakKravhaverMedÅpneBehandlinger("barn1", Stønadstype.BIDRAG, setOf(mockk()), saksnummer = "SAK-001")

        val førsteOppslag = setOf(fraBehandling)
        val utenLøpende = emptyList<SakKravhaver>()
        val mellomresultat = førsteOppslag + utenLøpende
        // Andre oppslag returnerer samme barn1 + BIDRAG fra samme saksnummer - skal filtreres ut
        val andreOppslag = setOf(fraBehandling.copy())

        every {
            service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, null)
        } returns førsteOppslag
        every {
            service.hentBarnUtenLøpendeBidrag(behandling, førsteOppslag)
        } returns utenLøpende
        every {
            service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling, mellomresultat)
        } returns andreOppslag

        val resultat = service.hentAlleRelevanteKravhavere(behandling)

        // distinctBy on (saksnummer, distinctKey) => same saksnummer + key = deduplicated (but kept due to distinctBy)
        resultat.size shouldBe 1
    }

    @Nested
    inner class `Filtrering av løpende bidrag på opphørsdato` {
        @BeforeEach
        fun setUpLøpendeBidrag() {
            mockkStatic(MODEL_KT)
            every { finnBeregningsperiodeForKravhavere(any(), any()) } returns ÅrMånedsperiode(LØPER_FRA, null)
            every { behandlingRepository.finnÅpneBidragsbehandlingerForBp(any(), any()) } returns emptyList()
            every { service.hentÅpneSøknader(any(), any(), any(), any()) } returns emptyList()
            every { sakConsumer.hentSak(SAKSNUMMER) } returns bidragssak()
        }

        @AfterEach
        fun tearDown() {
            unmockkStatic(MODEL_KT)
        }

        @Test
        fun `skal ekskludere kravhaver når opphørsdato er før eldste søkt fom dato`() {
            val resultat = hentLøpendeBidragMedOpphørsdato(ELDSTE_SØKT_FOM.minusMonths(1))

            resultat.shouldBeEmpty()
        }

        @Test
        fun `skal ekskludere kravhaver når opphørsdato er lik eldste søkt fom dato`() {
            val resultat = hentLøpendeBidragMedOpphørsdato(ELDSTE_SØKT_FOM)

            resultat.shouldBeEmpty()
        }

        @Test
        fun `skal inkludere kravhaver når opphørsdato er etter eldste søkt fom dato`() {
            val opphørsdato = ELDSTE_SØKT_FOM.plusMonths(1)

            val resultat = hentLøpendeBidragMedOpphørsdato(opphørsdato)

            resultat.map { it.kravhaver to it.opphørsdato } shouldBe listOf(KRAVHAVER to opphørsdato)
        }

        @Test
        fun `skal inkludere kravhaver når opphørsdato mangler`() {
            val resultat = hentLøpendeBidragMedOpphørsdato(null)

            resultat.map { it.kravhaver to it.opphørsdato } shouldBe listOf(KRAVHAVER to null)
        }

        @Test
        fun `skal ekskludere kravhaver når bidraget starter samme måned som det opphører etter eldste søkt fom dato`() {
            val opphørsdato = ELDSTE_SØKT_FOM.plusMonths(3)

            val resultat = hentLøpendeBidragMedOpphørsdato(opphørsdato, løperFra = opphørsdato)

            resultat.shouldBeEmpty()
        }

        private fun hentLøpendeBidragMedOpphørsdato(
            opphørsdato: YearMonth?,
            løperFra: YearMonth = LØPER_FRA,
        ): Set<SakKravhaver> {
            val behandling = behandlingMedEldsteSøktFom()
            every { behandling.finnOpphørsdato(Stønadstype.BIDRAG, KRAVHAVER) } returns opphørsdato
            every { service.hentSisteLøpendeStønader(any(), any()) } returns
                listOf(
                    LøpendeBidragSakPeriode(
                        sak = Saksnummer(SAKSNUMMER),
                        type = Stønadstype.BIDRAG,
                        kravhaver = Personident(KRAVHAVER),
                        valutakode = Valutakode.NOK.name,
                        periodeFra = løperFra,
                        periodeTil = null,
                    ),
                )

            return service.hentAlleÅpneEllerLøpendeBidraggsakerForBP(behandling)
        }

        private fun behandlingMedEldsteSøktFom(): Behandling {
            val bidragspliktig = mockk<Rolle> { every { ident } returns BIDRAGSPLIKTIG }
            return mockk<Behandling>(relaxed = true) {
                every { id } returns BEHANDLING_ID
                every { this@mockk.bidragspliktig } returns bidragspliktig
                every { eldsteSøktFomDato } returns ELDSTE_SØKT_FOM.atDay(1)
                every { omgjøringsdetaljer } returns null
                every { erKlageEllerOmgjøring } returns false
                every { privatAvtale } returns mutableSetOf()
            }
        }

        private fun bidragssak() = BidragssakDto(
            eierfogd = Enhetsnummer(ENHET),
            saksnummer = Saksnummer(SAKSNUMMER),
            saksstatus = Bidragssakstatus.IN,
            kategori = Sakskategori.NASJONAL,
            opprettetDato = ELDSTE_SØKT_FOM.atDay(1),
            levdeAdskilt = false,
            ukjentPart = false,
            roller = emptyList(),
        )
    }

    private fun sakKravhaver(
        kravhaver: String,
        stønadstype: Stønadstype,
    ) = SakKravhaver(
        saksnummer = "SAK-$kravhaver",
        kravhaver = kravhaver,
        stønadstype = stønadstype,
    )

    private fun sakKravhaverMedÅpneBehandlinger(
        kravhaver: String,
        stønadstype: Stønadstype,
        åpneBehandlinger: Set<Behandling>,
        saksnummer: String? = "SAK-$kravhaver",
    ) = SakKravhaver(
        saksnummer = saksnummer,
        kravhaver = kravhaver,
        stønadstype = stønadstype,
        åpneBehandlinger = åpneBehandlinger.toMutableSet(),
    )

    private fun sakKravhaverMedÅpneSøknader(
        kravhaver: String,
        stønadstype: Stønadstype,
        åpneSøknader: Set<HentSøknad>,
        saksnummer: String? = "SAK-$kravhaver",
    ) = SakKravhaver(
        saksnummer = saksnummer,
        kravhaver = kravhaver,
        stønadstype = stønadstype,
        åpneSøknader = åpneSøknader.toMutableSet(),
    )

    private fun sakKravhaverMedPrivatAvtale(
        kravhaver: String,
        stønadstype: Stønadstype,
        privatAvtale: PrivatAvtale,
        saksnummer: String? = null,
    ) = SakKravhaver(
        saksnummer = saksnummer,
        kravhaver = kravhaver,
        stønadstype = stønadstype,
        privatAvtale = privatAvtale,
    )

    companion object {
        private const val MODEL_KT = "no.nav.bidrag.behandling.service.forholdsmessigfordeling.ForholdsmessigFordelingModelKt"
        private const val BEHANDLING_ID = 1L
        private const val BIDRAGSPLIKTIG = "bidragspliktig"
        private const val KRAVHAVER = "kravhaver"
        private const val SAKSNUMMER = "2400001"
        private const val ENHET = "4806"
        private val ELDSTE_SØKT_FOM = YearMonth.of(2025, 6)
        private val LØPER_FRA = YearMonth.of(2024, 1)
    }
}
