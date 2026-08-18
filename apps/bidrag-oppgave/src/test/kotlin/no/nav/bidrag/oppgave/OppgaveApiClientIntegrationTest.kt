package no.nav.bidrag.oppgave

import no.nav.bidrag.oppgave.client.OppgaveApiClient
import no.nav.bidrag.oppgave.dto.OppgaveStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.client.RestClient

@IntegrationTest
class OppgaveApiClientIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `klienten henter hello-oppgave fra api-et over http`() {
        val token = MockOidcServer.issueToken()
        val restClient =
            RestClient.builder()
                .baseUrl("http://localhost:$port")
                .defaultHeaders { it.setBearerAuth(token) }
                .build()
        val client = OppgaveApiClient(restClient)

        val oppgave = client.hentHelloOppgave()

        assertThat(oppgave.tittel).isEqualTo("Hello, Bidrag!")
        assertThat(oppgave.status).isEqualTo(OppgaveStatus.OPPRETTET)
    }
}
