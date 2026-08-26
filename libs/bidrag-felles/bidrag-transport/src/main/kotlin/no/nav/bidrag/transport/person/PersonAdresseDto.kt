package no.nav.bidrag.transport.person

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.adresse.Adressetype
import no.nav.bidrag.domene.land.Landkode2
import no.nav.bidrag.domene.land.Landkode3

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonAdresseDto(
    @Schema(description = "Gyldige adressetyper: BOSTEDSADRESSE, KONTAKTADRESSE, eller OPPHOLDSADRESSE")
    val adressetype: Adressetype,
    @Schema(description = "Adresselinje 1")
    val adresselinje1: String? = null,
    @Schema(description = "Adresselinje 2")
    val adresselinje2: String? = null,
    @Schema(description = "Adresselinje 3")
    val adresselinje3: String? = null,
    @Schema(description = "Bruksenhetsnummer")
    val bruksenhetsnummer: String? = null,
    @Schema(description = "Postnummer, tilgjengelig hvis norsk adresse")
    val postnummer: String? = null,
    @Schema(description = "Poststed, tilgjengelig hvis norsk adresse")
    val poststed: String? = null,
    @Schema(description = "To-bokstavers landkode ihht iso3166-1 alfa-2")
    val land: Landkode2,
    @Schema(description = "Trebokstavs landkode ihht iso3166-1 alfa-3")
    val land3: Landkode3,
)
