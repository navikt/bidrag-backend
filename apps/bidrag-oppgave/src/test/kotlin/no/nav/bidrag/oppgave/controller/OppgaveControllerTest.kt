package no.nav.bidrag.oppgave.controller

import no.nav.bidrag.oppgave.dto.OppgaveDto
import no.nav.bidrag.oppgave.dto.OppgaveStatus
import no.nav.bidrag.oppgave.service.OppgaveService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.test.web.servlet.client.RestTestClient
import java.time.OffsetDateTime

class OppgaveControllerTest {

    private val service = mock(OppgaveService::class.java)
    private val client = RestTestClient.bindToController(OppgaveController(service)).build()

    @Test
    fun `GET api oppgaver returnerer oppgaver for saksnummer`() {
        val oppgave = OppgaveDto(
            id = 123,
            tittel = "BID - BEH_SAK",
            beskrivelse = "En bidragsoppgave",
            status = OppgaveStatus.UNDER_BEHANDLING,
            opprettet = OffsetDateTime.parse("2026-01-15T10:00:00+01:00"),
        )
        given(service.hentOppgaverForSak("SAK-123")).willReturn(listOf(oppgave))

        val oppgaver = client.get().uri("/api/oppgaver?saksnummer=SAK-123")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Array<OppgaveDto>::class.java)
            .returnResult()
            .responseBody

        assertThat(oppgaver).isNotNull
        assertThat(oppgaver!!).hasSize(1)
        assertThat(oppgaver.single().id).isEqualTo(123)
        assertThat(oppgaver.single().status).isEqualTo(OppgaveStatus.UNDER_BEHANDLING)
    }
}
