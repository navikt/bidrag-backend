@file:Suppress("unused")

package no.nav.bidrag.domene.enums.rolle

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class Rolletype {
    @JsonProperty("BA")
    @JsonAlias("BARN")
    BARN,

    @JsonProperty("BM")
    @JsonAlias("BIDRAGSMOTTAKER")
    BIDRAGSMOTTAKER,

    @JsonProperty("BP")
    @JsonAlias("BIDRAGSPLIKTIG")
    BIDRAGSPLIKTIG,

    @JsonProperty("FR")
    @JsonAlias("FEILREGISTRERT")
    FEILREGISTRERT,

    @JsonProperty("RM")
    @JsonAlias("REELMOTTAKER")
    REELMOTTAKER,
}
