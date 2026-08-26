package no.nav.bidrag.domene.enums.inntekt

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class Skattegrunnlagstype {
    ORDINÆR,
    SVALBARD,
}
