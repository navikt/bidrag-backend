@file:Suppress("unused")

package no.nav.bidrag.domene.ident

import no.nav.bidrag.domene.felles.Verdiobjekt

class Ident(
    override val verdi: String,
) : Verdiobjekt<String>() {
    override fun gyldig(): Boolean =
        Organisasjonsnummer(verdi).gyldig() ||
            Personident(verdi).gyldig() ||
            SamhandlerId(verdi).gyldig()

    fun erOrganisasjonsnummer() = Organisasjonsnummer(verdi).gyldig()

    fun erPersonIdent() = Personident(verdi).gyldig()

    fun erSamhandlerId() = SamhandlerId(verdi).gyldig()

    fun type() =
        when {
            Organisasjonsnummer(verdi).gyldig() -> Identtype.Organisasjonsnummer
            Personident(verdi).gyldig() -> Identtype.PersonIdent
            SamhandlerId(verdi).gyldig() -> Identtype.SamhandlerId
            else -> Identtype.Ukjent
        }

    override fun toString(): String =
        if (erPersonIdent()) {
            verdi.mapIndexed { index, c -> if (index % 2 == 0) c else '*' }.joinToString("")
        } else {
            super.toString()
        }
}

enum class Identtype {
    PersonIdent, // Fødselsnummer, D-nummer, Nav-syntetisk, Skatt-syntetisk
    Organisasjonsnummer,
    SamhandlerId,
    Ukjent,
}
