package no.nav.bidrag.domene.enums.samværskalkulator

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class SamværskalkulatorNetterFrekvens {
    HVERT_ÅR,
    ANNET_HVERT_ÅR,
}
