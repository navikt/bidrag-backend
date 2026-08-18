package no.nav.bidrag.aktoerregister.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Representerer navn for en bidragsaktør.")
data class DodsboDTO(

    @param:Schema(description = "Navn på kontaktperson for dødsboet.")
    val kontaktperson: String? = null,

    val adresse: AdresseDTO? = null,
)
