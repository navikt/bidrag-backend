package no.nav.bidrag.oppgave.controller

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.oppgave.OppgaveTestData.bidragsoppgaveDto
import no.nav.bidrag.oppgave.OppgaveTestData.forventetBidragOppgaveDto
import no.nav.bidrag.oppgave.OppgaveTestData.oppgaveResponse
import no.nav.bidrag.oppgave.config.RestConfig
import no.nav.bidrag.oppgave.config.SecurityConfig
import no.nav.bidrag.oppgave.consumer.oppgaveapi.OppgaveClient
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.AktorId
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.FellesKodeverkTema
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.FinnOppgaverParams
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.OppgaveDto
import no.nav.bidrag.oppgave.service.OppgaveService
import no.nav.bidrag.tilgang.TilgangskontrollException
import no.nav.bidrag.tilgang.TilgangskontrollService
import no.nav.bidrag.transport.tilgang.TilgangskontrollResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.web.client.HttpClientErrorException
import tools.jackson.databind.ObjectMapper

@WebMvcTest(OppgaveController::class)
@Import(OppgaveService::class, SecurityConfig::class, RestConfig::class, FinnOppgaverControllerTest.TestConfig::class)
class FinnOppgaverControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvcTester

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var oppgaveClient: OppgaveClient

    @MockitoBean
    private lateinit var tilgangkontrollService: TilgangskontrollService

    @Test
    fun `POST oppgaver med kun saksnummer bruker standardverdier`() {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willReturn(oppgaveResponse())

        val resultat = postOppgaver(FinnOppgaverRequest(saksnummer = "SAK-123"))

        assertThat(resultat).hasStatusOk()
        assertThat(resultat.oppgaver())
            .singleElement()
            .usingRecursiveComparison()
            .isEqualTo(forventetBidragOppgaveDto)

        val params = capturedParams()
        assertThat(params.saksreferanse).containsExactly("SAK-123")
        assertThat(params.tema).containsExactly(FellesKodeverkTema.BID)
        assertThat(params.statuskategori).isEqualTo("AAPEN")
        assertThat(params.limit).isEqualTo(100)
        assertThat(params.offset).isEqualTo(0)
    }

    @Test
    fun `POST oppgaver videresender alle sokefelter og returnerer mappet oppgave`() {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willReturn(oppgaveResponse())

        val resultat = postOppgaver(
            FinnOppgaverRequest(
                saksnummer = "SAK-123",
                aktoerId = AktorId("1234567890123"),
            ),
        )

        assertThat(resultat).hasStatusOk()
        assertThat(resultat.oppgaver())
            .singleElement()
            .usingRecursiveComparison()
            .isEqualTo(forventetBidragOppgaveDto)

        val params = capturedParams()
        assertThat(params.saksreferanse).containsExactly("SAK-123")
        assertThat(params.aktoerId).containsExactly(AktorId("1234567890123"))
        assertThat(params.tema).containsExactly(FellesKodeverkTema.BID)
        assertThat(params.statuskategori).isEqualTo("AAPEN")
        assertThat(params.limit).isEqualTo(100)
    }

    @ParameterizedTest
    @MethodSource("brukereUtenPersonident")
    fun `POST oppgaver sender ikke brukerIdent for aktør-ID eller andre brukertyper`(bruker: OppgaveDto.Bruker) {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willReturn(oppgaveResponse(listOf(bidragsoppgaveDto.copy(bruker = bruker))))

        val resultat = postOppgaver(FinnOppgaverRequest(saksnummer = "SAK-123"))

        assertThat(resultat).hasStatusOk()
        assertThat(resultat.oppgaver()).singleElement().extracting { it.brukerIdent }.isNull()
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, 0])
    fun `POST oppgaver avviser ugyldig limit`(limit: Int) {
        val resultat = postOppgaver(FinnOppgaverRequest(saksnummer = "SAK-123", limit = limit))

        assertThat(resultat).hasStatus(HttpStatus.BAD_REQUEST)
        verifyNoInteractions(oppgaveClient)
    }

    // Verdien er større enn Int og kan ikke uttrykkes med FinnOppgaverRequest, så her sendes rå JSON.
    @Test
    fun `POST oppgaver avviser limit som ikke passer i Int`() {
        val resultat = mockMvc.post()
            .uri("/api/oppgaver")
            .with(jwtToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"saksnummer": "SAK-123", "limit": 999999999999999999999999}""")
            .exchange()

        assertThat(resultat).hasStatus(HttpStatus.BAD_REQUEST)
        verifyNoInteractions(oppgaveClient)
    }

    @ParameterizedTest
    @MethodSource("søkUtenAvgrensning")
    fun `POST oppgaver avviser søk uten avgrensning`(request: FinnOppgaverRequest) {
        val resultat = postOppgaver(request)

        assertThat(resultat).hasStatus(HttpStatus.BAD_REQUEST)
        verifyNoInteractions(oppgaveClient)
    }

    @ParameterizedTest
    @MethodSource("søkMedEttKriterium")
    fun `POST oppgaver godtar hvert søkekriterium alene`(request: FinnOppgaverRequest) {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willReturn(oppgaveResponse())

        val resultat = postOppgaver(request)

        assertThat(resultat).hasStatusOk()
        verify(oppgaveClient).finnOppgaver(anyFinnOppgaverParams())
    }

    @Test
    fun `POST oppgaver bruker standardgrense når limit er null`() {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willReturn(oppgaveResponse())

        val resultat = postOppgaver(FinnOppgaverRequest(saksnummer = "SAK-123", limit = null))

        assertThat(resultat).hasStatusOk()
        assertThat(capturedParams().limit).isEqualTo(100)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 100, 101, 5000])
    fun `POST oppgaver godtar limit uten øvre grense`(limit: Int) {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willReturn(oppgaveResponse())

        val resultat = postOppgaver(FinnOppgaverRequest(saksnummer = "SAK-123", limit = limit))

        assertThat(resultat).hasStatusOk()
        assertThat(capturedParams().limit).isEqualTo(limit)
    }

    @Test
    fun `POST oppgaver sender offset og limit direkte videre`() {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willReturn(oppgaveResponse())

        val resultat = postOppgaver(FinnOppgaverRequest(saksnummer = "SAK-123", offset = 60, limit = 20))

        assertThat(resultat).hasStatusOk()
        val params = capturedParams()
        assertThat(params.limit).isEqualTo(20)
        assertThat(params.offset).isEqualTo(60)
    }

    @Test
    fun `POST oppgaver bruker offset 0 når offset er null`() {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willReturn(oppgaveResponse())

        val resultat = postOppgaver(FinnOppgaverRequest(saksnummer = "SAK-123", offset = null))

        assertThat(resultat).hasStatusOk()
        assertThat(resultat).hasHeader(OppgaveController.HEADER_OFFSET, "0")
        assertThat(capturedParams().offset).isEqualTo(0)
    }

    @Test
    fun `POST oppgaver avviser negativ offset`() {
        val resultat = postOppgaver(FinnOppgaverRequest(saksnummer = "SAK-123", offset = -1))

        assertThat(resultat).hasStatus(HttpStatus.BAD_REQUEST)
        verifyNoInteractions(oppgaveClient)
    }

    @Test
    fun `POST oppgaver returnerer pagineringsheadere`() {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willReturn(oppgaveResponse().copy(antallTreffTotalt = 42))

        val resultat = postOppgaver(FinnOppgaverRequest(saksnummer = "SAK-123", offset = 20, limit = 10))

        assertThat(resultat).hasStatusOk()
        assertThat(resultat)
            .hasHeader(OppgaveController.HEADER_OFFSET, "20")
            .hasHeader(OppgaveController.HEADER_LIMIT, "10")
            .hasHeader(OppgaveController.HEADER_TOTAL_COUNT, "42")
    }

    @Test
    fun `POST oppgaver utelater X-Total-Count når oppgave-API ikke oppgir totalt antall`() {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willReturn(oppgaveResponse().copy(antallTreffTotalt = null))

        val resultat = postOppgaver(FinnOppgaverRequest(saksnummer = "SAK-123"))

        assertThat(resultat).hasStatusOk()
        assertThat(resultat).doesNotContainHeader(OppgaveController.HEADER_TOTAL_COUNT)
    }

    @Test
    fun `Returnerer ProblemDetail ved feil fra eksternt API`() {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willThrow(HttpClientErrorException(HttpStatus.NOT_ACCEPTABLE, "Bad Gateway"))

        val resultat = postOppgaver(FinnOppgaverRequest(saksnummer = "SAK-123"))

        assertThat(resultat).hasStatus(HttpStatus.NOT_ACCEPTABLE)
        assertThat(resultat.response.contentType)
            .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
        assertThat(resultat.response.contentAsString)
            .contains(
                "\"status\":406",
                "\"title\":\"Feil mot ekstern tjeneste\"",
            )
    }

    @Test
    fun `Avvist tilgang til sak gir 403`() {
        willThrow(TilgangskontrollException(TilgangskontrollResponse(false, emptyList())))
            .given(tilgangkontrollService)
            .sjekkTilgangSaksnummer(Saksnummer("SAK-123"))

        val resultat = postOppgaver(FinnOppgaverRequest(saksnummer = "SAK-123"))

        assertThat(resultat).hasStatus(HttpStatus.FORBIDDEN)
        assertThat(resultat.response.contentType)
            .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
    }

    @Test
    fun `Avvist tilgang til person gir 403`() {
        willThrow(TilgangskontrollException(TilgangskontrollResponse(false, emptyList())))
            .given(tilgangkontrollService)
            .sjekkTilgangPerson(Personident("1234567890123"))

        val resultat = postOppgaver(FinnOppgaverRequest(aktoerId = AktorId("1234567890123")))

        assertThat(resultat).hasStatus(HttpStatus.FORBIDDEN)
        assertThat(resultat.response.contentType)
            .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
    }

    private fun postOppgaver(request: FinnOppgaverRequest) = mockMvc.post()
        .uri("/api/oppgaver")
        .with(jwtToken())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .exchange()

    private fun capturedParams(): FinnOppgaverParams {
        val captor = ArgumentCaptor.forClass(FinnOppgaverParams::class.java)
        verify(oppgaveClient).finnOppgaver(captor.capture() ?: FinnOppgaverParams())
        return captor.value
    }

    private fun anyFinnOppgaverParams(): FinnOppgaverParams = any(FinnOppgaverParams::class.java) ?: FinnOppgaverParams()

    private fun org.springframework.test.web.servlet.assertj.MvcTestResult.oppgaver(): List<BidragOppgaveDto> = objectMapper
        .readValue(response.contentAsByteArray, Array<BidragOppgaveDto>::class.java)
        .toList()

    private fun jwtToken(): RequestPostProcessor = jwt()

    companion object {
        @JvmStatic
        fun søkUtenAvgrensning() = listOf(
            FinnOppgaverRequest(),
            FinnOppgaverRequest(limit = 1),
            FinnOppgaverRequest(saksnummer = "  "),
            FinnOppgaverRequest(aktoerId = AktorId("")),
        )

        @JvmStatic
        fun søkMedEttKriterium() = listOf(
            FinnOppgaverRequest(saksnummer = "SAK-123"),
            FinnOppgaverRequest(aktoerId = AktorId("1234567890123")),
        )

        @JvmStatic
        fun brukereUtenPersonident() = listOf(
            OppgaveDto.Bruker(ident = "1234567890123", type = OppgaveDto.Bruker.BrukerType.PERSON),
            OppgaveDto.Bruker(ident = "123456789", type = OppgaveDto.Bruker.BrukerType.ARBEIDSGIVER),
            OppgaveDto.Bruker(ident = "80000123456", type = OppgaveDto.Bruker.BrukerType.SAMHANDLER),
        )
    }

    @TestConfiguration
    @EnableWebSecurity
    class TestConfig {
        @Bean
        fun jwtDecoder(): JwtDecoder = JwtDecoder {
            error("JwtDecoder skal ikke kalles når testen bruker jwt()")
        }
    }
}
