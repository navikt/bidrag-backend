package no.nav.bidrag.grunnlag.consumer.valutakurs.domene.norgesbank

import no.nav.bidrag.grunnlag.consumer.valutakurs.exception.NorgesBankValutakursMappingException

enum class Frekvens(
    val verdi: String,
) {
    VIRKEDAG("B"),
    MÅNEDLIG("M"),
    ÅRLIG("A"),
    ;

    companion object {
        fun fraVerdi(verdi: String): Frekvens = entries.firstOrNull { it.verdi == verdi }
            ?: throw NorgesBankValutakursMappingException.UgyldigData("Ukjent frekvens.")
    }
}
