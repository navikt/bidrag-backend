package no.nav.bidrag.transport.person

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HusstandsmedlemmerDto(
    @Schema(description = "Periodiser liste over husstander og dens medlemmer i perioden")
    val husstandListe: List<Husstand> = emptyList(),
)

data class Husstand(
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
    val adressenavn: String? = null,
    val husnummer: String? = null,
    val husbokstav: String? = null,
    val bruksenhetsnummer: String? = null,
    val postnummer: String? = null,
    val bydelsnummer: String? = null,
    val kommunenummer: String? = null,
    val matrikkelId: Long? = null,
    val husstandsmedlemListe: List<Husstandsmedlem>,
)

data class Husstandsmedlem(
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
    val personId: Personident,
    val navn: String,
    val fødselsdato: LocalDate? = null,
    val dødsdato: LocalDate? = null,
    @Schema(deprecated = true)
    @Deprecated("Skrivefeil", ReplaceWith("fødselsdato"))
    val foedselsdato: LocalDate? = fødselsdato,
    @Schema(deprecated = true)
    @Deprecated("Skrivefeil", ReplaceWith("dødsdato"))
    val doedsdato: LocalDate? = dødsdato,
)
