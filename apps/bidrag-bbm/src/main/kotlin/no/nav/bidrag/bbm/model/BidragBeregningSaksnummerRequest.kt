package no.nav.bidrag.bbm.model

import com.fasterxml.jackson.annotation.JsonValue
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class BidragBeregningSaksnummerRequest(
    @field:Valid val saksnummerListe: List<Saksnummer>,
) {
    data class Saksnummer(
        @field:NotBlank(message = "Saksnummer kan ikke være blank")
        @field:Size(max = 7, min = 7, message = "Saksnummer skal ha sju tegn")
        @get:JsonValue val verdi: String,
    )
}
