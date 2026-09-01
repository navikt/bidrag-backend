package no.nav.bidrag.automatiskjobb.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.persistence.entity.Sak
import no.nav.bidrag.automatiskjobb.persistence.entity.SakBarn
import no.nav.bidrag.automatiskjobb.persistence.repository.SakRepository
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.enums.vedtak.Vedtakstype
import no.nav.bidrag.domene.felles.personidentNav
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.ident.ReellMottaker
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.generer.testdata.person.genererFødselsnummer
import no.nav.bidrag.generer.testdata.sak.genererSaksnummer
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadPeriodeDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import no.nav.bidrag.transport.sak.BarnISak
import no.nav.bidrag.transport.sak.SakHendelse
import no.nav.bidrag.transport.sak.SakKafkaHendelsestype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.bidrag.beregn.barnebidrag.service.external.VedtakService as BeregnVedtakService

@ExtendWith(MockKExtension::class)
class SakServiceTest {
    @RelaxedMockK
    private lateinit var sakRepository: SakRepository

    @RelaxedMockK
    private lateinit var bidragVedtakConsumer: BidragVedtakConsumer

    @RelaxedMockK
    private lateinit var bidragBeløpshistorikkConsumer: BidragBeløpshistorikkConsumer

    @RelaxedMockK
    private lateinit var identUtils: IdentUtils

    @RelaxedMockK
    private lateinit var beregnVedtakService: BeregnVedtakService

    @InjectMockKs
    private lateinit var sakService: SakService

    private val saksnummer = genererSaksnummer()
    private val kravhaver = genererFødselsnummer()
    private val bidragspliktig = genererFødselsnummer()
    private val bidragsmottaker = genererFødselsnummer()
    private val reellMottaker = genererFødselsnummer()
    private val nyReellMottaker = genererFødselsnummer()

    @BeforeEach
    fun setup() {
        every { identUtils.hentNyesteIdent(any()) } returnsArgument 0
        every { sakRepository.save(any()) } returnsArgument 0
    }

    @Test
    fun `skal ikke opprette vedtaksforslag ved første hendelse for ukjent sak`() {
        every { sakRepository.findBySaksnummer(saksnummer) } returns null

        sakService.behandleSakHendelse(sakHendelse(reellMottaker = reellMottaker))

        verify(exactly = 0) { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(any()) }
    }

    @Test
    fun `skal opprette vedtaksforslag for endring av mottaker når reell mottaker er endret`() {
        every { sakRepository.findBySaksnummer(saksnummer) } returns lagretSak(reellMottaker)
        every { bidragBeløpshistorikkConsumer.hentLøpendeStønad(any()) } returns løpendeForskudd()

        sakService.behandleSakHendelse(sakHendelse(reellMottaker = nyReellMottaker))

        val request = slot<OpprettVedtakRequestDto>()
        verify(exactly = 1) { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(capture(request)) }

        request.captured.type shouldBe Vedtakstype.ENDRING_MOTTAKER
        val stønadsendring = request.captured.stønadsendringListe.single()
        stønadsendring.type shouldBe Stønadstype.FORSKUDD
        stønadsendring.sak shouldBe Saksnummer(saksnummer)
        stønadsendring.kravhaver shouldBe Personident(kravhaver)
        stønadsendring.skyldner shouldBe personidentNav
        stønadsendring.mottaker shouldBe Personident(nyReellMottaker)
    }

    @Test
    fun `skal sette siste vedtaksid på stønadsendringen`() {
        every { sakRepository.findBySaksnummer(saksnummer) } returns lagretSak(reellMottaker)
        every { bidragBeløpshistorikkConsumer.hentLøpendeStønad(any()) } returns løpendeForskudd()
        every { beregnVedtakService.finnSisteVedtaksid(any()) } returns 4242

        sakService.behandleSakHendelse(sakHendelse(reellMottaker = nyReellMottaker))

        val request = slot<OpprettVedtakRequestDto>()
        verify(exactly = 1) { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(capture(request)) }
        request.captured.stønadsendringListe
            .single()
            .sisteVedtaksid shouldBe 4242
    }

    @Test
    fun `skal ikke opprette vedtaksforslag når reell mottaker er uendret`() {
        every { sakRepository.findBySaksnummer(saksnummer) } returns lagretSak(reellMottaker)

        sakService.behandleSakHendelse(sakHendelse(reellMottaker = reellMottaker))

        verify(exactly = 0) { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(any()) }
    }

    @Test
    fun `skal ikke opprette vedtaksforslag når reell mottaker kun har fått nytt fødselsnummer`() {
        val nyttFødselsnummerSammePerson = genererFødselsnummer()
        every { identUtils.hentNyesteIdent(Personident(reellMottaker)) } returns Personident(nyttFødselsnummerSammePerson)
        every { sakRepository.findBySaksnummer(saksnummer) } returns lagretSak(reellMottaker)
        every { bidragBeløpshistorikkConsumer.hentLøpendeStønad(any()) } returns løpendeForskudd()

        sakService.behandleSakHendelse(sakHendelse(reellMottaker = nyttFødselsnummerSammePerson))

        verify(exactly = 0) { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(any()) }
    }

    @Test
    fun `skal ikke opprette vedtaksforslag når det ikke finnes løpende forskudd`() {
        every { sakRepository.findBySaksnummer(saksnummer) } returns lagretSak(reellMottaker)
        every { bidragBeløpshistorikkConsumer.hentLøpendeStønad(any()) } returns null

        sakService.behandleSakHendelse(sakHendelse(reellMottaker = nyReellMottaker))

        verify(exactly = 0) { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(any()) }
    }

    @Test
    fun `skal sette bidragsmottaker som mottaker når reell mottaker er fjernet`() {
        every { sakRepository.findBySaksnummer(saksnummer) } returns lagretSak(reellMottaker)
        every { bidragBeløpshistorikkConsumer.hentLøpendeStønad(any()) } returns løpendeForskudd()

        sakService.behandleSakHendelse(sakHendelse(reellMottaker = null))

        val request = slot<OpprettVedtakRequestDto>()
        verify(exactly = 1) { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(capture(request)) }
        request.captured.stønadsendringListe
            .single()
            .mottaker shouldBe Personident(bidragsmottaker)
    }

    @Test
    fun `skal ikke opprette vedtaksforslag når ny reell mottaker er en samhandler`() {
        every { sakRepository.findBySaksnummer(saksnummer) } returns lagretSak(reellMottaker)
        every { bidragBeløpshistorikkConsumer.hentLøpendeStønad(any()) } returns løpendeForskudd()

        sakService.behandleSakHendelse(sakHendelse(reellMottaker = SAMHANDLER_ID))

        verify(exactly = 0) { bidragVedtakConsumer.opprettEllerOppdaterVedtaksforslag(any()) }
    }

    private fun lagretSak(reellMottaker: String?) = Sak(
        id = 1,
        saksnummer = saksnummer,
        bidragspliktig = bidragspliktig,
        barn = mutableListOf(SakBarn(id = 1, kravhaver = kravhaver, reellMottaker = reellMottaker)),
    )

    private fun løpendeForskudd() = StønadDto(
        stønadsid = 1,
        type = Stønadstype.FORSKUDD,
        sak = Saksnummer(saksnummer),
        skyldner = personidentNav,
        kravhaver = Personident(kravhaver),
        mottaker = Personident(reellMottaker),
        førsteIndeksreguleringsår = 2025,
        nesteIndeksreguleringsår = 2026,
        innkreving = Innkrevingstype.MED_INNKREVING,
        opprettetAv = "",
        opprettetTidspunkt = LocalDateTime.parse("2025-01-01T00:00:00"),
        endretAv = null,
        endretTidspunkt = null,
        periodeListe =
        listOf(
            StønadPeriodeDto(
                periodeid = 1,
                periode = ÅrMånedsperiode(LocalDate.parse("2025-01-01"), null),
                stønadsid = 1,
                vedtaksid = 42,
                gyldigFra = LocalDateTime.parse("2025-01-01T00:00:00"),
                gyldigTil = null,
                periodeGjortUgyldigAvVedtaksid = null,
                beløp = BigDecimal(1500),
                valutakode = "NOK",
                resultatkode = "OK",
            ),
        ),
    )

    private fun sakHendelse(reellMottaker: String?) = SakHendelse(
        saksnummer = Saksnummer(saksnummer),
        hendelsestype = SakKafkaHendelsestype.ENDRING,
        bidragspliktig = Personident(bidragspliktig),
        bidragsmottaker = Personident(bidragsmottaker),
        barn =
        listOf(
            BarnISak(
                ident = Personident(kravhaver),
                reellMottaker = reellMottaker?.let { ReellMottaker(it) },
            ),
        ),
    )

    companion object {
        private const val SAMHANDLER_ID = "80000000001"
    }
}
