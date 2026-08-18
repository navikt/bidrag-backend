package no.nav.bidrag.sak.domain.projections

import no.nav.bidrag.domene.enums.rolle.Rolletype
import no.nav.bidrag.domene.ident.Personident

data class RollePipInfo(
    val rolleType: Rolletype,
    val samhandlerIdent: String?,
    val fødselsnummer: String?,
) {
    fun erPerson(): Boolean = rolleType != Rolletype.REELMOTTAKER &&
        harIkkeSamhandlerIdent() &&
        harFødselsnummerPaElleveSiffer()

    private fun harIkkeSamhandlerIdent(): Boolean = samhandlerIdent.isNullOrBlank()

    private fun harFødselsnummerPaElleveSiffer(): Boolean = fødselsnummer?.let { Personident(it).gyldig() } ?: false
}
