package no.nav.bidrag.transport.person

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.person.Diskresjonskode
import no.nav.bidrag.domene.enums.person.Kjønn
import no.nav.bidrag.domene.ident.Personident
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PersonDto(
    @Schema(description = "Identen til personen")
    val ident: Personident,
    @Schema(description = "Navn til personen, format <Etternavn, Fornavn Middelnavn>")
    val navn: String? = null,
    @Schema(description = "Fornavn til personen")
    val fornavn: String? = null,
    @Schema(description = "Mellomnavn til personen")
    val mellomnavn: String? = null,
    @Schema(description = "Etternavn til personen")
    val etternavn: String? = null,
    @Schema(description = "Kjønn til personen")
    val kjønn: Kjønn? = null,
    @Schema(description = "Kjønn til personen", deprecated = true)
    @Deprecated("Skrivefeil", ReplaceWith("kjønn"))
    val kjoenn: Kjønn? = kjønn,
    @Schema(description = "Dødsdato til personen")
    val dødsdato: LocalDate? = null,
    @Schema(description = "Dødsdato til personen", deprecated = true)
    @Deprecated("Skrivefeil", ReplaceWith("dødsdato"))
    val doedsdato: LocalDate? = dødsdato,
    @Schema(description = "Fødselsdato til personen")
    val fødselsdato: LocalDate? = null,
    @Schema(description = "Fødselsdato til personen", deprecated = true)
    @Deprecated("Skrivefeil", ReplaceWith("fødselsdato"))
    val foedselsdato: LocalDate? = fødselsdato,
    @Schema(description = "Diskresjonskode (personvern)")
    val diskresjonskode: Diskresjonskode? = null,
    @Schema(description = "Aktør id til personen")
    val aktørId: String? = null,
    @Schema(description = "Aktør id til personen", deprecated = true)
    @Deprecated("Skrivefeil", ReplaceWith("aktørId"))
    val aktoerId: String? = aktørId,
    @Schema(description = "Kortnavn på personen, navn som benyttes ved maskinelle utskrifter (maks 40 tegn)")
    val kortnavn: String? = null,
    @Schema(description = "Kortnavn på personen, navn som benyttes ved maskinelle utskrifter (maks 40 tegn)", deprecated = true)
    @Deprecated("Skrivefeil", ReplaceWith("kortnavn"))
    val kortNavn: String? = kortnavn,
    @Schema(description = "Navn som benyttes for visning av personavn i skjermbilder og dokumenter")
    val visningsnavn: String = navn ?: kortnavn ?: "",
)
