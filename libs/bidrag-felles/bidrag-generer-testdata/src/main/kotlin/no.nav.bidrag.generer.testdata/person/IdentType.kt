package no.nav.bidrag.generer.testdata.person

import no.nav.bidrag.generer.testdata.RandomTestData
import java.time.LocalDate

@Suppress("unused")
interface IdentType {
    fun generer(
        fodtDato: LocalDate,
        kjønn: Kjønn?,
    ): String?

    fun generer(): String? =
        generer(
            RandomTestData.random().dateBetween(LocalDate.of(1900, 1, 1), LocalDate.now()),
            Kjønn.entries.random(),
        )
}
