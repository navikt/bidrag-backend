package no.nav.bidrag.domene.enums.rolle

import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true)
enum class SøktAvType(
    val kode: String,
) {
    BIDRAGSMOTTAKER("MO"),
    BIDRAGSPLIKTIG("PL"),

    @JsonAlias("BARN_18_AAR")
    BARN_18_ÅR("BB"),
    BM_I_ANNEN_SAK("AS"),
    NAV_BIDRAG("ET"), // TK
    FYLKESNEMDA("FN"),
    NAV_INTERNASJONALT("IN"),
    KOMMUNE("KU"),
    NORSKE_MYNDIGHET("NM"),
    UTENLANDSKE_MYNDIGHET("UM"),
    VERGE("VE"),
    TRYGDEETATEN_INNKREVING("TI"),
    KLAGE_ANKE("FK"), // FTK
    KONVERTERING("KV"), // Kodeverdi som brukes i eldre overførte saker i Bisys. Ikke bruk
    ;

    companion object {
        fun fraKode(kode: String): SøktAvType? = entries.find { it.kode == kode }
    }
}
