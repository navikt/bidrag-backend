package no.nav.bidrag.person.query

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.person.BidragPerson.Companion.SECURE_LOGGER
import no.nav.bidrag.transport.person.NavnFødselDødDto

data class NavnFødselsdatoDødsfallQuery(val personId: Personident) : GraphQuery() {
    private val query = graphqlQuery("NavnFødselsdatoDødsfallQuery")

    override fun getQuery(): String = query

    override fun getVariables(): HashMap<String, Any> = hashMapOf("personId" to personId)
}

data class NavnFødselsdatoDødsfallResponse(
    @JsonProperty("hentPerson")
    var hentNavnFødselsdatoDødsfall: HentNavnFødselsdatoDødsfall,
) {
    fun mapToNavnFødselsdatoDødsfallDto(personident: Personident): NavnFødselDødDto {
        val navn = hentNavnFødselsdatoDødsfall.navn.firstOrNull()?.visningsnavn ?: ""

        val fødselsdato = hentNavnFødselsdatoDødsfall.foedselsdato.firstOrNull()?.foedselsdato
        val fødselsaar =
            hentNavnFødselsdatoDødsfall.foedselsdato.firstOrNull()?.foedselsaar
        if (fødselsaar == null) {
            SECURE_LOGGER.warn("Fødselsår mangler for person: ${personident.verdi} fødselsdato: $fødselsdato.")
        }
        val dødsdato = hentNavnFødselsdatoDødsfall.doedsfall.firstOrNull()?.doedsdato
        return NavnFødselDødDto(navn, fødselsdato, fødselsaar, dødsdato)
    }
}

data class HentNavnFødselsdatoDødsfall(
    val foedselsdato: List<Fødselsdato> = emptyList(),
    val doedsfall: List<Dødsfall> = emptyList(),
    val navn: List<Navn> = emptyList(),
)
