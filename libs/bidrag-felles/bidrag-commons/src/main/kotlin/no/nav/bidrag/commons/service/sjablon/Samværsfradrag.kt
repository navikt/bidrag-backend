package no.nav.bidrag.commons.service.sjablon

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class Samværsfradrag(
    val samvaersklasse: String? = null,
    val alderTom: Int? = null,
    val datoFom: LocalDate? = null,
    val datoTom: LocalDate? = null,
    val antDagerTom: Int? = null,
    val antNetterTom: Int? = null,
    val belopFradrag: BigDecimal? = null,
) : SjablonDto
