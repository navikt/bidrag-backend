package no.nav.bidrag.domene.enums.privatavtale

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class PrivatAvtaleType {
    PRIVAT_AVTALE,
    DOM_RETTSFORLIK,
    VEDTAK_FRA_NAV,
}
