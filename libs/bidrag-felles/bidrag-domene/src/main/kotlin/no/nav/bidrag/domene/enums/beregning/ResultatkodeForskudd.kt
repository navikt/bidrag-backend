package no.nav.bidrag.domene.enums.beregning

@Deprecated("", replaceWith = ReplaceWith("Resultatkode"))
enum class ResultatkodeForskudd(
    val legacyKode: String,
) {
    AVSLAG("A"),
    REDUSERT_FORSKUDD_50_PROSENT("50"),
    ORDINÆRT_FORSKUDD_75_PROSENT("75"),
    FORHØYET_FORSKUDD_100_PROSENT("100"),
    FORHØYET_FORSKUDD_11_ÅR_125_PROSENT("125"),
    ;

    companion object {
        fun fraKode(kode: String): ResultatkodeForskudd? =
            try {
                entries.find { it.legacyKode == kode } ?: ResultatkodeForskudd.valueOf(kode)
            } catch (e: Exception) {
                null
            }
    }
}
