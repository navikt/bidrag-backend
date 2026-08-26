@file:Suppress("unused")

package no.nav.bidrag.domene.enums.person

enum class Familierelasjon {
    BARN,
    FAR,
    MEDMOR,
    MOR,
    INGEN,
    FORELDER,
    EKTEFELLE,
    MOTPART_TIL_FELLES_BARN,

    // Hvis relasjon ikke er BARN og det finnes ingen annen informasjon om relasjonen
    UKJENT,
}
