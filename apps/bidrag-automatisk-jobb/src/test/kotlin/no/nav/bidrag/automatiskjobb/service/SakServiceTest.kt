package no.nav.bidrag.automatiskjobb.service

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.service.model.OpprettVedtakConflictResponse
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
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadPeriodeDto
import no.nav.bidrag.transport.behandling.vedtak.request.OpprettVedtakRequestDto
import no.nav.bidrag.transport.sak.BarnISak
import no.nav.bidrag.transport.sak.SakHendelse
import no.nav.bidrag.transport.sak.SakKafkaHendelsestype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.HttpClientErrorException
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.bidrag.beregn.barnebidrag.service.external.VedtakService as BeregnVedtakService

@ExtendWith(MockKExtension::class)
class SakServiceTest {
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
    }

    @Test
    fun `skal fatte vedtak for endring av mottaker når reell mottaker avviker fra beløpshistorikken`() {
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)

        behandle(reellMottaker = nyReellMottaker)

        val request = slot<OpprettVedtakRequestDto>()
        verify(exactly = 1) { bidragVedtakConsumer.opprettVedtak(capture(request)) }

        request.captured.type shouldBe Vedtakstype.ENDRING_MOTTAKER
        val stønadsendring = request.captured.stønadsendringListe.single()
        stønadsendring.type shouldBe Stønadstype.FORSKUDD
        stønadsendring.sak shouldBe Saksnummer(saksnummer)
        stønadsendring.kravhaver shouldBe Personident(kravhaver)
        stønadsendring.skyldner shouldBe personidentNav
        stønadsendring.mottaker shouldBe Personident(nyReellMottaker)
        stønadsendring.periodeListe.shouldBeEmpty()
    }

    @Test
    fun `skal ikke sette perioder på vedtaket`() {
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)

        behandle(reellMottaker = nyReellMottaker)

        val request = slot<OpprettVedtakRequestDto>()
        verify(exactly = 1) { bidragVedtakConsumer.opprettVedtak(capture(request)) }
        request.captured.stønadsendringListe
            .single()
            .periodeListe
            .shouldBeEmpty()
    }

    @Test
    fun `skal fatte vedtak for alle tre stønadstyper når disse løper`() {
        stubLøpendeStønad(Stønadstype.BIDRAG, mottaker = reellMottaker)
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)
        stubLøpendeStønad(Stønadstype.BIDRAG18AAR, mottaker = reellMottaker)

        behandle(reellMottaker = nyReellMottaker)

        val requests = mutableListOf<OpprettVedtakRequestDto>()
        verify(exactly = 3) { bidragVedtakConsumer.opprettVedtak(capture(requests)) }
        requests
            .map { it.stønadsendringListe.single().type }
            .toSet() shouldBe setOf(Stønadstype.BIDRAG, Stønadstype.FORSKUDD, Stønadstype.BIDRAG18AAR)
    }

    @Test
    fun `skal sette siste vedtaksid på stønadsendringen`() {
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)
        every { beregnVedtakService.finnSisteVedtaksid(any()) } returns 4242

        behandle(reellMottaker = nyReellMottaker)

        val request = slot<OpprettVedtakRequestDto>()
        verify(exactly = 1) { bidragVedtakConsumer.opprettVedtak(capture(request)) }
        request.captured.stønadsendringListe
            .single()
            .sisteVedtaksid shouldBe 4242
    }

    @Test
    fun `skal ikke fatte vedtak når mottaker er uendret`() {
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)

        behandle(reellMottaker = reellMottaker)

        verify(exactly = 0) { bidragVedtakConsumer.opprettVedtak(any()) }
    }

    @Test
    fun `skal ikke fatte vedtak når reell mottaker kun har fått nytt fødselsnummer`() {
        val nyttFødselsnummerSammePerson = genererFødselsnummer()
        every { identUtils.hentNyesteIdent(Personident(reellMottaker)) } returns Personident(nyttFødselsnummerSammePerson)
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)

        behandle(reellMottaker = nyttFødselsnummerSammePerson)

        verify(exactly = 0) { bidragVedtakConsumer.opprettVedtak(any()) }
    }

    @Test
    fun `skal ikke fatte vedtak når det ikke finnes løpende stønad`() {
        every { bidragBeløpshistorikkConsumer.hentLøpendeStønad(any()) } returns null

        behandle(reellMottaker = nyReellMottaker)

        verify(exactly = 0) { bidragVedtakConsumer.opprettVedtak(any()) }
    }

    @Test
    fun `skal sette bidragsmottaker som mottaker når reell mottaker er fjernet`() {
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)

        behandle(reellMottaker = null)

        val request = slot<OpprettVedtakRequestDto>()
        verify(exactly = 1) { bidragVedtakConsumer.opprettVedtak(capture(request)) }
        request.captured.stønadsendringListe
            .single()
            .mottaker shouldBe Personident(bidragsmottaker)
    }

    @Test
    fun `skal ikke fatte vedtak når ny reell mottaker er en samhandler`() {
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)

        behandle(reellMottaker = SAMHANDLER_ID)

        verify(exactly = 0) { bidragVedtakConsumer.opprettVedtak(any()) }
    }

    @Test
    fun `skal ikke feile når vedtaket allerede finnes (409 Conflict)`() {
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)
        val konflikt = HttpClientErrorException.create(
            HttpStatus.CONFLICT,
            "Conflict",
            HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON },
            """{"vedtaksid": 999}""".toByteArray(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8,
        ).apply {
            // RestTemplate setter normalt denne; her setter vi den manuelt så getResponseBodyAs virker.
            setBodyConvertFunction { OpprettVedtakConflictResponse(999) }
        }
        every { bidragVedtakConsumer.opprettVedtak(any()) } throws konflikt

        shouldNotThrowAny {
            behandle(reellMottaker = nyReellMottaker)
        }

        verify(exactly = 1) { bidragVedtakConsumer.opprettVedtak(any()) }
    }

    @Test
    fun `skal sette lesbar unik referanse av saksnummer, tidspunkt, hendelsestype, stønadstype og identer`() {
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)

        behandle(reellMottaker = nyReellMottaker, hendelseTidspunkt = HENDELSE_TIDSPUNKT)

        val request = slot<OpprettVedtakRequestDto>()
        verify(exactly = 1) { bidragVedtakConsumer.opprettVedtak(capture(request)) }
        request.captured.unikReferanse shouldBe
            "endring_mottaker_${saksnummer}_20260910083015123_ENDRING_FORSKUDD_" +
            "${kravhaver}_${personidentNav.verdi}_$nyReellMottaker"
    }

    @Test
    fun `unik referanse skal være deterministisk for samme hendelse`() {
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)

        behandle(reellMottaker = nyReellMottaker, hendelseTidspunkt = HENDELSE_TIDSPUNKT)
        behandle(reellMottaker = nyReellMottaker, hendelseTidspunkt = HENDELSE_TIDSPUNKT)

        val requests = mutableListOf<OpprettVedtakRequestDto>()
        verify(exactly = 2) { bidragVedtakConsumer.opprettVedtak(capture(requests)) }
        requests[0].unikReferanse shouldBe requests[1].unikReferanse
    }

    @Test
    fun `unik referanse skal skille ulike stønadstyper for samme hendelse`() {
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)
        stubLøpendeStønad(Stønadstype.BIDRAG, mottaker = reellMottaker)

        behandle(reellMottaker = nyReellMottaker)

        val requests = mutableListOf<OpprettVedtakRequestDto>()
        verify(exactly = 2) { bidragVedtakConsumer.opprettVedtak(capture(requests)) }
        requests.map { it.unikReferanse }.toSet() shouldHaveSize 2
    }

    private fun behandle(
        reellMottaker: String?,
        hendelseTidspunkt: Instant = HENDELSE_TIDSPUNKT,
    ) = sakService.behandleSakHendelse(sakHendelse(reellMottaker = reellMottaker), hendelseTidspunkt)

    private fun stubLøpendeStønad(
        type: Stønadstype,
        mottaker: String,
    ) {
        every {
            bidragBeløpshistorikkConsumer.hentLøpendeStønad(match<HentStønadRequest> { it.type == type })
        } returns løpendeStønad(type, mottaker)
    }

    private fun løpendeStønad(
        type: Stønadstype,
        mottaker: String,
    ) = StønadDto(
        stønadsid = 1,
        type = type,
        sak = Saksnummer(saksnummer),
        skyldner = if (type == Stønadstype.FORSKUDD) personidentNav else Personident(bidragspliktig),
        kravhaver = Personident(kravhaver),
        mottaker = Personident(mottaker),
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
        private val HENDELSE_TIDSPUNKT: Instant = Instant.parse("2026-09-10T08:30:15.123Z")
    }
}
