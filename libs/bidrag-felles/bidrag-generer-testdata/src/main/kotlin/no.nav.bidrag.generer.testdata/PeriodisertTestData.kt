package no.nav.bidrag.generer.testdata

import java.time.LocalDate

@Suppress("unused")
interface PeriodisertTestData {
    val periodeFra: LocalDate?

    val periodeTil: LocalDate?

    fun overlapperMed(dag: LocalDate?): Boolean =
        !this.periodeFra!!.isAfter(dag) &&
            (this.periodeTil == null || this.periodeTil!!.isAfter(dag))

    fun overlapperMedIdag(): Boolean = overlapperMed(LocalDate.now())
}
