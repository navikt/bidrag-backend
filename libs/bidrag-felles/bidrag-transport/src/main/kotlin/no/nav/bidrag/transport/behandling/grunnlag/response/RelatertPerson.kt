package no.nav.bidrag.transport.behandling.grunnlag.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.person.Familierelasjon
import java.time.LocalDate
import java.time.LocalDateTime

data class RelatertPersonDto(
    @Schema(description = "Personid til BM/BP")
    val partPersonId: String?,
    @Schema(description = "Personid til relatert person. Dette er husstandsmedlem eller barn av BM/BP")
    val relatertPersonPersonId: String?,
    @Schema(description = "Navn på den relaterte personen, format <Fornavn, mellomnavn, Etternavn")
    val navn: String?,
    @Schema(description = "Den relaterte personens fødselsdato")
    val fodselsdato: LocalDate?,
    @Schema(description = "Angir om den relaterte personen er barn av BM/BP")
    val erBarnAvBmBp: Boolean,
    @Schema(description = "Angir om en grunnlagsopplysning er aktiv")
    val aktiv: Boolean,
    @Schema(description = "Tidspunkt grunnlaget tas i bruk")
    val brukFra: LocalDateTime,
    @Schema(description = "Tidspunkt grunnlaget ikke lenger er aktivt. Null betyr at grunnlaget er aktivt")
    val brukTil: LocalDateTime?,
    @Schema(description = "Hentet tidspunkt")
    val hentetTidspunkt: LocalDateTime,
    @Schema(description = "Liste over perioder personen bor i samme husstand som BM/BP")
    val borISammeHusstandDtoListe: List<BorISammeHusstandDto>,
)

data class RelatertPersonGrunnlagDto(
    @Schema(description = "Personid til BM/BP")
    val partPersonId: String?,
    @Schema(description = "Personid til relatert person. Dette er husstandsmedlem eller barn av BM/BP")
    @Deprecated("relatertPersonPersonId er erstattet av gjelderPerson", ReplaceWith("gjelderPerson"))
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    var relatertPersonPersonId: String? = null,
    @Schema(description = "Personid til relatert person. Dette er husstandsmedlem eller barn av BM/BP")
    val gjelderPersonId: String? = relatertPersonPersonId,
    @Schema(description = "Navn på den relaterte personen, format <Fornavn, mellomnavn, Etternavn")
    val navn: String?,
    @Schema(description = "Den relaterte personens fødselsdato")
    val fødselsdato: LocalDate?,
    @Deprecated("Erstattet av relasjon == BARN", ReplaceWith("relasjon"))
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    var erBarnAvBmBp: Boolean? = null,
    @Schema(description = "Angir gjelderPersons relasjon til BM/BP")
    val relasjon: Familierelasjon = if (erBarnAvBmBp == true) Familierelasjon.BARN else Familierelasjon.UKJENT,
    @Schema(description = "Liste over perioder personen bor i samme husstand som BM/BP")
    val borISammeHusstandDtoListe: List<BorISammeHusstandDto>,
) {
    @get:JsonIgnore
    val erBarn get() = relasjon == Familierelasjon.BARN

    init {
        erBarnAvBmBp = if (erBarnAvBmBp != null) erBarnAvBmBp else relasjon == Familierelasjon.BARN
        relatertPersonPersonId = if (relatertPersonPersonId != null) relatertPersonPersonId else gjelderPersonId
    }
}

data class BorISammeHusstandDto(
    @Schema(description = "Personen bor i samme husstand som BM/BP fra- og med måned")
    val periodeFra: LocalDate?,
    @Schema(description = "Personen bor i samme husstand som BM/BP til- og med måned")
    val periodeTil: LocalDate?,
)
