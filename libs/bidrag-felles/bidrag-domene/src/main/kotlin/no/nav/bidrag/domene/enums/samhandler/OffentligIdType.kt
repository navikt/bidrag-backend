package no.nav.bidrag.domene.enums.samhandler

import io.swagger.v3.oas.annotations.media.Schema

@Schema(enumAsRef = true, name = "Offentlig ID type")
enum class OffentligIdType(
    beskrivelse: String,
) {
    ORG("Organisasjonsnummer"),
    FNR("Norsk fødselsnummer"),
    UTOR("Utenlandsk organisasjonsnummer"),
    UTPE("Utenlandsk personnummer"),
    DNR("D-nummer"),
    HPR("Helsepersonellregisteret"),
}
