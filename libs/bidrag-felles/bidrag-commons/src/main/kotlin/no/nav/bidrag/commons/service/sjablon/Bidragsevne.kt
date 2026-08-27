package no.nav.bidrag.commons.service.sjablon

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class Bidragsevne(
    val bostatus: String? = null,
    val datoFom: LocalDate? = null,
    val datoTom: LocalDate? = null,
    val belopBoutgift: BigDecimal? = null,
    val belopUnderhold: BigDecimal? = null,
) : SjablonDto
