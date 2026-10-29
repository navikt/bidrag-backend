package no.nav.bidrag.sak.domain

import no.nav.bidrag.domene.enums.rolle.Rolletype

const val FØRSTE_OBJEKTNUMMER_BARN = 3

enum class Forelderrolle(
    val rolletype: Rolletype,
    val objektnummer: String,
) {
    BIDRAGSPLIKTIG(Rolletype.BIDRAGSPLIKTIG, "01"),
    BIDRAGSMOTTAKER(Rolletype.BIDRAGSMOTTAKER, "02"),
    ;

    companion object {
        fun fra(rolletype: Rolletype) = entries.find { it.rolletype == rolletype }
    }
}
