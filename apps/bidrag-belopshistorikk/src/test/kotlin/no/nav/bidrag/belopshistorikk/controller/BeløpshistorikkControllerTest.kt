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
import no.nav.bidrag.domene.enums.vedtak.Innkrevingstype
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.domene.tid.ÅrMånedsperiode
import no.nav.bidrag.transport.behandling.belopshistorikk.request.OpprettStønadRequestDto
import no.nav.bidrag.transport.behandling.belopshistorikk.request.OpprettStønadsperiodeRequestDto
import no.nav.bidrag.transport.behandling.belopshistorikk.response.EngangsbeløpDto
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
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
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
    fun `skal finne engangsbeløp for sak`() {
        // Oppretter to engangsbeløp for SAK-001
        persistenceService.opprettEngangsbeløp(TestUtil.byggEngangsbeløpRequest())
        persistenceService.opprettEngangsbeløp(TestUtil.byggEngangsbeløpRequest2())

        val response =
            securedTestRestTemplate.getForEntity<List<EngangsbeløpDto>>(
                "${makeFullContextPath()}/engangsbelop/SAK-001",
            )

        assertAll(
            { assertThat(response).isNotNull() },
            { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
            { assertThat(response.body).isNotNull },
            { assertThat(response.body).hasSize(2) },
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
