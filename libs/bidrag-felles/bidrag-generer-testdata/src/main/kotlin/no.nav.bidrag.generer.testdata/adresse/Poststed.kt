package no.nav.bidrag.generer.testdata.adresse

@Suppress("unused")
data class Poststed(
    val postnummer: String,
    val poststed: String,
) {
    override fun toString(): String = "$postnummer, $poststed"
}
