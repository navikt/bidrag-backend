package no.nav.bidrag.oppgave.controller

import no.nav.bidrag.oppgave.OppgaveTestData.forventetBidragOppgaveDto
import no.nav.bidrag.oppgave.OppgaveTestData.oppgaveResponse
import no.nav.bidrag.oppgave.config.SecurityConfig
import no.nav.bidrag.oppgave.consumer.oppgaveapi.OppgaveClient
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.AktorId
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.Enhetsnummer
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.FellesKodeverkTema
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.FinnOppgaverParams
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.NavIdent
import no.nav.bidrag.oppgave.dto.OppgaveDto
import no.nav.bidrag.oppgave.service.OppgaveService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.request.RequestPostProcessor
import tools.jackson.databind.ObjectMapper

@WebMvcTest(OppgaveController::class)
@Import(OppgaveService::class, SecurityConfig::class, OppgaveControllerTest.TestConfig::class)
class OppgaveControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvcTester

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var oppgaveClient: OppgaveClient

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
