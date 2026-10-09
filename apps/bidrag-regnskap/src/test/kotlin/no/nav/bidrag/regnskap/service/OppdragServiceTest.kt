package no.nav.bidrag.regnskap.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.regnskap.UnleashFeatures
import no.nav.bidrag.regnskap.consumer.BidragPersonConsumer
import no.nav.bidrag.regnskap.consumer.BidragSakConsumer
import no.nav.bidrag.regnskap.persistence.entity.Oppdrag
import no.nav.bidrag.regnskap.utils.TestData
import no.nav.bidrag.transport.person.Identgruppe
import no.nav.bidrag.transport.person.PersonidentDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OppdragServiceTest {

    @MockK(relaxed = true)
    private lateinit var persistenceService: PersistenceService

    @MockK(relaxed = true)
    private lateinit var oppdragsperiodeService: OppdragsperiodeService

    @MockK(relaxed = true)
    private lateinit var konteringService: KonteringService

    @MockK(relaxed = true)
    private lateinit var bidragSakConsumer: BidragSakConsumer

    @MockK(relaxed = true)
    private lateinit var personhendelseService: PersonhendelseService

    @MockK(relaxed = true)
    private lateinit var bidragPersonConsumer: BidragPersonConsumer

    @MockK(relaxed = true)
    private lateinit var endreMottakerService: EndreMottakerService

    @InjectMockKs
    private lateinit var oppdragService: OppdragService

    @BeforeEach
    fun aktiverEndreMottaker() {
        mockkObject(UnleashFeaturesProvider)
        every { UnleashFeaturesProvider.isEnabled(UnleashFeatures.ENDRE_MOTTAKER.featureName, false, false) } returns true
    }

    @AfterEach
    fun tilbakestillEndreMottaker() {
        unmockkObject(UnleashFeaturesProvider)
    }

    @Nested
    inner class OpprettOppdrag {

        @Test
        fun `skal opprette oppdrag`() {
            val hendelse = TestData.opprettHendelse()

            every { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any(), any()) } returns TestData.opprettOppdragsperiode()

            val oppdragId = oppdragService.lagreEllerOppdaterOppdrag(null, hendelse, false)

            oppdragId shouldBe 0
        }

        @Test
        fun `skal ikke opprette oppdrag om beløp er null`() {
            val hendelse = TestData.opprettHendelse(periodeListe = listOf(TestData.opprettPeriodeDomene(beløp = null)))

            val oppdragId = oppdragService.lagreEllerOppdaterOppdrag(null, hendelse, false)

            oppdragId shouldBe null
        }

        @Test
        fun `skal ikke opprette oppdag om beløp er 0`() {
            val hendelse = TestData.opprettHendelse(periodeListe = listOf(TestData.opprettPeriodeDomene(beløp = BigDecimal.ZERO)))

            val oppdragId = oppdragService.lagreEllerOppdaterOppdrag(null, hendelse, false)

            oppdragId shouldBe null
        }

        @Test
        fun `skal ikke opprette oppdag om beløp er 0,0`() {
            val hendelse = TestData.opprettHendelse(periodeListe = listOf(TestData.opprettPeriodeDomene(beløp = BigDecimal.valueOf(0.0))))

            val oppdragId = oppdragService.lagreEllerOppdaterOppdrag(null, hendelse, false)

            oppdragId shouldBe null
        }

        @Test
        fun `skal ikke opprette oppdrag eller sende til Elin uten perioder`() {
            val hendelse = TestData.opprettHendelse(periodeListe = emptyList())

            oppdragService.lagreEllerOppdaterOppdrag(null, hendelse, false) shouldBe null

            verify(exactly = 0) { persistenceService.lagreOppdrag(any<Oppdrag>()) }
            verify(exactly = 0) { endreMottakerService.opprettEndreMottaker(any(), any(), any(), any()) }
        }

        @Test
        fun `skal hoppe over mottakerendring for engangsbeløp uten matchende oppdrag`() {
            val hendelse = TestData.opprettHendelse(vedtakType = Vedtakstype.ENDRING_MOTTAKER, periodeListe = emptyList())
            every { persistenceService.hentOppdragPåReferanseOgOmgjørVedtakId("referanse", 8002) } returns null
            val endring = hendelse.copy(referanse = "referanse", omgjørVedtakId = 8002)

            oppdragService.lagreHendelse(endring, true) shouldBe null

            verify(exactly = 0) { persistenceService.lagreOppdrag(any<Oppdrag>()) }
            verify(exactly = 0) { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any(), any()) }
            verify(exactly = 0) { endreMottakerService.opprettEndreMottaker(any(), any(), any(), any()) }
        }
    }

    @Nested
    inner class OppdaterOppdrag {

        @Test
        fun `skal oppdatere oppdrag`() {
            val hendelse = TestData.opprettHendelse()
            val oppdrag = TestData.opprettOppdrag()

            every { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any(), any()) } returns TestData.opprettOppdragsperiode()

            oppdragService.lagreEllerOppdaterOppdrag(oppdrag, hendelse, false)

            verify { persistenceService.lagreOppdrag(oppdrag) }
        }

        @Test
        fun `skal lagre mottakerendring for eksisterende oppdrag uten nye perioder`() {
            val barnIdent = genererFødselsnummer()
            val nyMottaker = genererFødselsnummer()
            val hendelse = TestData.opprettHendelse(
                vedtakType = Vedtakstype.ENDRING_MOTTAKER,
                kravhaverIdent = barnIdent,
                mottakerIdent = nyMottaker,
                periodeListe = emptyList(),
            )
            val oppdrag = TestData.opprettOppdrag(mottakerIdent = genererFødselsnummer())

            oppdragService.lagreEllerOppdaterOppdrag(oppdrag, hendelse, false)

            oppdrag.mottakerIdent shouldBe nyMottaker
            verify(exactly = 0) { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any(), any()) }
            verify(exactly = 0) { persistenceService.finnSisteOverførtePeriode() }
            verify(exactly = 1) { persistenceService.lagreOppdrag(oppdrag) }
            verify(exactly = 1) {
                endreMottakerService.opprettEndreMottaker(hendelse.vedtakId, hendelse.sakId, barnIdent, nyMottaker)
            }
        }

        @Test
        fun `skal oppdatere mottaker på eksisterende engangsbeløp uten periode eller kontering`() {
            val barnIdent = genererFødselsnummer()
            val nyMottaker = genererFødselsnummer()
            val hendelse = TestData.opprettHendelse(
                vedtakType = Vedtakstype.ENDRING_MOTTAKER,
                kravhaverIdent = barnIdent,
                mottakerIdent = nyMottaker,
                periodeListe = emptyList(),
            ).copy(referanse = "referanse", omgjørVedtakId = 8002)
            val oppdrag = TestData.opprettOppdrag(mottakerIdent = genererFødselsnummer())
            every { persistenceService.hentOppdragPåReferanseOgOmgjørVedtakId("referanse", 8002) } returns oppdrag

            oppdragService.lagreHendelse(hendelse, true)

            oppdrag.mottakerIdent shouldBe nyMottaker
            verify(exactly = 1) { persistenceService.lagreOppdrag(oppdrag) }
            verify(exactly = 0) { persistenceService.finnSisteOverførtePeriode() }
            verify(exactly = 0) { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any(), any()) }
            verify(exactly = 0) { konteringService.opprettNyeKonteringerPåOppdragsperiode(any(), any(), any()) }
            verify(exactly = 1) { endreMottakerService.opprettEndreMottaker(hendelse.vedtakId, hendelse.sakId, barnIdent, nyMottaker) }
        }

        @Test
        fun `skal lagre mottakerendring fra andre vedtakstyper med perioder`() {
            val barnIdent = genererFødselsnummer()
            val nyMottaker = genererFødselsnummer()
            val hendelse = TestData.opprettHendelse(kravhaverIdent = barnIdent, mottakerIdent = nyMottaker)
            val oppdrag = TestData.opprettOppdrag(mottakerIdent = genererFødselsnummer())
            every { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any(), any()) } returns TestData.opprettOppdragsperiode()

            oppdragService.lagreEllerOppdaterOppdrag(oppdrag, hendelse, false)

            verify(exactly = 1) { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any(), any()) }
            verify(exactly = 1) {
                endreMottakerService.opprettEndreMottaker(hendelse.vedtakId, hendelse.sakId, barnIdent, nyMottaker)
            }
        }

        @Test
        fun `skal ikke sende mottakerendring naar mottaker er uendret`() {
            val mottaker = genererFødselsnummer()
            val hendelse = TestData.opprettHendelse(mottakerIdent = mottaker, periodeListe = emptyList())
            val oppdrag = TestData.opprettOppdrag(mottakerIdent = mottaker)

            oppdragService.lagreEllerOppdaterOppdrag(oppdrag, hendelse, false)

            verify(exactly = 0) { endreMottakerService.opprettEndreMottaker(any(), any(), any(), any()) }
        }

        @Test
        fun `skal ikke sende mottakerendring naar funksjonen er deaktivert`() {
            every { UnleashFeaturesProvider.isEnabled(UnleashFeatures.ENDRE_MOTTAKER.featureName, false, false) } returns false
            val hendelse = TestData.opprettHendelse(periodeListe = emptyList())
            val oppdrag = TestData.opprettOppdrag(mottakerIdent = genererFødselsnummer())

            oppdragService.lagreEllerOppdaterOppdrag(oppdrag, hendelse, false)

            oppdrag.mottakerIdent shouldBe hendelse.mottakerIdent
            verify(exactly = 0) { endreMottakerService.opprettEndreMottaker(any(), any(), any(), any()) }
        }

        @Test
        fun `skal ikke sende mottakerendring for engangsbelop`() {
            val hendelse = TestData.opprettHendelse(periodeListe = emptyList())
            val oppdrag = TestData.opprettOppdrag(mottakerIdent = genererFødselsnummer())

            oppdragService.lagreEllerOppdaterOppdrag(oppdrag, hendelse, true)

            verify(exactly = 0) { endreMottakerService.opprettEndreMottaker(any(), any(), any(), any()) }
        }

        @Test
        fun `skal oppdatere utsatt til dato om ikke satt på oppdrag`() {
            val utsattTilDato = LocalDate.now().plusDays(3)
            val hendelse = TestData.opprettHendelse(utsattTilDato = utsattTilDato)
            val oppdrag = TestData.opprettOppdrag(utsattTilDato = null)

            oppdragService.oppdatererVerdierPåOppdrag(hendelse, oppdrag)

            oppdrag.utsattTilDato shouldBe utsattTilDato
        }

        @Test
        fun `skal oppdatere utsatt til dato om satt på oppdrag men hendelse er lenger frem i tid`() {
            val utsattTilDato = LocalDate.now().plusDays(3)
            val hendelse = TestData.opprettHendelse(utsattTilDato = utsattTilDato)
            val oppdrag = TestData.opprettOppdrag(utsattTilDato = utsattTilDato.minusDays(1))

            oppdragService.oppdatererVerdierPåOppdrag(hendelse, oppdrag)

            oppdrag.utsattTilDato shouldBe utsattTilDato
        }

        @Test
        fun `skal ikke oppdatere utsatt til dato om satt på oppdrag og hendelse er tilbake i tid`() {
            val utsattTilDato = LocalDate.now().plusDays(3)
            val hendelse = TestData.opprettHendelse(utsattTilDato = utsattTilDato)
            val oppdrag = TestData.opprettOppdrag(utsattTilDato = utsattTilDato.plusDays(1))

            oppdragService.oppdatererVerdierPåOppdrag(hendelse, oppdrag)

            oppdrag.utsattTilDato shouldBe utsattTilDato.plusDays(1)
        }

        @Test
        fun `skal ikke oppdatere utsatt til dato om satt på oppdrag og hendelse er null`() {
            val utsattTilDato = LocalDate.now().plusDays(3)
            val hendelse = TestData.opprettHendelse(utsattTilDato = null)
            val oppdrag = TestData.opprettOppdrag(utsattTilDato = utsattTilDato)

            oppdragService.oppdatererVerdierPåOppdrag(hendelse, oppdrag)

            oppdrag.utsattTilDato shouldBe utsattTilDato
        }

        @Test
        fun `skal ikke oppdatere utsatt til dato null på oppdrag og hendelse er null`() {
            val hendelse = TestData.opprettHendelse(utsattTilDato = null)
            val oppdrag = TestData.opprettOppdrag(utsattTilDato = null)

            oppdragService.oppdatererVerdierPåOppdrag(hendelse, oppdrag)

            oppdrag.utsattTilDato shouldBe null
        }
    }

    @Nested
    inner class HentOppdragMedNyIdent {

        private fun personident(ident: String, historisk: Boolean) = PersonidentDto(ident = ident, historisk = historisk, gruppe = Identgruppe.FOLKEREGISTERIDENT)

        @Test
        fun `skal oppdatere kravhaver ident og hente oppdrag på nytt når kravhaver har fått ny ident`() {
            val gammelKravhaver = genererFødselsnummer()
            val nyKravhaver = genererFødselsnummer()
            val skyldner = genererFødselsnummer()
            val oppdrag = TestData.opprettOppdrag()
            val hendelse = TestData.opprettHendelse(kravhaverIdent = nyKravhaver, skyldnerIdent = skyldner)

            every { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any(), any()) } returns TestData.opprettOppdragsperiode()
            every { bidragPersonConsumer.hentAlleIdenterForPerson(nyKravhaver) } returns listOf(personident(nyKravhaver, false), personident(gammelKravhaver, true))
            every { bidragPersonConsumer.hentAlleIdenterForPerson(skyldner) } returns listOf(personident(skyldner, false))
            every { persistenceService.hentOppdragPaUnikeIdentifikatorer(any(), eq(nyKravhaver), any(), any()) } returns null
            every { persistenceService.hentOppdragPaUnikeIdentifikatorer(any(), eq(gammelKravhaver), any(), any()) } returns oppdrag

            oppdragService.lagreHendelse(hendelse)

            verify { personhendelseService.oppdaterKravhaver(gammelKravhaver, nyKravhaver) }
            verify(exactly = 0) { personhendelseService.oppdaterSkyldner(any(), any()) }
        }

        @Test
        fun `skal oppdatere skyldner ident og hente oppdrag på nytt når skyldner har fått ny ident`() {
            val gammelSkyldner = genererFødselsnummer()
            val nySkyldner = genererFødselsnummer()
            val kravhaver = genererFødselsnummer()
            val oppdrag = TestData.opprettOppdrag()
            val hendelse = TestData.opprettHendelse(kravhaverIdent = kravhaver, skyldnerIdent = nySkyldner)

            every { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any(), any()) } returns TestData.opprettOppdragsperiode()
            every { bidragPersonConsumer.hentAlleIdenterForPerson(kravhaver) } returns listOf(personident(kravhaver, false))
            every { bidragPersonConsumer.hentAlleIdenterForPerson(nySkyldner) } returns listOf(personident(nySkyldner, false), personident(gammelSkyldner, true))
            every { persistenceService.hentOppdragPaUnikeIdentifikatorer(any(), any(), eq(nySkyldner), any()) } returns null
            every { persistenceService.hentOppdragPaUnikeIdentifikatorer(any(), any(), eq(gammelSkyldner), any()) } returns oppdrag

            oppdragService.lagreHendelse(hendelse)

            verify { personhendelseService.oppdaterSkyldner(gammelSkyldner, nySkyldner) }
            verify(exactly = 0) { personhendelseService.oppdaterKravhaver(any(), any()) }
        }

        @Test
        fun `skal ikke oppdatere identer når oppdrag finnes på gjeldende identer i historikk`() {
            val kravhaver = genererFødselsnummer()
            val skyldner = genererFødselsnummer()
            val oppdrag = TestData.opprettOppdrag()
            val hendelse = TestData.opprettHendelse(kravhaverIdent = kravhaver, skyldnerIdent = skyldner)

            every { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any(), any()) } returns TestData.opprettOppdragsperiode()
            every { bidragPersonConsumer.hentAlleIdenterForPerson(kravhaver) } returns listOf(personident(kravhaver, false))
            every { bidragPersonConsumer.hentAlleIdenterForPerson(skyldner) } returns listOf(personident(skyldner, false))
            every { persistenceService.hentOppdragPaUnikeIdentifikatorer(any(), any(), any(), any()) } returnsMany listOf(null, oppdrag)

            oppdragService.lagreHendelse(hendelse)

            verify(exactly = 0) { personhendelseService.oppdaterKravhaver(any(), any()) }
            verify(exactly = 0) { personhendelseService.oppdaterSkyldner(any(), any()) }
        }

        @Test
        fun `skal ikke oppdatere identer når oppdrag ikke finnes på noen historiske identer`() {
            val kravhaver = genererFødselsnummer()
            val skyldner = genererFødselsnummer()
            val hendelse = TestData.opprettHendelse(kravhaverIdent = kravhaver, skyldnerIdent = skyldner)

            every { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any(), any()) } returns TestData.opprettOppdragsperiode()
            every { bidragPersonConsumer.hentAlleIdenterForPerson(kravhaver) } returns listOf(personident(kravhaver, false))
            every { bidragPersonConsumer.hentAlleIdenterForPerson(skyldner) } returns listOf(personident(skyldner, false))
            every { persistenceService.hentOppdragPaUnikeIdentifikatorer(any(), any(), any(), any()) } returns null

            oppdragService.lagreHendelse(hendelse)

            verify(exactly = 0) { personhendelseService.oppdaterKravhaver(any(), any()) }
            verify(exactly = 0) { personhendelseService.oppdaterSkyldner(any(), any()) }
        }

        @Test
        fun `skal oppdatere både kravhaver og skyldner når begge har fått ny ident`() {
            val gammelKravhaver = genererFødselsnummer()
            val nyKravhaver = genererFødselsnummer()
            val gammelSkyldner = genererFødselsnummer()
            val nySkyldner = genererFødselsnummer()
            val oppdrag = TestData.opprettOppdrag()
            val hendelse = TestData.opprettHendelse(kravhaverIdent = nyKravhaver, skyldnerIdent = nySkyldner)

            every { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any(), any()) } returns TestData.opprettOppdragsperiode()
            every { bidragPersonConsumer.hentAlleIdenterForPerson(nyKravhaver) } returns listOf(personident(nyKravhaver, false), personident(gammelKravhaver, true))
            every { bidragPersonConsumer.hentAlleIdenterForPerson(nySkyldner) } returns listOf(personident(nySkyldner, false), personident(gammelSkyldner, true))
            every { persistenceService.hentOppdragPaUnikeIdentifikatorer(any(), any(), any(), any()) } returns null
            every { persistenceService.hentOppdragPaUnikeIdentifikatorer(any(), eq(gammelKravhaver), eq(gammelSkyldner), any()) } returns oppdrag

            oppdragService.lagreHendelse(hendelse)

            verify { personhendelseService.oppdaterKravhaver(gammelKravhaver, nyKravhaver) }
            verify { personhendelseService.oppdaterSkyldner(gammelSkyldner, nySkyldner) }
        }

        @Test
        fun `skal ikke søke på historiske identer eller kalle personhendelseService når oppdrag finnes på første forsøk`() {
            val oppdrag = TestData.opprettOppdrag()
            val hendelse = TestData.opprettHendelse()

            every { oppdragsperiodeService.opprettNyOppdragsperiode(any(), any(), any()) } returns TestData.opprettOppdragsperiode()
            every { persistenceService.hentOppdragPaUnikeIdentifikatorer(any(), any(), any(), any()) } returns oppdrag

            oppdragService.lagreHendelse(hendelse)

            verify(exactly = 0) { personhendelseService.oppdaterKravhaver(any(), any()) }
            verify(exactly = 0) { personhendelseService.oppdaterSkyldner(any(), any()) }
            verify(exactly = 0) { bidragPersonConsumer.hentAlleIdenterForPerson(any()) }
            verify(exactly = 1) { persistenceService.hentOppdragPaUnikeIdentifikatorer(any(), any(), any(), any()) }
        }
    }

    @Nested
    inner class PatchMottaker {

        @Test
        fun `skal patche mottaker når det finnes et bidrags oppdrag`() {
            val saksnummer = Saksnummer("123456")
            val kravhaver = Personident(genererFødselsnummer())
            val mottaker = Personident(genererFødselsnummer())

            val oppdrag = TestData.opprettOppdrag(
                stonadType = Stønadstype.BIDRAG,
                sakId = saksnummer.verdi,
                kravhaverIdent = kravhaver.verdi,
                mottakerIdent = "DUMMY",
            )
            val oppdragsperiode1 = TestData.opprettOppdragsperiode(oppdrag = oppdrag)
            val oppdragsperiode2 = TestData.opprettOppdragsperiode(oppdrag = oppdrag)
            oppdrag.oppdragsperioder = listOf(oppdragsperiode1, oppdragsperiode2)

            every { persistenceService.hentOppdragPåSaksnummerOgKravhaver(saksnummer, kravhaver) } returns listOf(oppdrag)

            oppdragService.patchMottaker(saksnummer, kravhaver, mottaker)

            oppdrag.mottakerIdent shouldBe mottaker.verdi
        }

        @Test
        fun `skal patche mottaker når det finnes et bidrag- og et forskudds oppdrag`() {
            val saksnummer = Saksnummer("123456")
            val kravhaver = Personident(genererFødselsnummer())
            val mottaker = Personident(genererFødselsnummer())

            val oppdrag1 = TestData.opprettOppdrag(
                stonadType = Stønadstype.BIDRAG,
                sakId = saksnummer.verdi,
                kravhaverIdent = kravhaver.verdi,
                mottakerIdent = "DUMMY",
            )
            val oppdragsperiode1 = TestData.opprettOppdragsperiode(oppdrag = oppdrag1)
            val oppdragsperiode2 = TestData.opprettOppdragsperiode(oppdrag = oppdrag1)
            oppdrag1.oppdragsperioder = listOf(oppdragsperiode1, oppdragsperiode2)

            val oppdrag2 = TestData.opprettOppdrag(
                stonadType = Stønadstype.FORSKUDD,
                sakId = saksnummer.verdi,
                kravhaverIdent = kravhaver.verdi,
                mottakerIdent = "DUMMY",
            )
            val oppdragsperiode3 = TestData.opprettOppdragsperiode(oppdrag = oppdrag2)
            val oppdragsperiode4 = TestData.opprettOppdragsperiode(oppdrag = oppdrag2)
            oppdrag2.oppdragsperioder = listOf(oppdragsperiode3, oppdragsperiode4)

            every { persistenceService.hentOppdragPåSaksnummerOgKravhaver(saksnummer, kravhaver) } returns listOf(oppdrag1, oppdrag2)

            oppdragService.patchMottaker(saksnummer, kravhaver, mottaker)

            oppdrag1.mottakerIdent shouldBe mottaker.verdi
            oppdrag2.mottakerIdent shouldBe mottaker.verdi
        }

        @Test
        fun `skal patche mottaker for gebyr`() {
            val saksnummer = Saksnummer("123456")
            val kravhaver = Personident(genererFødselsnummer())
            val mottaker = Personident(genererFødselsnummer())

            val oppdrag1 = TestData.opprettOppdrag(
                stonadType = null,
                engangsbelopType = Engangsbeløptype.GEBYR_MOTTAKER,
                sakId = saksnummer.verdi,
                kravhaverIdent = kravhaver.verdi,
                mottakerIdent = "DUMMY",
            )
            val oppdragsperiode1 = TestData.opprettOppdragsperiode(oppdrag = oppdrag1)
            oppdrag1.oppdragsperioder = listOf(oppdragsperiode1)

            every { persistenceService.hentOppdragPåSaksnummerOgKravhaver(saksnummer, kravhaver) } returns listOf(oppdrag1)

            oppdragService.patchMottaker(saksnummer, kravhaver, mottaker)

            oppdrag1.mottakerIdent shouldBe IdentUtils.NAV_TSS_IDENT
        }

        @Test
        fun `skal patche mottaker for ektefellebidrag`() {
            val saksnummer = Saksnummer("123456")
            val kravhaver = Personident(genererFødselsnummer())
            val mottaker = Personident(genererFødselsnummer())

            val oppdrag1 = TestData.opprettOppdrag(
                stonadType = Stønadstype.EKTEFELLEBIDRAG,
                sakId = saksnummer.verdi,
                kravhaverIdent = kravhaver.verdi,
                mottakerIdent = "DUMMY",
            )
            val oppdragsperiode1 = TestData.opprettOppdragsperiode(oppdrag = oppdrag1)
            oppdrag1.oppdragsperioder = listOf(oppdragsperiode1)

            every { persistenceService.hentOppdragPåSaksnummerOgKravhaver(saksnummer, kravhaver) } returns listOf(oppdrag1)

            oppdragService.patchMottaker(saksnummer, kravhaver, mottaker)

            oppdrag1.mottakerIdent shouldBe oppdrag1.kravhaverIdent
        }
    }
}
