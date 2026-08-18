package no.nav.bidrag.person.dto

data class KontoregisterResponse(
    val kontohaver: String,
    val kontonummer: String,
    val utenlandskKontoInfo: UtenlandskKontoInfo?,
    val gyldigFom: String,
    val opprettetAv: String,
    val kilde: String?,
)

data class UtenlandskKontoInfo(
    val banknavn: String?,
    val bankkode: String?,
    val bankLandkode: String,
    val valutakode: String,
    val swiftBicKode: String?,
    val bankadresse1: String?,
    val bankadresse2: String?,
    val bankadresse3: String?,
)
