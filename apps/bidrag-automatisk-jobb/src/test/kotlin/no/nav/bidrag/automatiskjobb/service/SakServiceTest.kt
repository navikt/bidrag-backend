package no.nav.bidrag.automatiskjobb.service

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import no.nav.bidrag.automatiskjobb.consumer.BidragBeløpshistorikkConsumer
import no.nav.bidrag.automatiskjobb.consumer.BidragVedtakConsumer
import no.nav.bidrag.automatiskjobb.service.model.OpprettVedtakConflictResponse
import no.nav.bidrag.automatiskjobb.utils.UnleashFeatures
import no.nav.bidrag.commons.unleash.UnleashFeaturesProvider
import no.nav.bidrag.commons.util.IdentUtils
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
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
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentEngangsbeløpRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.EngangsbeløpDto
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
        mockkObject(UnleashFeaturesProvider)
        every {
            UnleashFeaturesProvider.isEnabled(eq(UnleashFeatures.FATTE_ENDRING_MOTTAKER_VEDTAK.featureName), any())
        } returns true
        every { identUtils.hentNyesteIdent(any()) } returnsArgument 0
        every { bidragBeløpshistorikkConsumer.hentEngangsbeløpForSak(any()) } returns emptyList()
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
    fun `skal fatte ett vedtak for alle tre stønadstyper når disse løper`() {
        stubLøpendeStønad(Stønadstype.BIDRAG, mottaker = reellMottaker)
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)
        stubLøpendeStønad(Stønadstype.BIDRAG18AAR, mottaker = reellMottaker)

        behandle(reellMottaker = nyReellMottaker)

        val request = slot<OpprettVedtakRequestDto>()
        verify(exactly = 1) { bidragVedtakConsumer.opprettVedtak(capture(request)) }
        request.captured.stønadsendringListe.map { it.type }.toSet() shouldBe
            setOf(Stønadstype.BIDRAG, Stønadstype.FORSKUDD, Stønadstype.BIDRAG18AAR)
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
    fun `skal sette lesbar unik referanse av saksnummer, tidspunkt, hendelsestype og barn`() {
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)

        behandle(reellMottaker = nyReellMottaker, hendelseTidspunkt = HENDELSE_TIDSPUNKT)

        val request = slot<OpprettVedtakRequestDto>()
        verify(exactly = 1) { bidragVedtakConsumer.opprettVedtak(capture(request)) }
        request.captured.unikReferanse shouldBe
            "endring_mottaker_${saksnummer}_20260910083015123_ENDRING_" +
            "${kravhaver}_$nyReellMottaker"
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
    fun `ulik stønadstype for samme barn samles i samme vedtak`() {
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)
        stubLøpendeStønad(Stønadstype.BIDRAG, mottaker = reellMottaker)

        behandle(reellMottaker = nyReellMottaker)

        val request = slot<OpprettVedtakRequestDto>()
        verify(exactly = 1) { bidragVedtakConsumer.opprettVedtak(capture(request)) }
        request.captured.stønadsendringListe shouldHaveSize 2
    }

    @Test
    fun `skal fatte ett vedtak for flere særbidrag og løpende stønad for samme barn`() {
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)
        val første = engangsbeløp("referanse-1", reellMottaker)
        val andre = engangsbeløp("referanse-2", reellMottaker)
        every { bidragBeløpshistorikkConsumer.hentEngangsbeløpForSak(any()) } returns listOf(første, andre)
        every { bidragBeløpshistorikkConsumer.hentEngangsbeløp(any()) } answers {
            when (firstArg<HentEngangsbeløpRequest>().referanse) {
                "referanse-1" -> første
                "referanse-2" -> andre
                else -> error("Ukjent referanse")
            }
        }

        behandle(reellMottaker = nyReellMottaker)

        val request = slot<OpprettVedtakRequestDto>()
        verify(exactly = 1) { bidragVedtakConsumer.opprettVedtak(capture(request)) }
        request.captured.stønadsendringListe.single().periodeListe.shouldBeEmpty()
        request.captured.engangsbeløpListe.map { it.referanse }.toSet() shouldBe setOf("referanse-1", "referanse-2")
        request.captured.engangsbeløpListe.forEach {
            it.omgjørVedtakId shouldBe 42
            it.mottaker shouldBe Personident(nyReellMottaker)
        }
        verify(exactly = 2) { bidragBeløpshistorikkConsumer.hentEngangsbeløp(any()) }
    }

    @Test
    fun `skal fatte vedtak bare for endrede særbidrag`() {
        val endret = engangsbeløp("referanse-1", reellMottaker)
        val uendret = engangsbeløp("referanse-2", nyReellMottaker)
        every { bidragBeløpshistorikkConsumer.hentEngangsbeløpForSak(any()) } returns listOf(endret, uendret)
        every { bidragBeløpshistorikkConsumer.hentEngangsbeløp(any()) } answers {
            if (firstArg<HentEngangsbeløpRequest>().referanse == "referanse-1") endret else uendret
        }

        behandle(reellMottaker = nyReellMottaker)

        val request = slot<OpprettVedtakRequestDto>()
        verify(exactly = 1) { bidragVedtakConsumer.opprettVedtak(capture(request)) }
        request.captured.stønadsendringListe.shouldBeEmpty()
        request.captured.engangsbeløpListe.single().referanse shouldBe "referanse-1"
    }

    @Test
    fun `skal vurdere særbidrag uten innkreving når mottaker er endret`() {
        val særbidrag = engangsbeløp("referanse-1", reellMottaker, innkrevingstype = Innkrevingstype.UTEN_INNKREVING)
        every { bidragBeløpshistorikkConsumer.hentEngangsbeløpForSak(any()) } returns listOf(særbidrag)
        every { bidragBeløpshistorikkConsumer.hentEngangsbeløp(any()) } returns særbidrag

        behandle(reellMottaker = nyReellMottaker)

        val request = slot<OpprettVedtakRequestDto>()
        verify(exactly = 1) { bidragVedtakConsumer.opprettVedtak(capture(request)) }
        request.captured.engangsbeløpListe.single().innkreving shouldBe Innkrevingstype.UTEN_INNKREVING
    }

    @Test
    fun `skal fatte ett vedtak per barn med særbidrag`() {
        val annetBarn = genererFødselsnummer()
        val første = engangsbeløp("referanse-1", reellMottaker)
        val andre = engangsbeløp("referanse-2", reellMottaker, annetBarn)
        every { bidragBeløpshistorikkConsumer.hentEngangsbeløpForSak(any()) } returns listOf(første, andre)
        every { bidragBeløpshistorikkConsumer.hentEngangsbeløp(any()) } answers {
            if (firstArg<HentEngangsbeløpRequest>().referanse == "referanse-1") første else andre
        }

        sakService.behandleSakHendelse(
            sakHendelse(reellMottaker = nyReellMottaker).copy(
                barn = listOf(
                    BarnISak(ident = Personident(kravhaver), reellMottaker = ReellMottaker(nyReellMottaker)),
                    BarnISak(ident = Personident(annetBarn), reellMottaker = ReellMottaker(nyReellMottaker)),
                ),
            ),
            HENDELSE_TIDSPUNKT,
        )

        val requests = mutableListOf<OpprettVedtakRequestDto>()
        verify(exactly = 2) { bidragVedtakConsumer.opprettVedtak(capture(requests)) }
        requests.map { it.engangsbeløpListe.single().referanse }.toSet() shouldBe setOf("referanse-1", "referanse-2")
        requests.map { it.unikReferanse }.toSet() shouldHaveSize 2
    }

    @Test
    fun `skal avbryte når særbidrag fra sakslisten ikke finnes i oppslag`() {
        every { bidragBeløpshistorikkConsumer.hentEngangsbeløpForSak(any()) } returns listOf(engangsbeløp("referanse-1", reellMottaker))
        every { bidragBeløpshistorikkConsumer.hentEngangsbeløp(any()) } returns null

        shouldThrow<IllegalStateException> { behandle(reellMottaker = nyReellMottaker) }

        verify(exactly = 0) { bidragVedtakConsumer.opprettVedtak(any()) }
    }

    @Test
    fun `skal ikke fatte vedtak når oppslag etter særbidrag feiler`() {
        every { bidragBeløpshistorikkConsumer.hentEngangsbeløpForSak(any()) } throws IllegalStateException("Oppslag feilet")

        shouldThrow<IllegalStateException> { behandle(reellMottaker = nyReellMottaker) }

        verify(exactly = 0) { bidragVedtakConsumer.opprettVedtak(any()) }
    }

    @Test
    fun `skal ikke behandle sakhendelse eller fatte vedtak når feature toggle er avskrudd`() {
        every {
            UnleashFeaturesProvider.isEnabled(eq(UnleashFeatures.FATTE_ENDRING_MOTTAKER_VEDTAK.featureName), any())
        } returns false
        stubLøpendeStønad(Stønadstype.FORSKUDD, mottaker = reellMottaker)

        behandle(reellMottaker = nyReellMottaker)

        verify(exactly = 0) { bidragBeløpshistorikkConsumer.hentLøpendeStønad(any()) }
        verify(exactly = 0) { bidragBeløpshistorikkConsumer.hentEngangsbeløpForSak(any()) }
        verify(exactly = 0) { bidragVedtakConsumer.opprettVedtak(any()) }
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

    private fun engangsbeløp(
        referanseForSærbidrag: String,
        opprinneligMottaker: String,
        barn: String = kravhaver,
        innkrevingstype: Innkrevingstype = Innkrevingstype.MED_INNKREVING,
    ): EngangsbeløpDto = mockk {
        every { type } returns Engangsbeløptype.SÆRBIDRAG
        every { sak } returns Saksnummer(saksnummer)
        every { skyldner } returns Personident(bidragspliktig)
        every { kravhaver } returns Personident(barn)
        every { mottaker } returns Personident(opprinneligMottaker)
        every { innkreving } returns innkrevingstype
        every { referanse } returns referanseForSærbidrag
        every { vedtaksid } returns 42
        every { resultatkode } returns "SÆRBIDRAG_INNVILGET"
        every { beløp } returns BigDecimal(1000)
        every { valutakode } returns "NOK"
    }

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
