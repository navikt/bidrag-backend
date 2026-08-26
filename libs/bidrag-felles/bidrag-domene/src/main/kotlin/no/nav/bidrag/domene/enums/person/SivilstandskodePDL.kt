package no.nav.bidrag.domene.enums.person

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class SivilstandskodePDL {
    GIFT,
    UGIFT,
    UOPPGITT,
    ENKE_ELLER_ENKEMANN,
    SKILT,
    SEPARERT,
    REGISTRERT_PARTNER,
    SEPARERT_PARTNER,
    SKILT_PARTNER,
    GJENLEVENDE_PARTNER,
}
