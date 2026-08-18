package no.nav.bidrag.person.query

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.bidrag.domene.ident.Personident
import java.time.LocalDate

class PersonFødselsdatoerQuery(private val identer: Set<Personident>) : GraphQuery() {
    override fun getQuery() = graphqlQuery("fødselsdatoer")

    override fun getVariables() = mapOf("identer" to identer)
}

data class PersonFødselBolkResponse(override val personBolk: List<PersonFødselForekomst>) : PersonBolkResponse<PersonFødsel>

data class PersonFødselForekomst(override val ident: Personident, override val code: String, override val person: PersonFødsel?) : PersonForekomst<PersonFødsel>

data class PersonFødsel(
    @param:JsonProperty("foedselsdato")
    val foedselsdato: List<Fødsel> = emptyList(),
) {
    fun tilFødselsdato(): LocalDate? = foedselsdato.firstOrNull()?.foedselsdato
}

data class Fødsel(
    @param:JsonProperty("foedselsdato")
    val foedselsdato: LocalDate?,
)
