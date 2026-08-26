package no.nav.bidrag.transport.person

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.person.Gradering
import no.nav.bidrag.domene.ident.Personident

data class Graderingsinfo(
    @Schema(description = "Map med ident til gradering.")
    val identerTilGradering: Map<Personident, Gradering?>,
    @Schema(description = "Hvor vidt hovedident fra GraderingQuery er skjerment.")
    val identerTilSkjerming: Map<Personident, Boolean>,
)
