package no.nav.bidrag.transport.person

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.person.Skifteform
import no.nav.bidrag.domene.land.Landkode3
import java.time.LocalDate

data class DødsboDto(
    @Schema(description = "Fra Tingretten angis skifteformen for booppgjøret.")
    val skifteform: Skifteform,
    @Schema(description = "Attestutstedelsesdato for skifteattest.")
    val attestutstedelsesdato: LocalDate,
    @Schema(description = "Kontaktadresse i Norge eller utlandet for person, advokat eller oganisasjon.")
    val kontaktadresse: DødsboKontaktadresse,
    @Schema(description = "Navn på kontaktperson for dødsboet. Dette kan være en person, advokat eller oganisasjon.")
    val kontaktperson: String,
)

data class DødsboKontaktadresse(
    @Schema(description = "Adresselinje 1")
    val adresselinje1: String,
    @Schema(description = "Adresselinje 2")
    val adresselinje2: String? = null,
    @Schema(description = "Postnummer.")
    val postnummer: String,
    @Schema(description = "Poststed")
    val poststed: String,
    @Schema(description = "Landkode 3 siffer.")
    val land3: Landkode3?,
)
