package no.nav.bidrag.transport.person

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NavnFødselDødDto(
    @Schema(
        description =
            "Gir navn, fødselsdato og fødselsår for angitt person. " +
                "Fødselsår finnes for alle i PDL(men ikke opphørte personer), mens noen ikke har utfyllt fødselsdato",
    )
    val navn: String,
    val fødselsdato: LocalDate?,
    val fødselsår: Int?,
    @Schema(description = "Eventuell dødsdato til personen")
    val dødsdato: LocalDate?,
    @Schema(deprecated = true)
    @Deprecated("Skrivefeil", ReplaceWith("fødselsdato"))
    val foedselsdato: LocalDate? = fødselsdato,
    @Schema(deprecated = true)
    @Deprecated("Skrivefeil", ReplaceWith("fødselsår"))
    val foedselsaar: Int? = fødselsår,
    @Schema(description = "Eventuell dødsdato til personen", deprecated = true)
    @Deprecated("Skrivefeil", ReplaceWith("dødsdato"))
    val doedsdato: LocalDate? = dødsdato,
)
