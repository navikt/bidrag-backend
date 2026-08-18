package no.nav.bidrag.person.query

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.ForelderBarnRelasjon

data class ForelderBarnRelasjonQuery(val personId: Personident) : GraphQuery() {
    private val query = """
        query ForelderBarnRelasjonQuery(${"$"}personId: ID!) 
        {
            hentPerson(ident: ${"$"}personId) {
                forelderBarnRelasjon {
                   relatertPersonsIdent,
                   relatertPersonsRolle,
                   minRolleForPerson
                }
            }
        }
        """

    override fun getQuery(): String = query

    override fun getVariables(): HashMap<String, Any> = hashMapOf("personId" to personId)
}

data class ForelderBarnRelasjonResponse(
    @JsonProperty("hentPerson")
    var hentForelderBarnRelasjon: HentForelderBarnRelasjon,
)

data class HentForelderBarnRelasjon(val forelderBarnRelasjon: List<ForelderBarnRelasjon> = emptyList())
