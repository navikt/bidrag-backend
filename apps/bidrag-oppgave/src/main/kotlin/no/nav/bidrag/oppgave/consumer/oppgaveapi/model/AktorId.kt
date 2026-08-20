package no.nav.bidrag.oppgave.consumer.oppgaveapi.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Syntetisk id for en person (13-sifret aktørid, kan veksles via PDL).
 *
 * Wrapper for det som tidligere lå i `no.nav.common.types.AktorId`.
 * Modelleres som streng for å bevare ledende nuller.
 */
@JvmInline
value class AktorId
@JsonCreator constructor(
    @get:JsonValue val verdi: String,
) {
    override fun toString(): String = verdi
}
