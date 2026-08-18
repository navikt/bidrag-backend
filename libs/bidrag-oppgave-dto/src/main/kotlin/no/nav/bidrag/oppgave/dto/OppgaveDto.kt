package no.nav.bidrag.oppgave.dto

import java.time.OffsetDateTime

data class OppgaveDto(
    val id: Long,
    val tittel: String,
    val beskrivelse: String? = null,
    val status: OppgaveStatus = OppgaveStatus.OPPRETTET,
    val opprettet: OffsetDateTime = OffsetDateTime.now(),
)

enum class OppgaveStatus {
    OPPRETTET,
    UNDER_BEHANDLING,
    FERDIG,
}
