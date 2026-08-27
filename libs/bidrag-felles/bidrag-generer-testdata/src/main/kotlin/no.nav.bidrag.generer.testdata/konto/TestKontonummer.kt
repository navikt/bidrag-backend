package no.nav.bidrag.generer.testdata.konto

import no.nav.bidrag.domene.enums.diverse.LandkoderIso3
import no.nav.bidrag.domene.enums.samhandler.Valutakode

@Suppress("unused")
data class TestKontonummer(
    val norskKontonummer: String? = null,
    val iban: String? = null,
    val swift: String? = null,
    val bankNavn: String? = null,
    val landkodeBank: LandkoderIso3? = null,
    val bankCode: String? = null,
    val valutakode: Valutakode? = null,
)
