package no.nav.bidrag.transport.person

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident
import java.time.LocalDate

data class Fødselsdatoer(
    @Schema(description = "Map med ident til fødselsdato-elementer.")
    val identerTilDatoer: Map<Personident, LocalDate?>,
)
