package no.nav.bidrag.grunnlag.consumer.valutakurs.domene.norgesbank

enum class Frekvens(
    val verdi: String,
) {
    VIRKEDAG("B"),
    MÅNEDLIG("M"),
    ÅRLIG("A"),
    ;

    companion object {
        fun fraVerdi(verdi: String): Frekvens = entries.first { it.verdi == verdi }
    }
}
