package no.nav.bidrag.oppgave

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [MockOidcServerInitializer::class])
class SecurityTest {

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `get kall mot api uten token gir 401`() {
        val exception = assertThrows<HttpClientErrorException.Unauthorized> {
            restClient().get()
                .uri("/api/oppgaver?saksnummer=SAK-123")
                .retrieve()
                .toBodilessEntity()
        }

        assertThat(exception.statusCode.value()).isEqualTo(401)
    }

    @Test
    fun `post kall mot api uten token gir 401`() {
        val exception = assertThrows<HttpClientErrorException.Unauthorized> {
            restClient().get()
                .uri("/api/oppgaver")
                .retrieve()
                .toBodilessEntity()
        }

        assertThat(exception.statusCode.value()).isEqualTo(401)
    }

    @Test
    fun `gyldig token med rett issuer og audience slipper gjennom sikkerhetsfilteret`() {
        val exception = assertThrows<HttpClientErrorException.BadRequest> {
            restClient(MockOidcServer.issueToken()).get()
                .uri("/api/oppgaver")
                .retrieve()
                .toBodilessEntity()
        }

        assertThat(exception.statusCode.value()).isEqualTo(400)
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
    fun `api-docs er apent og tilbyr bearer-autorisering`() {
        val apiDocs = restClient()
            .get().uri("/v3/api-docs")
            .retrieve()
            .body(String::class.java)

        assertThat(apiDocs).contains("\"bearer-key\"")
        assertThat(apiDocs).contains("\"scheme\":\"bearer\"")
        assertThat(apiDocs).contains("\"bearerFormat\":\"JWT\"")
    }

    private fun restClient(token: String? = null): RestClient {
        val builder = RestClient.builder().baseUrl("http://localhost:$port")
        if (token != null) {
            builder.defaultHeaders { it.setBearerAuth(token) }
        }
        return builder.build()
    }
}
