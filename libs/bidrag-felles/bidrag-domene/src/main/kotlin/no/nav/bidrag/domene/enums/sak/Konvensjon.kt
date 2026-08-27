@file:Suppress("unused")

package no.nav.bidrag.domene.enums.sak

enum class Konvensjon(
    private val beskrivelse: String,
) {
    AiS("Annet - iSupport"),
    HiS("Haag 2007 - iSupport"),
    H5("Haag"),
    L("Lugano"),
    NI("Nordisk innkreving"),
    NY("New York"),
    US("USA-avtalen"),
    H73("Haag 1973"),
    INGEN("Ingen"),
    ;

    override fun toString(): String = beskrivelse
}
