package no.nav.bidrag.belopshistorikk.controller

import no.nav.bidrag.belopshistorikk.BidragBeløpshistorikkTest
import no.nav.bidrag.belopshistorikk.BidragBeløpshistorikkTest.Companion.TEST_PROFILE
import no.nav.bidrag.belopshistorikk.TestUtil
import no.nav.bidrag.belopshistorikk.bo.toPeriodeBo
import no.nav.bidrag.belopshistorikk.persistence.repository.EngangsbeløpRepository
import no.nav.bidrag.belopshistorikk.persistence.repository.PeriodeRepository
import no.nav.bidrag.belopshistorikk.persistence.repository.StønadRepository
import no.nav.bidrag.belopshistorikk.service.PersistenceService
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate
import no.nav.bidrag.domene.enums.vedtak.Engangsbeløptype
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentEngangsbeløpRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadHistoriskRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.HentStønadRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.LøpendeBidragPeriodeRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.LøpendeBidragssakerRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.request.OpprettStønadRequestDto
import no.nav.bidrag.transport.behandling.belopshistorikk.request.OpprettStønadsperiodeRequestDto
import no.nav.bidrag.transport.behandling.belopshistorikk.request.SkyldnerStønaderRequest
import no.nav.bidrag.transport.behandling.belopshistorikk.response.EngangsbeløpDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidragPeriodeResponse
import no.nav.bidrag.transport.behandling.belopshistorikk.response.LøpendeBidragssakerResponse
import no.nav.bidrag.transport.behandling.belopshistorikk.response.SkyldnerStønaderResponse
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.StønadMedPeriodeBeløpResponse
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest(classes = [BidragBeløpshistorikkTest::class], webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(TEST_PROFILE)
@EnableWireMock(
    ConfigureWireMock(name = "my-service", port = 0),
)
@EnableMockOAuth2Server
class BeløpshistorikkControllerTest {
    @Autowired
    private lateinit var securedTestRestTemplate: HttpHeaderTestRestTemplate

    @Autowired
    private lateinit var periodeRepository: PeriodeRepository

    @Autowired
    private lateinit var stønadRepository: StønadRepository

    @Autowired
    private lateinit var engangsbeløpRepository: EngangsbeløpRepository

    @Autowired
    private lateinit var persistenceService: PersistenceService

    @LocalServerPort
    private val port = 0

    @BeforeEach
    fun `init`() {
        // Sletter alle forekomster
        periodeRepository.deleteAll()
        stønadRepository.deleteAll()
        engangsbeløpRepository.deleteAll()
    }

    @Test
    fun `skal mappe til context path med random port`() {
        assertThat(makeFullContextPath()).isEqualTo("http://localhost:$port")
    }

    @Test
    fun `skal finne data for en stønad`() {
        // Oppretter ny forekomst av stønad

        val periodeListe =
            listOf(
                OpprettStønadsperiodeRequestDto(
                    periode = ÅrMånedsperiode(fom = LocalDate.parse("2019-01-01"), til = LocalDate.parse("2019-07-01")),
                    vedtaksid = 321,
                    gyldigFra = LocalDateTime.now(),
                    gyldigTil = null,
                    periodeGjortUgyldigAvVedtaksid = 246,
                    beløp = BigDecimal.valueOf(3490),
                    valutakode = "NOK",
                    resultatkode = "KOSTNADSBEREGNET_BIDRAG",
                ),
                OpprettStønadsperiodeRequestDto(
                    periode = ÅrMånedsperiode(fom = LocalDate.parse("2019-07-01"), til = LocalDate.parse("2020-01-01")),
                    vedtaksid = 323,
                    gyldigFra = LocalDateTime.now(),
                    gyldigTil = null,
                    periodeGjortUgyldigAvVedtaksid = 22,
                    beløp = BigDecimal.valueOf(3520),
                    valutakode = "NOK",
                    resultatkode = "KOSTNADSBEREGNET_BIDRAG",
                ),
            )

        val stønadOpprettetStønadsid =
            persistenceService.opprettStønad(
                OpprettStønadRequestDto(
                    type = Stønadstype.BIDRAG,
                    sak = Saksnummer("SAK-001"),
                    skyldner = Personident(TestUtil.SKYLDNER_IDENT),
                    kravhaver = Personident(TestUtil.KRAVHAVER_IDENT),
                    mottaker = Personident(TestUtil.MOTTAKER_IDENT),
                    nesteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    opprettetAv = "X123456",
                    periodeListe = periodeListe,
                ),
            )

        periodeListe.forEach {
            persistenceService.opprettPeriode(periodeBo = it.toPeriodeBo(), stønadsid = stønadOpprettetStønadsid)
        }

        // Henter forekomst
        val response =
            securedTestRestTemplate.postForEntity<StønadDto>(
                "${makeFullContextPath()}/hent-stonad/",
                byggStønadRequest(),
            )

        assertAll(
            { assertThat(response).isNotNull() },
            { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(response.body).isNotNull },
        )
        periodeRepository.deleteAll()
        stønadRepository.deleteAll()
    }

    @Test
    fun `skal returnere 404 med feildetaljer når enkeltoppslag ikke gir treff`() {
        val stønad = TestUtil.byggStønadRequest()
        val engangsbeløp = TestUtil.byggEngangsbeløpRequest()
        val oppslag = listOf(
            Triple(
                "/hent-stonad/",
                HentStønadRequest(stønad.type, stønad.sak, stønad.skyldner, stønad.kravhaver),
                "Stønad ikke funnet",
            ),
            Triple(
                "/hent-stonad-historisk/",
                HentStønadHistoriskRequest(stønad.type, stønad.sak, stønad.skyldner, stønad.kravhaver),
                "Stønad ikke funnet",
            ),
            Triple(
                "/hent-stonad-periodebeløp/",
                HentStønadRequest(stønad.type, stønad.sak, stønad.skyldner, stønad.kravhaver),
                "Stønad ikke funnet",
            ),
            Triple(
                "/hent-engangsbelop",
                HentEngangsbeløpRequest(
                    engangsbeløp.type,
                    engangsbeløp.sak,
                    engangsbeløp.skyldner,
                    engangsbeløp.kravhaver,
                    requireNotNull(engangsbeløp.referanse),
                ),
                "Engangsbeløp ikke funnet",
            ),
        )

        oppslag.forEach { (sti, request, melding) ->
            val response = securedTestRestTemplate.postForEntity<String>("${makeFullContextPath()}$sti", request)

            assertThat(response.statusCode).describedAs(sti).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(response.headers.contentType?.subtype).describedAs(sti).isIn("json", "problem+json")
            assertThat(response.body).describedAs(sti).contains("\"status\":404", "\"detail\":\"$melding\"")
            assertThat(response.body).doesNotContain(stønad.skyldner.verdi, engangsbeløp.skyldner.verdi)
        }
    }

    @Test
    fun `skal returnere ProblemDetail ved ugyldig forespørsel`() {
        val response = securedTestRestTemplate.postForEntity<String>(
            "${makeFullContextPath()}/hent-stonad/",
            initHttpEntity("""{"type": "UKJENT_STØNADSTYPE"}"""),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.headers.contentType?.subtype).isIn("json", "problem+json")
        assertThat(response.body).contains("\"status\":400", "\"detail\":")
        assertThat(response.body).doesNotContain("UKJENT_STØNADSTYPE")
    }

    @Test
    fun `skal returnere tomme lister når listeoppslag ikke gir treff`() {
        val skyldner = Personident(TestUtil.SKYLDNER_IDENT)
        val sak = securedTestRestTemplate.getForEntity<List<StønadDto>>("${makeFullContextPath()}/hent-stonader-for-sak/SAK-999")
        val løpendeSaker = securedTestRestTemplate.postForEntity<LøpendeBidragssakerResponse>(
            "${makeFullContextPath()}/hent-lopende-bidragssaker-for-skyldner",
            LøpendeBidragssakerRequest(skyldner),
        )
        val stønader = securedTestRestTemplate.postForEntity<SkyldnerStønaderResponse>(
            "${makeFullContextPath()}/hent-alle-stonader-for-skyldner",
            SkyldnerStønaderRequest(skyldner),
        )
        val løpendePerioder = securedTestRestTemplate.postForEntity<LøpendeBidragPeriodeResponse>(
            "${makeFullContextPath()}/hent-stonader-i-periode/",
            LøpendeBidragPeriodeRequest(skyldner, ÅrMånedsperiode(LocalDate.parse("2024-01-01"), LocalDate.parse("2025-01-01"))),
        )

        assertThat(sak.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(sak.body).isEmpty()
        assertThat(løpendeSaker.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(løpendeSaker.body?.bidragssakerListe).isEmpty()
        assertThat(stønader.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(stønader.body?.stønader).isEmpty()
        assertThat(løpendePerioder.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(løpendePerioder.body?.bidragListe).isEmpty()
    }

    @Test
    fun `skal finne stønad med periodebeløp`() {
        // Oppretter ny forekomst av stønad

        val periodeListe =
            listOf(
                OpprettStønadsperiodeRequestDto(
                    periode = ÅrMånedsperiode(fom = LocalDate.parse("2019-01-01"), til = LocalDate.parse("2019-07-01")),
                    vedtaksid = 321,
                    gyldigFra = LocalDateTime.now(),
                    gyldigTil = null,
                    periodeGjortUgyldigAvVedtaksid = 246,
                    beløp = BigDecimal.valueOf(3490),
                    valutakode = "DKK",
                    resultatkode = "KOSTNADSBEREGNET_BIDRAG",
                ),
                OpprettStønadsperiodeRequestDto(
                    periode = ÅrMånedsperiode(fom = LocalDate.parse("2019-07-01"), til = LocalDate.parse("2020-01-01")),
                    vedtaksid = 323,
                    gyldigFra = LocalDateTime.now(),
                    gyldigTil = null,
                    periodeGjortUgyldigAvVedtaksid = 22,
                    beløp = BigDecimal.valueOf(3520),
                    valutakode = "DKK",
                    resultatkode = "KOSTNADSBEREGNET_BIDRAG",
                ),
            )

        val stønadOpprettetStønadsid =
            persistenceService.opprettStønad(
                OpprettStønadRequestDto(
                    type = Stønadstype.BIDRAG,
                    sak = Saksnummer("SAK-001"),
                    skyldner = Personident(TestUtil.SKYLDNER_IDENT),
                    kravhaver = Personident(TestUtil.KRAVHAVER_IDENT),
                    mottaker = Personident(TestUtil.MOTTAKER_IDENT),
                    nesteIndeksreguleringsår = 2024,
                    innkreving = Innkrevingstype.MED_INNKREVING,
                    opprettetAv = "X123456",
                    periodeListe = periodeListe,
                ),
            )

        periodeListe.forEach {
            persistenceService.opprettPeriode(it.toPeriodeBo(), stønadOpprettetStønadsid)
        }

        // Henter forekomst
        val response =
            securedTestRestTemplate.postForEntity<StønadMedPeriodeBeløpResponse>(
                "${makeFullContextPath()}/hent-stonad-periodebeløp/",
                byggStønadRequest(),
            )

        assertAll(
            { assertThat(response).isNotNull() },
            { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(response.body).isNotNull },
        )
        periodeRepository.deleteAll()
        stønadRepository.deleteAll()
    }

    @Test
    fun `skal hente særbidrag fra referanse`() {
        val særbidrag = TestUtil.byggEngangsbeløpRequest()
        persistenceService.opprettEngangsbeløp(særbidrag)

        val response = securedTestRestTemplate.postForEntity<EngangsbeløpDto>(
            "${makeFullContextPath()}/hent-engangsbelop",
            HentEngangsbeløpRequest(
                type = særbidrag.type,
                sak = særbidrag.sak,
                skyldner = særbidrag.skyldner,
                kravhaver = særbidrag.kravhaver,
                referanse = requireNotNull(særbidrag.referanse),
            ),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.referanse).isEqualTo(særbidrag.referanse)
        assertThat(response.body?.vedtaksid).isEqualTo(særbidrag.vedtaksid)
    }

    @Test
    fun `skal finne engangsbeløp for sak, tester også at type engangsbeløp overstyres fra SAERTILSKUDD og SÆRTILSKUDD til SÆRBIDRAG`() {
        // Oppretter to engangsbeløp for SAK-001
        persistenceService.opprettEngangsbeløp(TestUtil.byggEngangsbeløpRequest3())
        persistenceService.opprettEngangsbeløp(TestUtil.byggEngangsbeløpRequest4())

        val response =
            securedTestRestTemplate.exchange(
                "${makeFullContextPath()}/engangsbelop/SAK-001",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<List<EngangsbeløpDto>>() {},
            )

        assertAll(
            { assertThat(response).isNotNull() },
            { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(response.body).isNotNull },
            { assertThat(response.body).hasSize(2) },
            { assertThat(response.body?.get(0)?.type).isEqualTo(Engangsbeløptype.SÆRBIDRAG) },
            { assertThat(response.body?.get(1)?.type).isEqualTo(Engangsbeløptype.SÆRBIDRAG) },
        )
    }

    @Test
    fun `skal returnere tom liste når ingen engangsbeløp finnes for sak`() {
        persistenceService.opprettEngangsbeløp(TestUtil.byggEngangsbeløpRequest())
        val response =
            securedTestRestTemplate.getForEntity<List<EngangsbeløpDto>>(
                "${makeFullContextPath()}/engangsbelop/SAK-999",
            )

        assertAll(
            { assertThat(response).isNotNull() },
            { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(response.body).isNotNull },
            { assertThat(response.body).isEmpty() },
        )
    }

    private fun makeFullContextPath(): String = "http://localhost:$port"

    private fun byggStønadRequest(): HttpEntity<OpprettStønadRequestDto> = initHttpEntity(TestUtil.byggStønadRequest())

    private fun <T : Any> initHttpEntity(body: T): HttpEntity<T> {
        val httpHeaders = HttpHeaders()
        httpHeaders.contentType = MediaType.APPLICATION_JSON
        return HttpEntity(body, httpHeaders)
    }
}
