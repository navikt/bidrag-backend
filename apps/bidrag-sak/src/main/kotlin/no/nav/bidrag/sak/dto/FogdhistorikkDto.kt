package no.nav.bidrag.sak.dto

import no.nav.bidrag.domene.enums.sak.Fogdårsak
import no.nav.bidrag.domene.enums.sak.Tilgangstype
import no.nav.bidrag.sak.domain.Tilgang
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

fun Tilgang.tilFogdhistorikkDto(): FogdhistorikkDto = FogdhistorikkDto(
    tilgangId = tilgangId,
    enhetsnummer = enhetsnummer,
    tilgangFomDato = tilgangFomDato,
    tilgangTomDato = tilgangTomDato,
    arsak = årsak,
    type = type,
    opprettetAv = opprettetAv,
)
