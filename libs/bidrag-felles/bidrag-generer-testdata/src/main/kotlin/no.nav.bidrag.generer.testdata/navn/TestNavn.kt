package no.nav.bidrag.generer.testdata.navn

import no.nav.bidrag.generer.testdata.PeriodisertTestData
import java.time.LocalDate

@Suppress("unused")
data class TestNavn(
    val fornavn: String? = null,
    val etternavn: String? = null,
    override val periodeFra: LocalDate? = null,
    override val periodeTil: LocalDate? = null,
) : PeriodisertTestData {
    val sammensatt: String
        get() = "$etternavn, $fornavn"
}
