package no.nav.bidrag.oppgaveapiclient

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Id for en journalpostreferanse (fra arkivet/Joark).
 *
 * Wrapper for det som tidligere lå i `no.nav.common.types.EksternJournalpostId`.
 * Modelleres som streng for å være robust mot ledende nuller og fremtidige formater.
 */
@JvmInline
value class EksternJournalpostId
@JsonCreator constructor(
    @get:JsonValue val verdi: String,
) {
    override fun toString(): String = verdi
}
