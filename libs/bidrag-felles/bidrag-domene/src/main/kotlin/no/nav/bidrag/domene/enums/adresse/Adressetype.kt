@file:Suppress("unused")

package no.nav.bidrag.domene.enums.adresse

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class Adressetype {
    BOSTEDSADRESSE,
    KONTAKTADRESSE,
    OPPHOLDSADRESSE,
    DELT_BOSTED,
}
