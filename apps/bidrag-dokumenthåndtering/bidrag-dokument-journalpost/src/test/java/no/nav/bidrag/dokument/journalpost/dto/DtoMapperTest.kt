package no.nav.bidrag.dokument.journalpost.dto

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostLocalTest
import no.nav.bidrag.dokument.journalpost.BidragDokumentJournalpostProfiles
import no.nav.bidrag.dokument.journalpost.model.SaksbehandlersEnhet
import no.nav.bidrag.transport.dokument.AvvikType
import no.nav.bidrag.transport.dokument.Avvikshendelse
import no.nav.bidrag.transport.dokument.JournalpostDto
import no.nav.bidrag.transport.felles.commonObjectmapper
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.io.IOException
import java.time.LocalDate

@DisplayName("Ved REST-kall")
@ActiveProfiles(BidragDokumentJournalpostProfiles.TEST)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [BidragDokumentJournalpostLocalTest::class],
)
@EnableWireMock(ConfigureWireMock(port = 0))
@EnableMockOAuth2Server
internal class DtoMapperTest {
    private val objectMapper: ObjectMapper = commonObjectmapper

    @Test
    @DisplayName("skal mappe JournalpostDto til json")
    @Throws(
        JsonProcessingException::class,
    )
    fun skalMappeJournalpostDtoTilJson() {
        val journalpostDto =
            JournalpostDto(
                avsenderNavn = "G. Rav Laks",
                innhold = "Jummy",
            )
        val json = objectMapper!!.writeValueAsString(journalpostDto)
        Assertions
            .assertThat(json)
            .contains("\"avsenderNavn\":\"G. Rav Laks\"")
            .contains("\"innhold\":\"Jummy\"")
    }

    @Test
    @DisplayName("skal mappe Avvikshendelse til json og tilbake")
    @Throws(
        IOException::class,
    )
    fun skalMappeAvvikshendelselTilJson() {
        val json =
            objectMapper!!.writeValueAsString(Avvikshendelse(AvvikType.BESTILL_ORIGINAL.name, ""))
        Assertions.assertThat(json).contains("\"avvikType\":\"BESTILL_ORIGINAL\"")
        val deserialisert = objectMapper.readValue(json, Avvikshendelse::class.java)
        Assertions
            .assertThat(deserialisert)
            .isEqualTo(Avvikshendelse(AvvikType.BESTILL_ORIGINAL.name, ""))
    }

    @Test
    @DisplayName("skal mappe Oppgave til json")
    @Throws(
        IOException::class,
    )
    fun skalMappeOppgaveTilJson() {
        val oppgave = TestOppgave("34111047", "3411104760", "Anyway...", "MOT", "SR", "1001")
        val json = objectMapper!!.writeValueAsString(oppgave)
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(json).contains("\"journalpostId\":\"34111047\"") },
            Executable { Assertions.assertThat(json).contains("\"saksreferanse\":\"3411104760\"") },
            Executable { Assertions.assertThat(json).contains("\"tema\":\"MOT\"") },
            Executable { Assertions.assertThat(json).contains("\"beskrivelse\":\"Anyway...\"") },
            Executable { Assertions.assertThat(json).contains("\"tildeltEnhetsnr\":\"1001\"") },
            Executable { Assertions.assertThat(json).contains("\"opprettetAvEnhetsnr\":\"1001\"") },
            Executable {
                Assertions.assertThat(json).contains("\"aktivDato\":\"" + LocalDate.now() + "\"")
            },
        )
    }

    @Test
    @DisplayName("skal ikke inneholde enhetsnummerTilAvviksbehandler fra Enheter")
    @Throws(
        JsonProcessingException::class,
    )
    fun skalIkkeInneholdeEnhetsnummerTilAvviksbehandlerFraEnheter() {
        val oppgave = TestOppgave("-1", "na", "na", "na", "na", "-1")
        val json = objectMapper!!.writeValueAsString(oppgave)
        Assertions.assertThat(json).doesNotContain("enhetsnummerTilAvviksbehandler")
    }

    @Test
    @DisplayName("skal mappe OppgaveType for BestillOriginalOppgave til json")
    @Throws(
        IOException::class,
    )
    fun skalMappeOppgaveTypeForBestillOriginalOppgaveTilJson() {
        val oppgave =
            BestillOriginalOppgave(
                101,
                "1001",
                LocalDate.now(),
                "BJORAKxyz",
                "123213",
                SaksbehandlersEnhet("1001"),
            )
        Assertions
            .assertThat(objectMapper!!.writeValueAsString(oppgave))
            .contains("\"oppgavetype\":\"SR\"")
    }

    @Test
    @DisplayName("skal mappe OppgaveType for BestillReskanningOppgave til json")
    @Throws(
        IOException::class,
    )
    fun skalMappeOppgaveTypeForBestillReskanningOppgaveTilJson() {
        val oppgave =
            BestillReskanningOppgave(
                101,
                "1001",
                LocalDate.now(),
                "BJORAKxyz",
                null,
                "123213",
                SaksbehandlersEnhet("1001"),
            )
        Assertions
            .assertThat(objectMapper!!.writeValueAsString(oppgave))
            .contains("\"oppgavetype\":\"SR\"")
    }

    @Test
    @DisplayName("skal mappe oppgavetype og prioritet for BestillSplittingOppgave til json")
    @Throws(
        IOException::class,
    )
    fun skalMappeOppgaveTypeForBestillSplittingOppgaveTilJson() {
        val oppgave =
            BestillSplittingOppgave(
                -1,
                "1001",
                LocalDate.now(),
                "BJORAKxyz",
                "fila.txt",
                "på midten",
                "123213",
                SaksbehandlersEnhet("1001"),
            )
        val json = objectMapper!!.writeValueAsString(oppgave)
        org.junit.jupiter.api.Assertions.assertAll(
            Executable { Assertions.assertThat(json).contains("\"oppgavetype\":\"SR\"") },
            Executable { Assertions.assertThat(json).contains("\"prioritet\":\"HOY\"") },
        )
    }

    private class TestOppgave internal constructor(
        journalpostId: String?,
        saksreferanse: String?,
        override val beskrivelse: String,
        fagomrade: String?,
        oppgavetype: String?,
        tildeltEnhetsnr: String?,
    ) : Oppgave(
        LocalDate.now().toString(),
        journalpostId!!,
        "",
        saksreferanse,
        fagomrade!!,
        oppgavetype!!,
        "HØY",
        tildeltEnhetsnr!!,
        SaksbehandlersEnhet(tildeltEnhetsnr),
        "123213",
    ) {
        public override fun hentBeskrivelse(): String = beskrivelse

        override fun toString(): String = super.toString()
    }
}
