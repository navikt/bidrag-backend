package no.nav.bidrag.transport.samhandler

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.land.Landkode3

@Schema(description = "Representerer navn og/eller adresse for en samhandler.")
data class AdresseDto(
    @Schema(description = "Første adresselinje inneholder normalt gatenavn, men kan også innehold f.eks c/o.")
    val adresselinje1: String? = null,
    @Schema(
        description =
            "Andre adresselinje brukes primært i utlandsadresser, " +
                "hvor postnr og poststed ikke er tilgjengelig som strukturerte data.",
    )
    val adresselinje2: String? = null,
    @Schema(description = "Tredje adresselinje brukes i noen tilfeller til region.")
    val adresselinje3: String? = null,
    @Deprecated(
        message = "Bruk postnnummer.",
        replaceWith = ReplaceWith("postnummer"),
    )
    val postnr: String? = null,
    @Schema(description = "Postnummer dersom dette er tilgjengelig som strukturerte data.")
    val postnummer: String? = null,
    @Schema(description = "Poststed dersom dette er tilgjengelig som strukturerte data.")
    val poststed: String? = null,
    @Schema(description = "Land. ISO 3166-1 alfa-3.")
    val land: Landkode3? = null,
)
