package no.nav.bidrag.person.query

import no.nav.bidrag.domene.enums.person.Gradering
import no.nav.bidrag.domene.ident.Personident

class PersonGraderingerQuery(private val identer: Set<Personident>) : GraphQuery() {
    override fun getQuery() = graphqlQuery("graderinger")

    override fun getVariables() = mapOf("identer" to identer)
}

data class PersonGraderingBolkResponse(override val personBolk: List<PersonGraderingForekomst>) : PersonBolkResponse<PersonGradering>

data class PersonGraderingForekomst(override val ident: Personident, override val code: String, override val person: PersonGradering?) : PersonForekomst<PersonGradering>

data class PersonGradering(val adressebeskyttelse: List<Adressebeskyttelse> = emptyList()) {
    fun tilGradering(): Gradering? = adressebeskyttelse.firstOrNull()?.gradering
}
