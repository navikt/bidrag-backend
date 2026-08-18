package no.nav.bidrag.aktoerregister.dto.enumer

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Angir hvilken type identitetsnummer som benyttes for å identifisere aktøren.")
enum class Identtype {
    @Schema(description = "PERSONNUMMER angir at identitetsnummeret som benyttes er enten et FNR eller et DNR.")
    PERSONNUMMER,

    @Schema(description = "AKTOERNUMMER angir at identitetsnummeret er en TSS-ident. Også kjent som samhandler-id.")
    AKTOERNUMMER,

    ;

    companion object {
        fun valueOf(type: String?): Identtype? = entries.find { it.name == type }
    }
}
