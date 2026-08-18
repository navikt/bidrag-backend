package no.nav.bidrag.oppgave.controller

import no.nav.bidrag.oppgave.dto.OppgaveDto
import no.nav.bidrag.oppgave.dto.OppgaveStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.client.RestTestClient

class HelloControllerTest {

    private val client = RestTestClient.bindToController(HelloController()).build()

    @Test
    fun `GET api hello returnerer 200 med forventet oppgave`() {
        val oppgave = client.get().uri("/api/hello")
            .exchange()
            .expectStatus().isOk()
            .expectBody(OppgaveDto::class.java)
            .returnResult()
            .responseBody

        assertThat(oppgave).isNotNull
        assertThat(oppgave!!.tittel).isEqualTo("Hello, Bidrag!")
        assertThat(oppgave.status).isEqualTo(OppgaveStatus.OPPRETTET)
    }
}
