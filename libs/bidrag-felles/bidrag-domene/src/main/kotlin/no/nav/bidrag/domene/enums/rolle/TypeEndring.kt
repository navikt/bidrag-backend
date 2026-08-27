@file:Suppress("unused")

package no.nav.bidrag.domene.enums.rolle

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class TypeEndring {
    @JsonProperty("Satt til BM manuelt")
    @JsonAlias("SATT_TIL_BM")
    SATT_TIL_BM,

    @JsonProperty("Satt RM manuelt")
    @JsonAlias("SATT_NY_RM")
    SATT_NY_RM,

    @JsonProperty("Endret RM manuelt")
    @JsonAlias("SATT_RM")
    SATT_RM,

    @JsonProperty("RM-endring maskinelt")
    @JsonAlias("ENDRE_RM")
    ENDRE_RM,

    @JsonProperty("FNR-endring maskinelt")
    @JsonAlias("ENDRE_FNR")
    ENDRE_FNR,
}
