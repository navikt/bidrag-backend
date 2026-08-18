package no.nav.bidrag.person.query

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.bidrag.transport.person.Identgruppe

data class HentIdenterQuery(private val ident: String, private val identgrupper: Set<Identgruppe>, private val historikk: Boolean) : GraphQuery() {
    private val query = graphqlQuery("hent-identer")

    override fun getQuery(): String = query

    override fun getVariables(): Map<String, Any> = mapOf("ident" to ident, "grupper" to identgrupper, "historikk" to historikk)
}

data class HentIdenterResponse(
    @JsonProperty("hentIdenter")
    val hentIdenter: Identer,
)

data class Identer(
    @JsonProperty("identer")
    val identer: List<Ident> = emptyList(),
) {
    private fun getIdentByGruppe(gruppe: Identgruppe): String? {
        val ident = identer.firstOrNull { ident -> ident.gruppe == gruppe.name }
        return ident?.ident
    }

    fun getIdent() = getIdentByGruppe(Identgruppe.FOLKEREGISTERIDENT) ?: getIdentByGruppe(Identgruppe.NPID) ?: ""

    fun getAktorId() = getIdentByGruppe(Identgruppe.AKTORID)
}

data class Ident(val ident: String, val historisk: Boolean, val gruppe: String)
