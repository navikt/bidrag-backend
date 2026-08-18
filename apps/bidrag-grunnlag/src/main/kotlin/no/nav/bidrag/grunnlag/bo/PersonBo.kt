package no.nav.bidrag.grunnlag.bo

import java.time.LocalDate

data class PersonBo(
    val personId: String?,
    var navn: String?,
    var fodselsdato: LocalDate?,
    val husstandsmedlemPeriodeFra: LocalDate? = null,
    val husstandsmedlemPeriodeTil: LocalDate? = null,
)
