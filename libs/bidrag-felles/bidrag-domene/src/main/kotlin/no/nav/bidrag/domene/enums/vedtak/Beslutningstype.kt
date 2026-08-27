package no.nav.bidrag.domene.enums.vedtak

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class Beslutningstype {
    AVVIST,
    STADFESTELSE,
    ENDRING,
    DELVEDTAK,
}
