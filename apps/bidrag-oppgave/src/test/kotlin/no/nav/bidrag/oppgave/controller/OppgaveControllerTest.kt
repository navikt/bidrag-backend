package no.nav.bidrag.oppgave.controller

import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.oppgave.OppgaveTestData.forventetBidragOppgaveDto
import no.nav.bidrag.oppgave.OppgaveTestData.oppgaveResponse
import no.nav.bidrag.oppgave.config.RestConfig
import no.nav.bidrag.oppgave.config.SecurityConfig
import no.nav.bidrag.oppgave.consumer.oppgaveapi.OppgaveClient
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.AktorId
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.Enhetsnummer
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.FellesKodeverkTema
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.FinnOppgaverParams
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.NavIdent
import no.nav.bidrag.oppgave.dto.OppgaveDto
import no.nav.bidrag.oppgave.service.OppgaveService
import no.nav.bidrag.tilgang.TilgangskontrollException
import no.nav.bidrag.tilgang.TilgangskontrollService
import no.nav.bidrag.transport.tilgang.TilgangskontrollResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
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
@Import(OppgaveService::class, SecurityConfig::class, RestConfig::class, OppgaveControllerTest.TestConfig::class)
class OppgaveControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvcTester

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var oppgaveClient: OppgaveClient

    @MockitoBean
    private lateinit var tilgangkontrollService: TilgangskontrollService

    @Test
    fun `GET oppgaver videresender saksnummer og returnerer mappet oppgave`() {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willReturn(oppgaveResponse())

        val resultat = mockMvc.get()
            .uri("/api/oppgaver?saksnummer=SAK-123")
            .with(jwtToken())
            .exchange()

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
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   "])
    fun `GET oppgaver avviser blankt saksnummer`(saksnummer: String) {
        val resultat = mockMvc.get()
            .uri("/api/oppgaver?saksnummer={saksnummer}", saksnummer)
            .with(jwtToken())
            .exchange()

        assertThat(resultat).hasStatus(HttpStatus.BAD_REQUEST)
        verifyNoInteractions(oppgaveClient)
    }

    @Test
    fun `POST oppgaver videresender alle sokefelter og returnerer mappet oppgave`() {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willReturn(oppgaveResponse())

        val resultat = mockMvc.post()
            .uri("/api/oppgaver")
            .with(jwtToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "saksnummer": "SAK-123",
                  "aktoerId": "1234567890123",
                  "saksbehandler": "Z999999",
                  "enhetsnummer": "4100"
                }
                """.trimIndent(),
            )
            .exchange()

        assertThat(resultat).hasStatusOk()
        assertThat(resultat.oppgaver())
            .singleElement()
            .usingRecursiveComparison()
            .isEqualTo(forventetBidragOppgaveDto)

        val params = capturedParams()
        assertThat(params.saksreferanse).containsExactly("SAK-123")
        assertThat(params.aktoerId).containsExactly(AktorId("1234567890123"))
        assertThat(params.tilordnetRessurs).isEqualTo(NavIdent("Z999999"))
        assertThat(params.tildeltEnhetsnr).isEqualTo(Enhetsnummer("4100"))
        assertThat(params.tema).containsExactly(FellesKodeverkTema.BID)
        assertThat(params.statuskategori).isEqualTo("AAPEN")
        assertThat(params.limit).isEqualTo(100)
    }

    @ParameterizedTest
    @ValueSource(strings = ["-1", "0", "101", "2147483647", "999999999999999999999999"])
    fun `POST oppgaver avviser ugyldig limit`(limit: String) {
        val resultat = mockMvc.post()
            .uri("/api/oppgaver")
            .with(jwtToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"saksnummer": "SAK-123", "limit": $limit}""")
            .exchange()

        assertThat(resultat).hasStatus(HttpStatus.BAD_REQUEST)
        verifyNoInteractions(oppgaveClient)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "{}",
            """{"limit": 1}""",
            """{"saksnummer": null, "aktoerId": null, "saksbehandler": null, "enhetsnummer": null}""",
            """{"saksnummer": "  "}""",
            """{"aktoerId": ""}""",
            """{"saksbehandler": "  "}""",
            """{"enhetsnummer": ""}""",
        ],
    )
    fun `POST oppgaver avviser søk uten avgrensning`(body: String) {
        val resultat = mockMvc.post()
            .uri("/api/oppgaver")
            .with(jwtToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .exchange()

        assertThat(resultat).hasStatus(HttpStatus.BAD_REQUEST)
        verifyNoInteractions(oppgaveClient)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            """{"saksnummer": "SAK-123"}""",
            """{"aktoerId": "1234567890123"}""",
            """{"saksbehandler": "Z999999"}""",
            """{"enhetsnummer": "4100"}""",
        ],
    )
    fun `POST oppgaver godtar hvert søkekriterium alene`(body: String) {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willReturn(oppgaveResponse())

        val resultat = mockMvc.post()
            .uri("/api/oppgaver")
            .with(jwtToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .exchange()

        assertThat(resultat).hasStatusOk()
        verify(oppgaveClient).finnOppgaver(anyFinnOppgaverParams())
    }

    @Test
    fun `POST oppgaver bruker standardgrense når limit er null`() {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willReturn(oppgaveResponse())

        val resultat = mockMvc.post()
            .uri("/api/oppgaver")
            .with(jwtToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"saksnummer": "SAK-123", "limit": null}""")
            .exchange()

        assertThat(resultat).hasStatusOk()
        assertThat(capturedParams().limit).isEqualTo(100)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 55, 100])
    fun `POST oppgaver godtar limit på grenseverdiene og midt i mellom`(limit: Int) {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willReturn(oppgaveResponse())

        val resultat = mockMvc.post()
            .uri("/api/oppgaver")
            .with(jwtToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"saksnummer": "SAK-123", "limit": $limit}""")
            .exchange()

        assertThat(resultat).hasStatusOk()
        assertThat(capturedParams().limit).isEqualTo(limit)
    }

    @Test
    fun `Returnerer ProblemDetail ved feil fra eksternt API`() {
        given(oppgaveClient.finnOppgaver(anyFinnOppgaverParams()))
            .willThrow(HttpClientErrorException(HttpStatus.NOT_ACCEPTABLE, "Bad Gateway"))

        val resultat = mockMvc.get()
            .uri("/api/oppgaver?saksnummer=SAK-123")
            .with(jwtToken())
            .exchange()

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
    fun `Avvist tilgang gir 403`() {
        willThrow(TilgangskontrollException(TilgangskontrollResponse(false, emptyList())))
            .given(tilgangkontrollService)
            .sjekkTilgangSaksnummer(Saksnummer("SAK-123"))

        val resultat = mockMvc.post()
            .uri("/api/oppgaver")
            .with(jwtToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"saksnummer": "SAK-123"}""")
            .exchange()

        assertThat(resultat).hasStatus(HttpStatus.FORBIDDEN)
        assertThat(resultat.response.contentType)
            .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
    }

    private fun capturedParams(): FinnOppgaverParams {
        val captor = ArgumentCaptor.forClass(FinnOppgaverParams::class.java)
        verify(oppgaveClient).finnOppgaver(captor.capture() ?: FinnOppgaverParams())
        return captor.value
    }

    private fun anyFinnOppgaverParams(): FinnOppgaverParams = any(FinnOppgaverParams::class.java) ?: FinnOppgaverParams()

    private fun org.springframework.test.web.servlet.assertj.MvcTestResult.oppgaver(): List<OppgaveDto> = objectMapper
        .readValue(response.contentAsByteArray, Array<OppgaveDto>::class.java)
        .toList()

    private fun jwtToken(): RequestPostProcessor = jwt()

    @TestConfiguration
    @EnableWebSecurity
    class TestConfig {
        @Bean
        fun jwtDecoder(): JwtDecoder = JwtDecoder {
            error("JwtDecoder skal ikke kalles når testen bruker jwt()")
        }
    }
}
