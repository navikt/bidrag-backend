package no.nav.bidrag.domene.enums.beregning

@Deprecated("", replaceWith = ReplaceWith("Resultatkode"))
enum class ResultatkodeSærtilskudd(
    val kode: String,
) {
    BARNET_ER_SELVFORSØRGET("5SF"), // Barnet er selvforsørget
    SÆRTILSKUDD_INNVILGET("VS"), // Resultat av beregning av særtilskudd
    SÆRTILSKUDD_IKKE_FULL_BIDRAGSEVNE("6MB"), // Resultat av beregning av særtilskudd
}
