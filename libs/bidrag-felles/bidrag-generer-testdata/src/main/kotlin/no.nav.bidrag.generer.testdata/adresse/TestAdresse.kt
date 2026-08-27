package no.nav.bidrag.generer.testdata.adresse

import no.nav.bidrag.domene.enums.diverse.LandkoderIso3

@Suppress("unused")
data class TestAdresse(
    val format: Adresseformat? = null,
    val adresselinje1: String? = null,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val postnummer: String? = null,
    val poststed: String? = null,
    val bolignummer: String? = null,
    val land: LandkoderIso3? = null,
)
