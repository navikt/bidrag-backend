package no.nav.bidrag.oppgave.dto

import java.time.LocalDateTime
import java.time.OffsetDateTime

data class OppgaveDto(
    val id: Long,
    val tittel: String,
    val beskrivelse: String? = null,
    /**
     * Strukturert tolkning av [beskrivelse]. Rekkefølgen er som i kilden (nyeste innslag først).
     *
     * `null` betyr at vi ikke klarte å utlede noen historikk. Tom liste betyr at tolkningen lyktes,
     * men at det ikke fantes noen innslag.
     */
    val beskrivelseshistorikk: List<Beskrivelseinnslag>? = null,
    val status: OppgaveStatus = OppgaveStatus.OPPRETTET,
    val opprettet: OffsetDateTime? = null,
)

data class Beskrivelseinnslag(
    val tidspunkt: LocalDateTime? = null,
    val saksbehandlerNavn: String? = null,
    val saksbehandlerId: String? = null,
    val enhetsnr: String? = null,
    val kommentar: String? = null,
    val endringer: List<String> = emptyList(),
)

enum class OppgaveStatus {
    OPPRETTET,
    UNDER_BEHANDLING,
    FERDIG,
}
