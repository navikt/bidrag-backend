package no.nav.bidrag.domene.enums.rolle

enum class Rolle(
    val kortnavn: String,
) {
    BIDRAGSPLIKTIG("BP"),
    BIDRAGSMOTTAKER("BM"),
    SØKNADSBARN("SB"),
}
