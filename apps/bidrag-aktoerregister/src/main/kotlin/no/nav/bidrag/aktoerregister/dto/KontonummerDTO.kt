package no.nav.bidrag.aktoerregister.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Representerer kontonummer for en bidragsaktør. For norske kontonummer er det kun norskKontornr som er utfyllt, ellers benyttes de andre feltene for utlandske kontonummer.",
)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class KontonummerDTO(
    @param:Schema(description = "Norsk kontonummer, 11 siffer.")
    val norskKontonr: String? = null,

    @param:Schema(description = "IBAN angir kontonummeret på et internasjonalt format.")
    val iban: String? = null,

    @param:Schema(description = "SWIFT angir banken på et internasjonalt format.")
    val swift: String? = null,

    @param:Schema(description = "Bankens navn.")
    val bankNavn: String? = null,

    @param:Schema(description = "Bankens landkode. TODO: Bestemme representasjon av land. 3-sifret land-kode?")
    val bankLandkode: String? = null,

    @param:Schema(description = "BankCode. Format varierer.")
    val bankCode: String? = null,

    @param:Schema(description = "Kontoens valuta.")
    val valutaKode: String? = null,
)
