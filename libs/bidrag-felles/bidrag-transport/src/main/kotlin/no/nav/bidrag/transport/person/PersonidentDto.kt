package no.nav.bidrag.transport.person

data class PersonidentDto(
    val ident: String,
    val historisk: Boolean,
    val gruppe: Identgruppe,
)

data class HentePersonidenterRequest(
    val ident: String,
    val grupper: Set<Identgruppe> = Identgruppe.values().toSet(),
    val inkludereHistoriske: Boolean = true,
)

enum class Identgruppe { AKTORID, FOLKEREGISTERIDENT, NPID }
