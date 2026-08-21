package no.nav.bidrag.oppgave.service

import no.nav.bidrag.oppgave.consumer.oppgaveapi.OppgaveClient
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.EksternOppgaveId
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.Enhetsnummer
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.OppgaveDto
import no.nav.bidrag.oppgave.consumer.oppgaveapi.model.SokOppgaverResponse
import no.nav.bidrag.oppgave.dto.OppgaveStatus
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.startsWith
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.OffsetDateTime

class OppgaveServiceTest {

    private val objectMapper = JsonMapper.builder().findAndAddModules().build()
    private val restTemplate = RestTemplate()
    private val mockServer = MockRestServiceServer.bindTo(restTemplate).build()
    private val restClient = RestClient.builder(restTemplate).build()
    private val service = OppgaveService(OppgaveClient(restClient))

    @Test
    fun `henter oppgaver for saksnummer og mapper til bidrag-dto`() {
        val opprettet = OffsetDateTime.parse("2026-01-15T10:00:00+01:00")
        val response = SokOppgaverResponse(
            antallTreffTotalt = 1,
            oppgaver = listOf(
                OppgaveDto(
                    id = EksternOppgaveId(123),
                    tildeltEnhetsnr = Enhetsnummer("4100"),
                    tema = "BID",
                    oppgavetype = "BEH_SAK",
                    versjon = 1,
                    prioritet = OppgaveDto.Prioritet.NORM,
                    status = OppgaveDto.Status.UNDER_BEHANDLING,
                    aktivDato = LocalDate.now(),
                    saksreferanse = "SAK-123",
                    beskrivelse = "En bidragsoppgave",
                    opprettetTidspunkt = opprettet,
                ),
            ),
        )

        mockServer.expect(requestTo(startsWith("/api/v1/oppgaver?")))
            .andExpect(method(HttpMethod.GET))
            .andExpect(queryParam("saksreferanse", "SAK-123"))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(response)),
            )

        val oppgaver = service.hentOppgaverForSak("SAK-123")

        assertThat(oppgaver).hasSize(1)
        val oppgave = oppgaver.single()
        assertThat(oppgave.id).isEqualTo(123)
        assertThat(oppgave.beskrivelse).isEqualTo("En bidragsoppgave")
        assertThat(oppgave.status).isEqualTo(OppgaveStatus.UNDER_BEHANDLING)
        assertThat(oppgave.opprettet).isEqualTo(opprettet)
        mockServer.verify()
    }
}
