package no.nav.bidrag.grunnlag.controller

import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.runBlocking
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate
import no.nav.bidrag.domene.enums.grunnlag.GrunnlagRequestType
import no.nav.bidrag.domene.enums.vedtak.Formål
import no.nav.bidrag.grunnlag.BidragGrunnlagTest
import no.nav.bidrag.grunnlag.BidragGrunnlagTest.Companion.TEST_PROFILE
import no.nav.bidrag.grunnlag.TestUtil
import no.nav.bidrag.grunnlag.consumer.arbeidsforhold.ArbeidsforholdConsumer
import no.nav.bidrag.grunnlag.consumer.bidragperson.BidragPersonConsumer
import no.nav.bidrag.grunnlag.consumer.familiebasak.FamilieBaSakConsumer
import no.nav.bidrag.grunnlag.consumer.familieefsak.FamilieEfSakConsumer
import no.nav.bidrag.grunnlag.consumer.familiekssak.FamilieKsSakConsumer
import no.nav.bidrag.grunnlag.consumer.inntektskomponenten.InntektskomponentenConsumer
import no.nav.bidrag.grunnlag.consumer.pensjon.PensjonConsumer
import no.nav.bidrag.grunnlag.consumer.skattegrunnlag.SigrunConsumer
import no.nav.bidrag.grunnlag.consumer.tilleggsstønad.TilleggsstønadConsumer
import no.nav.bidrag.grunnlag.controller.GrunnlagController.Companion.GRUNNLAGSPAKKE_HENT
import no.nav.bidrag.grunnlag.controller.GrunnlagController.Companion.GRUNNLAGSPAKKE_LUKK
import no.nav.bidrag.grunnlag.controller.GrunnlagController.Companion.GRUNNLAGSPAKKE_NY
import no.nav.bidrag.grunnlag.controller.GrunnlagController.Companion.GRUNNLAGSPAKKE_OPPDATER
import no.nav.bidrag.grunnlag.controller.GrunnlagController.Companion.HENT_GRUNNLAG
import no.nav.bidrag.grunnlag.service.GrunnlagspakkeService
import no.nav.bidrag.grunnlag.service.HentGrunnlagService
import no.nav.bidrag.grunnlag.service.HentValutakursService
import no.nav.bidrag.transport.behandling.grunnlag.request.GrunnlagRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.request.HentGrunnlagRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.request.OppdaterGrunnlagspakkeRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.request.OpprettGrunnlagspakkeRequestDto
import no.nav.bidrag.transport.behandling.grunnlag.response.HentGrunnlagDto
import no.nav.bidrag.transport.behandling.grunnlag.response.HentGrunnlagspakkeDto
import no.nav.bidrag.transport.behandling.grunnlag.response.OppdaterGrunnlagspakkeDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.util.UriComponentsBuilder
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("GrunnlagControllerTest")
@ActiveProfiles(TEST_PROFILE)
@SpringBootTest(classes = [BidragGrunnlagTest::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableMockOAuth2Server
@EnableWireMock(
    ConfigureWireMock(name = "my-service", port = 0),
)
class GrunnlagControllerTest {

    @Autowired
    private lateinit var securedTestRestTemplate: TestRestTemplate

    @MockitoBean
    private lateinit var grunnlagspakkeService: GrunnlagspakkeService

    @MockitoBean
    private lateinit var hentGrunnlagService: HentGrunnlagService

    @MockitoBean
    private lateinit var hentValutakursService: HentValutakursService

    @MockitoBean
    private lateinit var bidragPersonConsumer: BidragPersonConsumer

    @MockitoBean
    private lateinit var arbeidsforholdConsumer: ArbeidsforholdConsumer

    @MockitoBean
    private lateinit var familieBaSakConsumer: FamilieBaSakConsumer

    @MockitoBean
    private lateinit var tilleggsstønadConsumer: TilleggsstønadConsumer

    @MockitoBean
    private lateinit var familieEfSakConsumer: FamilieEfSakConsumer

    @MockitoBean
    private lateinit var familieKsSakConsumer: FamilieKsSakConsumer

    @MockitoBean
    private lateinit var inntektskomponentenConsumer: InntektskomponentenConsumer

    @MockitoBean
    private lateinit var pensjonConsumer: PensjonConsumer

    @MockitoBean
    private lateinit var sigrunConsumer: SigrunConsumer

    @LocalServerPort
    private val port = 0

    @Test
    fun `skal mappe til context path med random port`() {
        assertThat(makeFullContextPath()).isEqualTo("http://localhost:$port")
    }

    @Test
    fun `skal opprette ny grunnlagspakke`() {
        val request = OpprettGrunnlagspakkeRequestDto(Formål.FORSKUDD, "X123456")
        `when`(grunnlagspakkeService.opprettGrunnlagspakke(request)).thenReturn(1)

        val response = securedTestRestTemplate.exchange(
            fullUrlForOpprettGrunnlagspakke(),
            HttpMethod.POST,
            byggHttpOpprettGrunnlagspakkeRequest(request),
            Int::class.java,
        )

        assertAll(
            { assert(response.statusCode.is2xxSuccessful) },
            { assert(response.body == 1) },
        )
    }

    @Test
    fun `skal oppdatere grunnlagspakke`() {
        val request = TestUtil.byggOppdaterGrunnlagspakkeRequestKomplett()
        `when`(grunnlagspakkeService.oppdaterGrunnlagspakke(grunnlagspakkeId = 1, oppdaterGrunnlagspakkeRequestDto = request))
            .thenReturn(OppdaterGrunnlagspakkeDto(grunnlagspakkeId = 1, grunnlagTypeResponsListe = emptyList()))

        val response = securedTestRestTemplate.exchange(
            fullUrlForOppdaterGrunnlagspakke(1),
            HttpMethod.POST,
            byggHttpOppdaterGrunnlagspakkeRequest(request),
            OppdaterGrunnlagspakkeDto::class.java,
        )

        assertAll(
            { assert(response.statusCode.is2xxSuccessful) },
            { assert(response.body.shouldNotBeNull().grunnlagspakkeId == 1) },
            { assert(response.body.shouldNotBeNull().grunnlagTypeResponsListe.isEmpty()) },
        )
    }

    @Test
    fun `skal hente grunnlagspakke`() {
        `when`(grunnlagspakkeService.hentGrunnlagspakke(1))
            .thenReturn(
                HentGrunnlagspakkeDto(
                    grunnlagspakkeId = 1,
                    ainntektListe = emptyList(),
                    skattegrunnlagListe = emptyList(),
                    ubstListe = emptyList(),
                    barnetilleggListe = emptyList(),
                    kontantstotteListe = emptyList(),
                    husstandmedlemmerOgEgneBarnListe = emptyList(),
                    sivilstandListe = emptyList(),
                    barnetilsynListe = emptyList(),
                ),
            )

        val response = securedTestRestTemplate.exchange(
            fullUrlForHentGrunnlagspakke(1),
            HttpMethod.GET,
            HttpEntity.EMPTY,
            HentGrunnlagspakkeDto::class.java,
        )

        assertAll(
            { assert(response.statusCode.is2xxSuccessful) },
            { assert(response.body.shouldNotBeNull().grunnlagspakkeId == 1) },
            { assert(response.body.shouldNotBeNull().ainntektListe.isEmpty()) },
        )
    }

    @Test
    fun `skal lukke grunnlagspakke`() {
        `when`(grunnlagspakkeService.lukkGrunnlagspakke(1)).thenReturn(1)

        val response = securedTestRestTemplate.exchange(
            fullUrlForLukkGrunnlagspakke(1),
            HttpMethod.POST,
            byggHttpLukkGrunnlagspakkeRequest(1),
            Int::class.java,
        )

        assertAll(
            { assert(response.statusCode.is2xxSuccessful) },
            { assert(response.body == 1) },
        )
    }

    @Test
    fun `skal hente grunnlag`() = runBlocking {
        val request = HentGrunnlagRequestDto(
            formaal = Formål.FORSKUDD,
            grunnlagRequestDtoListe = listOf(
                GrunnlagRequestDto(
                    type = GrunnlagRequestType.AINNTEKT,
                    personId = "12345678901",
                    periodeFra = LocalDate.now(),
                    periodeTil = LocalDate.now(),
                ),
            ),
        )

        `when`(hentGrunnlagService.hentGrunnlag(request)).thenReturn(
            HentGrunnlagDto(
                ainntektListe = emptyList(),
                skattegrunnlagListe = emptyList(),
                utvidetBarnetrygdListe = emptyList(),
                småbarnstilleggListe = emptyList(),
                barnetilleggListe = emptyList(),
                kontantstøtteListe = emptyList(),
                husstandsmedlemmerOgEgneBarnListe = emptyList(),
                sivilstandListe = emptyList(),
                barnetilsynListe = emptyList(),
                arbeidsforholdListe = emptyList(),
                tilleggsstønadBarnetilsynListe = emptyList(),
                feilrapporteringListe = emptyList(),
                hentetTidspunkt = LocalDateTime.now(),
            ),
        )

        val response = securedTestRestTemplate.exchange(
            fullUrlForHentGrunnlag(),
            HttpMethod.POST,
            byggHttpHentGrunnlagRequest(request),
            HentGrunnlagDto::class.java,
        )

        assertAll(
            { assert(response.statusCode.is2xxSuccessful) },
            { assert(response.body.shouldNotBeNull().ainntektListe.isEmpty()) },
        )
    }

    private fun fullUrlForOpprettGrunnlagspakke() = UriComponentsBuilder.fromUriString(makeFullContextPath() + GRUNNLAGSPAKKE_NY).toUriString()

    private fun fullUrlForOppdaterGrunnlagspakke(grunnlagspakkeId: Int): String {
        val path = GRUNNLAGSPAKKE_OPPDATER.replace("{grunnlagspakkeId}", grunnlagspakkeId.toString())
        return UriComponentsBuilder.fromUriString(makeFullContextPath() + path).toUriString()
    }

    private fun fullUrlForHentGrunnlagspakke(grunnlagspakkeId: Int): String {
        val path = GRUNNLAGSPAKKE_HENT.replace("{grunnlagspakkeId}", grunnlagspakkeId.toString())
        return UriComponentsBuilder.fromUriString(makeFullContextPath() + path).toUriString()
    }

    private fun fullUrlForLukkGrunnlagspakke(grunnlagspakkeId: Int): String {
        val path = GRUNNLAGSPAKKE_LUKK.replace("{grunnlagspakkeId}", grunnlagspakkeId.toString())
        return UriComponentsBuilder.fromUriString(makeFullContextPath() + path).toUriString()
    }

    private fun fullUrlForHentGrunnlag() = UriComponentsBuilder.fromUriString(makeFullContextPath() + HENT_GRUNNLAG).toUriString()

    private fun byggHttpOpprettGrunnlagspakkeRequest(request: OpprettGrunnlagspakkeRequestDto): HttpEntity<OpprettGrunnlagspakkeRequestDto> = initHttpEntity(request)

    private fun byggHttpOppdaterGrunnlagspakkeRequest(request: OppdaterGrunnlagspakkeRequestDto): HttpEntity<OppdaterGrunnlagspakkeRequestDto> = initHttpEntity(request)

    private fun byggHttpLukkGrunnlagspakkeRequest(request: Int): HttpEntity<Int> = initHttpEntity(request)

    private fun byggHttpHentGrunnlagRequest(request: HentGrunnlagRequestDto): HttpEntity<HentGrunnlagRequestDto> = initHttpEntity(request)

    private fun makeFullContextPath() = "http://localhost:$port"

    private fun <T : Any> initHttpEntity(body: T): HttpEntity<T> {
        val httpHeaders = HttpHeaders()
        httpHeaders.contentType = MediaType.APPLICATION_JSON
        return HttpEntity(body, httpHeaders)
    }
}
