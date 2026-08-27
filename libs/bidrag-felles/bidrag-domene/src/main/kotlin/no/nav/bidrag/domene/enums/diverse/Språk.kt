package no.nav.bidrag.domene.enums.diverse

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true, name = "Språk")
enum class Språk(
    val visningsnavn: String,
) {
    NB("Norsk bokmål"),
    NN("Norsk nynorsk"),
    AR("Arabisk"),
    DA("Dansk"),
    DE("Tysk"),
    EN("Engelsk"),
    EL("Gresk"),
    ET("Estisk"),
    ES("Spansk"),
    FI("Finsk"),
    FR("Franks"),
    IS("Islandsk"),
    IT("Italiensk"),
    JA("Japansk"),
    HR("Kroatisk"),
    LV("Latvisk"),
    LT("Litauisk"),
    NL("Nederlandsk"),
    PL("Polsk"),
    PT("Portugisisk"),
    RO("Rumensk"),
    RU("Russisk"),
    SR("Serbisk"),
    SL("Slovensk"),
    SK("Slovakisk"),
    SV("Svensk"),
    TH("Thai"),
    TR("Tyrkisk"),
    UK("Ukrainsk"),
    HU("Ungarsk"),
    VI("Vietnamesisk"),
    ;

    companion object {
        fun fraVisningsnavn(visningsnavn: String): Språk? = entries.firstOrNull { it.visningsnavn == visningsnavn }
    }
}
