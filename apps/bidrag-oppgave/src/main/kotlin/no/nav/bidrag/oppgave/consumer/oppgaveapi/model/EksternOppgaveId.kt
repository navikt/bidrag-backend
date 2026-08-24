package no.nav.bidrag.oppgave.consumer.oppgaveapi.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Syntetisk id for en oppgave i oppgave-apiet.
 *
 * Wrapper for det som tidligere lå i `no.nav.common.types.EksternOppgaveId`.
 * Serialiseres som et tall (JSON number) og [toString] gir råverdien slik at
 * den kan brukes direkte i URI-er og query-parametere.
 */
@JvmInline
value class EksternOppgaveId
@JsonCreator constructor(
    @get:JsonValue val verdi: Long,
) {
    override fun toString(): String = verdi.toString()
}
