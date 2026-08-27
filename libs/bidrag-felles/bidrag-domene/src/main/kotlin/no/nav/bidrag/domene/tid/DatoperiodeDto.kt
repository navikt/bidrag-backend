package no.nav.bidrag.domene.tid

import java.time.LocalDate

data class DatoperiodeDto(
    var fom: LocalDate,
    var tom: LocalDate? = null,
)
