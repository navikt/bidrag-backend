package no.nav.bidrag.arbeidsflyt.dto

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import no.nav.bidrag.arbeidsflyt.model.Fagomrade
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PatchOppgaveRequestTest {
    @Test
    fun `skal utelate metadata når referanser og normdato mangler`() {
        val request = gjenopprettSøknadsoppgave()

        request.metadata shouldBe null
        jacksonObjectMapper().valueToTree<com.fasterxml.jackson.databind.JsonNode>(request)["metadata"].isNull shouldBe true
    }

    @Test
    fun `skal beholde søknadsreferanse når behandlingsreferanse og normdato mangler`() {
        val request = gjenopprettSøknadsoppgave(søknadsid = 123)

        request.metadata shouldBe mapOf(METADATA_NØKKEL_SØKNAD_ID to "123")
        jacksonObjectMapper().valueToTree<com.fasterxml.jackson.databind.JsonNode>(request)["metadata"].size() shouldBe 1
    }

    @Test
    fun `skal serialisere UpdateOppgaveAfterOpprettRequest`() {
        val opprinneligOppgave =
            OppgaveData(status = OppgaveStatus.AAPNET, tema = Fagomrade.BIDRAG, tildeltEnhetsnr = "007", versjon = 2, id = 1)

        val updateOppgaveAfterOpprettRequest = UpdateOppgaveAfterOpprettRequest(opprinneligOppgave, "JOARK-test")
        val stringValue = jacksonObjectMapper().writer().writeValueAsString(updateOppgaveAfterOpprettRequest)
        val expectedValue = "{\"journalpostId\":\"JOARK-test\",\"id\":1,\"versjon\":2}"

        assertThat(stringValue).`as`("Expected json string value").contains(expectedValue)
    }

    @Test
    fun `skal serialisere FerdigstillOppgaveRequest`() {
        val opprinneligOppgave =
            OppgaveData(status = OppgaveStatus.AAPNET, tema = Fagomrade.BIDRAG, tildeltEnhetsnr = "007", versjon = 2, id = 1)

        val ferdigstillOppgaveRequest = FerdigstillOppgaveRequest(opprinneligOppgave)
        val stringValue = jacksonObjectMapper().writer().writeValueAsString(ferdigstillOppgaveRequest)

        val expectedValue =
            "{" +
                "\"status\":\"FERDIGSTILT\"," +
                "\"id\":1," +
                "\"versjon\":2" +
                "}"

        assertThat(stringValue).`as`("Expected json string value").contains(expectedValue)
    }

    @Test
    fun `skal serialisere OverforOppgaveRequest`() {
        val opprinneligOppgave =
            OppgaveData(status = OppgaveStatus.AAPNET, tema = Fagomrade.BIDRAG, tildeltEnhetsnr = "007", versjon = 2, id = 1)

        val overforOppgaveRequest = OverforOppgaveRequest(opprinneligOppgave, "4812", "Z999999")
        val stringValue = jacksonObjectMapper().writer().writeValueAsString(overforOppgaveRequest)
        val expectedValue =
            "" +
                "{\"tildeltEnhetsnr\":\"4812\"," +
                "\"id\":1," +
                "\"versjon\":2," +
                "\"tilordnetRessurs\":\"\""

        assertThat(stringValue).`as`("Expected json string value").contains(expectedValue)
        assertThat(stringValue).`as`("Expected json string value").contains("Oppgave overført fra enhet 007 til 4812")
        assertThat(stringValue).`as`("Expected json string value").contains("Saksbehandler endret fra Z999999 til ikke valgt")
    }

    @Test
    fun `skal serialisere OpprettOppgaveRequest`() {
        val opprettOppgaveRequest = OpprettJournalforingsOppgaveRequest("1234", "123213")
        val stringValue = jacksonObjectMapper().writer().writeValueAsString(opprettOppgaveRequest)

        assertThat(stringValue)
            .`as`("Expected json string value")
            .containsPattern("\"opprettetAvEnhetsnr\":\"9999\"")
            .containsPattern("\"prioritet\":\"HOY\"")
            .containsPattern("\"oppgavetype\":\"JFR\"")
            .containsPattern("\"tema\":\"BID\"")
            .containsPattern("\"aktoerId\":\"123213\"")
            .containsPattern("\"journalpostId\":\"1234\"")
    }

    private fun gjenopprettSøknadsoppgave(søknadsid: Long? = null) = GjenopprettSøknadsoppgaveRequest(
        søknadsid = søknadsid,
        behandlingsid = null,
        normDato = null,
        frist = LocalDate.parse("2026-01-05"),
        aktivDato = "2026-01-01",
        ignorerNormDatoHvisIkkeFinnes = true,
        opprettetAvEnhetsnr = "4806",
        beskrivelse = "Test",
        saksreferanse = "1234567",
        tildeltEnhetsnr = "4806",
        personident = null,
        tilordnetRessurs = null,
        behandlesAvApplikasjon = null,
        oppgavetype = OppgaveType.BEH_SAK,
        tema = "BID",
    )
}
