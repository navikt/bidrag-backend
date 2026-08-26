package no.nav.bidrag.commons.service.sjablon

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class Barnetilsyn(
    @JsonProperty("typeStonad")
    val typeStønad: String? = null,
    val typeTilsyn: String? = null,
    val datoFom: LocalDate? = null,
    val datoTom: LocalDate? = null,
    @JsonAlias("belopBarneTilsyn", "belopBarnetilsyn")
    val beløpBarnetilsyn: BigDecimal? = null,
) : SjablonDto
