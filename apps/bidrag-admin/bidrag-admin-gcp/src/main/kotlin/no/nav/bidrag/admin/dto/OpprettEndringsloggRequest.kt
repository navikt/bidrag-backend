@file:Suppress("ktlint:standard:filename")

package no.nav.bidrag.admin.dto

import no.nav.bidrag.admin.persistence.entity.EndringsloggTilhørerSkjermbilde
import no.nav.bidrag.admin.persistence.entity.Endringstype
import java.time.LocalDate

data class OppdaterEndringsloggRequest(
    val tittel: String? = null,
    val tilhørerSkjermbilde: EndringsloggTilhørerSkjermbilde? = null,
    val sammendrag: String? = null,
    val innhold: String? = null,
    val erPåkrevd: Boolean? = null,
    val aktivFraTidspunkt: LocalDate? = null,
    val aktivTilTidspunkt: LocalDate? = null,
    val endringer: List<OppdaterEndringsloggEndring>? = null,
    val endringstyper: List<Endringstype>? = null,
)

data class OppdaterEndringsloggEndring(
    val id: Long,
    val tittel: String? = null,
    val innhold: String? = null,
    val endringstype: Endringstype? = null,
)

data class OpprettEndringsloggRequest(
    val tittel: String,
    val tilhørerSkjermbilde: EndringsloggTilhørerSkjermbilde,
    val sammendrag: String,
    val innhold: String? = null,
    val erPåkrevd: Boolean = false,
    val aktivFraTidspunkt: LocalDate? = null,
    val aktivTilTidspunkt: LocalDate? = null,
    val endringstyper: List<Endringstype> = emptyList(),
    val endringer: List<LeggTilEndringsloggEndring>? = null,
)

data class LeggTilEndringsloggEndring(
    val tittel: String,
    val innhold: String,
    val endringstype: Endringstype = Endringstype.ENDRING,
)
