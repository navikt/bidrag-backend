package no.nav.bidrag.domene.enums.diverse

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class InntektBeløpstype {
    @Schema(name = "ÅRSBELØP", description = "Beløp er et årsbeløp")
    ÅRSBELØP,
    MÅNEDSBELØP,
    MÅNEDSBELØP_11_MÅNEDER,
    DAGSATS,
}
