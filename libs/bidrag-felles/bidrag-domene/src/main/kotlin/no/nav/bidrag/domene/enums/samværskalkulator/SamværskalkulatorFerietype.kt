package no.nav.bidrag.domene.enums.samværskalkulator

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class SamværskalkulatorFerietype {
    JUL_NYTTÅR,
    VINTERFERIE,
    PÅSKE,
    SOMMERFERIE,
    HØSTFERIE,
    ANNET,
}
