package no.nav.bidrag.domene.enums.beregning

import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true, name = "Samværsklasse")
enum class Samværsklasse(
    val bisysKode: String,
) {
    @JsonAlias("INGEN_SAMVÆR")
    SAMVÆRSKLASSE_0("00"),
    SAMVÆRSKLASSE_1("01"),
    SAMVÆRSKLASSE_2("02"),
    SAMVÆRSKLASSE_3("03"),
    SAMVÆRSKLASSE_4("04"),
    DELT_BOSTED("DN"),
    ;

    companion object {
        fun fromBisysKode(bisysKode: String) = entries.firstOrNull { it.bisysKode == bisysKode }
    }
}
