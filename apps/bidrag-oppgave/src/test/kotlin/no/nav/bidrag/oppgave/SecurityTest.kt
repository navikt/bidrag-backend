package no.nav.bidrag.oppgave

import no.nav.bidrag.oppgave.client.OppgaveApiClient
import no.nav.bidrag.oppgave.dto.OppgaveDto
import no.nav.bidrag.oppgave.dto.OppgaveStatus
import no.nav.bidrag.oppgave.service.OppgaveService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import java.time.OffsetDateTime

@IntegrationTest
class SecurityTest {

    @LocalServerPort
    private var port: Int = 0

    // Mockes slik at et autentisert kall gir 200 uten å treffe nedstrøms oppgave-api.
    @MockitoBean
    private lateinit var oppgaveService: OppgaveService

    private fun oppgaveApiClient(token: String?): OppgaveApiClient {
        val builder = RestClient.builder().baseUrl("http://localhost:$port")
        if (token != null) builder.defaultHeaders { it.setBearerAuth(token) }
        return OppgaveApiClient(builder.build())
    }

    @Test
    fun `kall mot api uten token gir 401`() {
        val exception = assertThrows<HttpClientErrorException> {
            oppgaveApiClient(token = null).hentOppgaverForSak("SAK-123")
        }
        assertThat(exception.statusCode.value()).isEqualTo(401)
    }

    @Test
    fun `kall mot api med gyldig token slipper gjennom sikkerhetsfilteret`() {
        given(oppgaveService.hentOppgaverForSak("SAK-123")).willReturn(
            listOf(
                OppgaveDto(
                    id = 1,
                    tittel = "BID - BEH_SAK",
                    status = OppgaveStatus.OPPRETTET,
                    opprettet = OffsetDateTime.parse("2026-01-15T10:00:00+01:00"),
                ),
            ),
        )

        val oppgaver = oppgaveApiClient(MockOidcServer.issueToken()).hentOppgaverForSak("SAK-123")

        assertThat(oppgaver).hasSize(1)
    }

    @Test
    fun `internal endepunkt er apent uten token`() {
        val response = RestClient.builder()
            .baseUrl("http://localhost:$port")
            .build()
            .get().uri("/internal/health")
            .retrieve()
            .toBodilessEntity()

        assertThat(response.statusCode.is2xxSuccessful).isTrue()
    }

    @Test
    fun `swagger-ui og api-docs er apent uten token`() {
        val client = RestClient.builder().baseUrl("http://localhost:$port").build()

        val basePath = client.get().uri("/").retrieve().toBodilessEntity()
        val swaggerUi = client.get().uri("/swagger-ui/index.html").retrieve().toBodilessEntity()
        val apiDocs = client.get().uri("/v3/api-docs").retrieve().toBodilessEntity()

        assertThat(basePath.statusCode.value()).isEqualTo(302)
        assertThat(swaggerUi.statusCode.value()).isEqualTo(200)
        assertThat(apiDocs.statusCode.value()).isEqualTo(200)
    }

    @Test
    fun `openapi-doc tilbyr bearer-autorisering`() {
        val apiDocs = RestClient.builder()
            .baseUrl("http://localhost:$port")
            .build()
            .get().uri("/v3/api-docs")
            .retrieve()
            .body(String::class.java)

        assertThat(apiDocs).contains("\"bearer-key\"")
        assertThat(apiDocs).contains("\"scheme\":\"bearer\"")
        assertThat(apiDocs).contains("\"bearerFormat\":\"JWT\"")
    }
}
