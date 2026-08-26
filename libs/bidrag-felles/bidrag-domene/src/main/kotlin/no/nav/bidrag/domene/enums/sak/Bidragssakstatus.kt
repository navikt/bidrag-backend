@file:Suppress("unused")

package no.nav.bidrag.domene.enums.sak

enum class Bidragssakstatus(
    val beskrivelse: String,
) {
    AK("Aktiv"),
    IN("Inaktiv"),
    NY("Journalsak"),
    SA("Sanert"),
    SO("Åpensøknad"),
}
