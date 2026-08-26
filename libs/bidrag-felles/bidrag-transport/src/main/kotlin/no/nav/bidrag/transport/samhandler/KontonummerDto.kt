package no.nav.bidrag.transport.samhandler

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.enums.samhandler.Valutakode
import no.nav.bidrag.domene.land.Landkode3

@Schema(
    description =
        "Representerer kontonummer, primært samhandlere spesielt for utenlandske kontonummer. For norske kontonummer " +
            "er det kun norskKontonummer som er utfyllt, ellers benyttes de andre feltene for utlandske kontonummer.",
)
data class KontonummerDto(
    @param:Schema(description = "Norsk kontonummer, 11 siffer.")
    val norskKontonummer: String? = null,
    @param:Schema(description = "IBAN angir kontonummeret på et internasjonalt format.")
    val iban: String? = null,
    @param:Schema(description = "SWIFT angir banken på et internasjonalt format.")
    val swift: String? = null,
    @param:Schema(description = "Bankens navn.")
    val banknavn: String? = null,
    @param:Schema(description = "Bankens landkode. ISO 3166-1 alfa-3.")
    val landkodeBank: Landkode3? = null,
    @param:Schema(description = "BankCode. Format varierer.")
    val bankCode: String? = null,
    @param:Schema(description = "Kontoens valuta.")
    val valutakode: Valutakode? = null,
)
