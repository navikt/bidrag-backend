package no.nav.bidrag.domene.enums.samhandler

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true, name = "Kreditortype")
enum class Kreditortype(
    val visningsnavn: String,
) {
    UTENRIKSSTASJON("Utenriksstasjon"),
    ADVOKAT("Advokat"),
    ARBEIDSGIVER("Arbeidsgiver"),
    REELL_MOTTAKER("Reell mottaker"),
    UTENLANDSK_PERSONNR("Utenlandsk person"),
    UTENLANDSK_FOGD("Utenlandsk fogd"),
    SOSIALKONTOR("Sosialkontor"),
    BARNEVERNSINSTITUSJON("Barnevernsinstitusjon"),
    ;

    companion object {
        fun fraVisningsnavn(visningsnavn: String): Kreditortype? = entries.firstOrNull { it.visningsnavn == visningsnavn }
    }
}
