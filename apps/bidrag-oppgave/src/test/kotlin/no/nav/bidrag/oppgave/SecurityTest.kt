package no.nav.bidrag.oppgave

import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.servlet.client.RestTestClient

@IntegrationTest
class SecurityTest {

    @LocalServerPort
    private var port: Int = 0

    private val client by lazy {
        RestTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @Test
    fun `GET api hello uten token gir 401`() {
        client.get().uri("/api/hello")
            .exchange()
            .expectStatus().isUnauthorized()
    }

    @Test
    fun `GET api hello med gyldig token gir 200`() {
        client.get().uri("/api/hello")
            .headers { it.setBearerAuth(MockOidcServer.issueToken()) }
            .exchange()
            .expectStatus().isOk()
    }

    @Test
    fun `internal endepunkt er apent uten token`() {
        client.get().uri("/internal/health")
            .exchange()
            .expectStatus().isOk()
    }
}
