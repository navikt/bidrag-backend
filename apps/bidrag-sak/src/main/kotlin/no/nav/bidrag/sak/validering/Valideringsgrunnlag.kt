package no.nav.bidrag.sak.validering

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.land.Landkode
import java.time.LocalDate

data class Valideringsgrunnlag(
    val personer: Map<String, Person>,
    val fødselsdatoer: Map<Personident, LocalDate?>,
    val identer: Map<String, Set<String>>,
    val landkoder: Set<Landkode>,
) {
    data class Person(val fødselsdato: LocalDate?)

    fun finnes(ident: Personident): Boolean = ident.verdi in personer

    fun fødselsdato(ident: Personident): LocalDate? = personer[ident.verdi]?.fødselsdato ?: fødselsdatoer[ident]

    fun sammePerson(første: String?, andre: String?): Boolean {
        if (første == null || andre == null) return false
        if (første == andre) return true
        return andre in identerFor(første) || første in identerFor(andre)
    }

    private fun identerFor(ident: String): Set<String> = identer[ident] ?: error("Valideringsgrunnlaget mangler identer for en rolle.")
}
