package no.nav.bidrag.domene.sak

import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.domene.ident.Personident

data class Stønadsid(
    val type: Stønadstype,
    val kravhaver: Personident,
    val skyldner: Personident,
    val sak: Saksnummer,
) {
    override fun toString(): String = "Stønadsid(type=$type, kravhaver=${kravhaver.verdi}, skyldner=${skyldner.verdi}, sak=${sak.verdi})"

    fun toReferanse(): String = "stønadsid_type_${type}_kravhaver_${kravhaver.verdi}_skyldner_${skyldner.verdi}_sak_${sak.verdi}"
}

data class StønadRoller(
    val kravhaver: Personident,
    val skyldner: Personident,
    val sak: Saksnummer,
)
