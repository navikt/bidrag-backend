package no.nav.bidrag.domene.enums.person

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class Sivilstandskode {
    GIFT_SAMBOER, // Hvis sivilstand er gift, samboer eller registrert partner
    BOR_ALENE_MED_BARN, // Alle andre sivilstander,
    ENSLIG,
    SAMBOER,
    UKJENT,
}
