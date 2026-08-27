package no.nav.bidrag.generer.testdata.adresse

import no.nav.bidrag.domene.enums.adresse.Adressetype
import no.nav.bidrag.generer.testdata.PeriodisertTestData
import java.time.LocalDate

@Suppress("unused")
data class Adressetilknytning(
    val adresse: TestAdresse? = null,
    val type: Adressetype? = null,
    override val periodeFra: LocalDate? = null,
    override val periodeTil: LocalDate? = null,
) : PeriodisertTestData
