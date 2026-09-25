package no.nav.bidrag.oppgave

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.assertj.MockMvcTester

@ActiveProfiles("junit")
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTest {

    @Autowired
    private lateinit var mockMvc: MockMvcTester

    @Test
    fun `get kall mot api uten token gir 401`() {
        val resultat = mockMvc.get()
            .uri("/api/oppgaver?saksnummer=SAK-123")
            .exchange()

        assertThat(resultat).hasStatus(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `autentisert kall slipper gjennom sikkerhetsfilteret`() {
        val resultat = mockMvc.get()
            .uri("/api/oppgaver")
            .with(jwt())
            .exchange()

        assertThat(resultat).hasStatus(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `internal endepunkt er apent uten token`() {
        val resultat = mockMvc.get()
            .uri("/internal/health")
            .exchange()

        assertThat(resultat).hasStatusOk()
    }

    @Test
    fun `api docs er apent og tilbyr bearer autorisering`() {
        val resultat = mockMvc.get()
            .uri("/v3/api-docs")
            .exchange()

        assertThat(resultat).hasStatusOk()
        assertThat(resultat.response.contentAsString)
            .contains("\"bearer-key\"")
            .contains("\"scheme\":\"bearer\"")
            .contains("\"bearerFormat\":\"JWT\"")
    }
}
