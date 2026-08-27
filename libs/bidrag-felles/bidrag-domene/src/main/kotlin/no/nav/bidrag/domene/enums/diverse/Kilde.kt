package no.nav.bidrag.domene.enums.diverse

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class Kilde {
    MANUELL,
    OFFENTLIG,
}
