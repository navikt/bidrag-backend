package no.nav.bidrag.aktoerregister.dto.enumer

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Gradering/Diskresjonskoder:\n" +
        "| API-kode                 | TPS-kode | Også omtalt som |\n" +
        "| FORTROLIG                | SPFO     | Kode 7          |\n" +
        "| STRENGT_FORTROLIG        | SPSF     | Kode 6          |\n" +
        "| STRENGT_FORTROLIG_UTLAND | SPSF     | §19             |",
)
enum class Gradering(val diskresjonskode: Diskresjonskode, val omtaltSom: String) {
    FORTROLIG(Diskresjonskode.SPFO, "Kode 7"),
    STRENGT_FORTROLIG(Diskresjonskode.SPSF, "Kode 6"),
    STRENGT_FORTROLIG_UTLAND(Diskresjonskode.P19, "§19"),
    ;

    companion object {
        fun valueOf(type: String?): Gradering? = entries.find { it.name == type }

        fun from(diskresjonskode: Diskresjonskode?): Gradering? = entries.find { it.diskresjonskode == diskresjonskode }
    }
}
