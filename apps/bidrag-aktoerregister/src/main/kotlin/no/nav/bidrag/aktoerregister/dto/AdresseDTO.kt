package no.nav.bidrag.aktoerregister.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Representerer navn og/eller adresse for en bidragsaktør. TODO: Beskrivelse av felter må kvalitetssikres.")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AdresseDTO(

    @param:Schema(description = "Aktørens navn", deprecated = true)
    @Deprecated("Flyttet til NavnDTO.")
    val navn: String? = null,

    @param:Schema(description = "Første adresselinje inneholder normalt gatenavn, men kan også innehold f.eks c/o.")
    val adresselinje1: String? = null,

    @param:Schema(description = "Andre adresselinje brukes primært i utlandsadresser, hvor postnr og poststed ikke er tilgjengelig som strukturerte data.")
    val adresselinje2: String? = null,

    @param:Schema(description = "Tredje adresselinje brukes i noen tilfeller til region.")
    val adresselinje3: String? = null,

    @param:Schema(description = "Leilighetsnummer dersom dette er tilgjengelig som strukturerte data.")
    val leilighetsnummer: String? = null,

    @param:Schema(description = "Postnr dersom dette er tilgjengelig som strukturerte data.")
    val postnr: String? = null,

    @param:Schema(description = "Poststed dersom dette er tilgjengelig som strukturerte data.")
    val poststed: String? = null,

    @param:Schema(description = "Land som 3-bokstavs land-kode.")
    val land: String? = null,
)
