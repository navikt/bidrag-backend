package no.nav.bidrag.oppgaveapiclient

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Nav-ident for en saksbehandler/ressurs, f.eks. "Z999999".
 *
 * Wrapper for det som tidligere lå i `no.nav.common.types.NavIdent`.
 */
@JvmInline
value class NavIdent
@JsonCreator constructor(
    @get:JsonValue val verdi: String,
) {
    override fun toString(): String = verdi
}
