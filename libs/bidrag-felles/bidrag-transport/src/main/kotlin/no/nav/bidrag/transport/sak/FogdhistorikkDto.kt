package no.nav.bidrag.transport.sak

import no.nav.bidrag.domene.enums.sak.Fogdårsak
import no.nav.bidrag.domene.enums.sak.Tilgangstype
import java.time.LocalDate

data class FogdhistorikkDto(
    val tilgangId: Int,
    val enhetsnummer: String,
    val tilgangFomDato: LocalDate,
    val tilgangTomDato: LocalDate? = null,
    val arsak: Fogdårsak,
    val type: Tilgangstype,
    val opprettetAv: String? = null,
) {
    val arsakBeskrivelse: String
        get() = arsak.beskrivelse
    val typeBeskrivelse: String
        get() = type.beskrivelse
}
