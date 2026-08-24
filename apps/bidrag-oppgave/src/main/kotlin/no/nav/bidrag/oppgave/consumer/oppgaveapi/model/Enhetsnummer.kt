package no.nav.bidrag.oppgave.consumer.oppgaveapi.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Nav-enhetsnummer (4 siffer), f.eks. "4100".
 *
 * Wrapper for det som tidligere lå i `no.nav.common.types.Enhetsnummer`.
 */
@JvmInline
value class Enhetsnummer
@JsonCreator constructor(
    @get:JsonValue val verdi: String,
) {
    override fun toString(): String = verdi
}
