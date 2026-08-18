package no.nav.bidrag.person.query

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.bidrag.person.BidragPerson
import no.nav.bidrag.transport.person.Husstandsmedlem

data class SoekPersonGraphsqlQuery(val query: String, val variables: SokPersonVariabler)

data class SokPersonVariabler(val paging: Paging, val criteria: List<Criteria>)

data class Paging(val pageNumber: Int, val resultsPerPage: Int)

data class Criteria(val fieldName: String, val searchRule: Map<String, String>)

class SearchRule(key: String, value: String) : HashMap<String, String>() {
    init {
        put(key, value)
    }
}

data class HusstandsmedlemmerQuery(val paging: Paging, val criteria: List<Criteria>) : GraphQuery() {
    private val query = graphqlQuery("HusstandsmedlemmerQuery")

    override fun getQuery(): String = query

    override fun getVariables(): HashMap<String, Any> = hashMapOf("paging" to paging, "criteria" to criteria)
}

data class HusstandsmedlemmerResponseData(var sokPerson: HusstandsmedlemmerResponse)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class HusstandsmedlemmerResponse(
    @JsonProperty("totalHits")
    var totalHits: Long,
    @JsonProperty("hits")
    var hentHusstandsmedlemmerListe: List<HentHusstandsmedlemmer>,
) {
    fun mapToHusstandsmedlemmer(bostedsadresse: Bostedsadresse): List<Husstandsmedlem> {
        if (hentHusstandsmedlemmerListe.isNotEmpty()) {
            val husstandsmedlemListe = mutableListOf<Husstandsmedlem>()
            hentHusstandsmedlemmerListe.forEach { husstandsmedlem ->
                val navn =
                    listOfNotNull(
                        husstandsmedlem.person.navn.firstOrNull()?.fornavn,
                        husstandsmedlem.person.navn.firstOrNull()?.mellomnavn,
                        husstandsmedlem.person.navn.firstOrNull()?.etternavn,
                    ).joinToString(" ")
                if (husstandsmedlem.person.folkeregisteridentifikator.isEmpty()) {
                    BidragPerson.SECURE_LOGGER.debug("Folkeregisteridentifikator mangler for person: {}", husstandsmedlem)
                } else {
                    husstandsmedlemListe.add(
                        Husstandsmedlem(
                            // Datoer settes til null. Verdier settes i PersonService.
                            gyldigFraOgMed = null,
                            gyldigTilOgMed = null,
                            personId = husstandsmedlem.person.folkeregisteridentifikator.first().identifikasjonsnummer,
                            navn = navn,
                            fødselsdato = husstandsmedlem.person.foedselsdato.firstOrNull()?.foedselsdato,
                            dødsdato = husstandsmedlem.person.doedsfall.firstOrNull()?.doedsdato,
                        ),
                    )
                }
            }
            return husstandsmedlemListe
        } else {
            return listOf()
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class HentHusstandsmedlemmer(val person: PersonResponse.HentPersonNavnFødselsdatoDødsfallResponse)
