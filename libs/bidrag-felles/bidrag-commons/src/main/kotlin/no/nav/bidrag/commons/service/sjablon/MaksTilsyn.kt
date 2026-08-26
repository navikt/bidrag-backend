package no.nav.bidrag.commons.service.sjablon

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class MaksTilsyn(
    @JsonProperty("antBarnTom")
    val antallBarnTom: Int? = null,
    val datoFom: LocalDate? = null,
    val datoTom: LocalDate? = null,
    @JsonProperty("maksBelopTilsyn")
    val maksBeløpTilsyn: BigDecimal? = null,
) : SjablonDto
