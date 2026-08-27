package no.nav.bidrag.oppgave.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime

class OppgaveBeskrivelseParserTest {

    private val beskrivelseMedFlereInnslag =
        """
        --- 28.12.2016 20:17 Automatisk jobb ---
        Test-kommentar 1
        Linje 2 av kommentaren
        · Frist endret fra 01.01.2017 til 01.05.2017

        --- 31.12.2016 23:59 Saksbehandler, Per (S161234, 4802) ---
        · Frist endret fra 01.05.2017 til 01.05.2019
        --- 02.01.2016 15:00 Saksbehandler, Per (S161234, 4802) ---
        Hurra! Ferdig!
        """.trimIndent()

    @Test
    fun `parser flere innslag og beholder rekkefolgen fra kilden`() {
        val innslag = OppgaveBeskrivelseParser.parse(beskrivelseMedFlereInnslag)!!

        assertThat(innslag).hasSize(3)

        val autojobb = innslag[0]
        assertThat(autojobb.tidspunkt).isEqualTo(LocalDateTime.of(2016, 12, 28, 20, 17))
        assertThat(autojobb.saksbehandlerNavn).isEqualTo("Automatisk jobb")
        assertThat(autojobb.saksbehandlerId).isNull()
        assertThat(autojobb.kommentar).isEqualTo("Test-kommentar 1\nLinje 2 av kommentaren")
        assertThat(autojobb.endringer).containsExactly("Frist endret fra 01.01.2017 til 01.05.2017")

        val saksbehandler = innslag[1]
        assertThat(saksbehandler.tidspunkt).isEqualTo(LocalDateTime.of(2016, 12, 31, 23, 59))
        assertThat(saksbehandler.saksbehandlerNavn).isEqualTo("Saksbehandler, Per")
        assertThat(saksbehandler.saksbehandlerId).isEqualTo("S161234")
        assertThat(saksbehandler.enhetsnr).isEqualTo("4802")
        assertThat(saksbehandler.kommentar).isNull()
        assertThat(saksbehandler.endringer).containsExactly("Frist endret fra 01.05.2017 til 01.05.2019")

        assertThat(innslag[2].kommentar).isEqualTo("Hurra! Ferdig!")
    }

    @Test
    fun `handterer bade LF og CRLF som linjeskift`() {
        val innslag = OppgaveBeskrivelseParser.parse(
            "--- 28.12.2016 20:17 Automatisk jobb ---\r\nEn kommentar\r\n· En endring",
        )!!

        assertThat(innslag).hasSize(1)
        assertThat(innslag.single().kommentar).isEqualTo("En kommentar")
        assertThat(innslag.single().endringer).containsExactly("En endring")
    }

    @Test
    fun `header med enhetsnavn i tillegg til enhetsnr gir kun enhetsnr`() {
        val innslag = OppgaveBeskrivelseParser.parse(
            "--- 31.12.2016 23:59 Gjøresak, Iver (G161234, 4802 NAV Familie og pensjonsytelser) ---",
        )!!

        assertThat(innslag.single().enhetsnr).isEqualTo("4802")
        assertThat(innslag.single().saksbehandlerId).isEqualTo("G161234")
    }

    @Test
    fun `linje som starter med bindestreker men ikke er gyldig header blir kommentar`() {
        val innslag = OppgaveBeskrivelseParser.parse(
            "--- 28.12.2016 20:17 Automatisk jobb ---\n--- ikke en header ---",
        )!!

        assertThat(innslag).hasSize(1)
        assertThat(innslag.single().kommentar).isEqualTo("--- ikke en header ---")
    }

    @Test
    fun `tekst uten header havner i default-innslag utledet fra sist endret`() {
        val innslag = OppgaveBeskrivelseParser.parse(
            beskrivelse = "En gammel beskrivelse uten header",
            sistEndretTidspunkt = OffsetDateTime.parse("2026-01-15T10:30:00+01:00"),
            sistEndretAv = "Z999999",
            sistEndretEnhetsnr = "4806",
        )!!

        val enkeltinnslag = innslag.single()
        assertThat(enkeltinnslag.tidspunkt).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 30))
        assertThat(enkeltinnslag.saksbehandlerId).isEqualTo("Z999999")
        assertThat(enkeltinnslag.enhetsnr).isEqualTo("4806")
        assertThat(enkeltinnslag.kommentar).isEqualTo("En gammel beskrivelse uten header")
    }

    @Test
    fun `default-innslag fra srvbisys markeres som automatisk jobb`() {
        val innslag = OppgaveBeskrivelseParser.parse(
            beskrivelse = "Opprettet av en autojobb",
            sistEndretTidspunkt = OffsetDateTime.parse("2026-01-15T10:30:00+01:00"),
            sistEndretAv = "srvbisys",
        )!!

        assertThat(innslag.single().saksbehandlerNavn).isEqualTo("Automatisk jobb")
        assertThat(innslag.single().saksbehandlerId).isNull()
    }

    @Test
    fun `enhetsnr i default-innslag settes kun nar verdien er rene siffer`() {
        val innslag = OppgaveBeskrivelseParser.parse(
            beskrivelse = "En kommentar",
            sistEndretTidspunkt = OffsetDateTime.parse("2026-01-15T10:30:00+01:00"),
            sistEndretAv = "Z999999",
            sistEndretEnhetsnr = "4806 NAV Familie og pensjonsytelser",
        )!!

        assertThat(innslag.single().enhetsnr).isNull()
    }

    @Test
    fun `tom eller manglende beskrivelse gir tom liste og ikke null`() {
        assertThat(OppgaveBeskrivelseParser.parse(null)).isEmpty()
        assertThat(OppgaveBeskrivelseParser.parse("")).isEmpty()
        assertThat(OppgaveBeskrivelseParser.parse("   \n  ")).isEmpty()
    }

    @Test
    fun `ugyldig dato i header gir null istedenfor exception`() {
        val innslag = OppgaveBeskrivelseParser.parse("--- 31.02.2017 23:59 Automatisk jobb ---")

        assertThat(innslag).isNull()
    }
}
