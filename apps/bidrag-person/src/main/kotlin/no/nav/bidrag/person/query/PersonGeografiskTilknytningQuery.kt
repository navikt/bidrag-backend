package no.nav.bidrag.person.query

import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.transport.person.GeografiskTilknytningDto

data class PersonGeografiskTilknytningQuery(val personIdent: Personident) : GraphQuery() {
    private val query = """
       query GeografiskTilknytningQuery(${"$"}personId: ID!) 
        {
           hentIdenter(ident: ${"$"}personId, historikk:false) {
                identer {
                    ident,
                    historisk,
                    gruppe
                }
             }
            hentPerson( ident: ${"$"}personId) {
               adressebeskyttelse {
                   gradering
               }
               oppholdsadresse {
                   oppholdAnnetSted
               }
            }
            hentGeografiskTilknytning(ident: ${"$"}personId) {
                    gtType
                    gtBydel
                    gtKommune
                    gtLand
            }
      }
        """

    override fun getQuery(): String = query

    override fun getVariables(): HashMap<String, Any> = hashMapOf("personId" to personIdent)
}

data class GeografiskTilknytningResponse(
    var hentGeografiskTilknytning: HentGeografiskTilknytning?,
    var hentPerson: HentPersonDiskresjon,
    var hentIdenter: PersonResponse.HentIdenter,
) {
    fun mapToGeografiskTilknytningDto(): GeografiskTilknytningDto = GeografiskTilknytningDto(
        hentIdenter.getIdent(),
        hentIdenter.getAktorId(),
        geografiskTilknytning = hentGeografiskTilknytning?.hentGeografiskTilknytning(),
        diskresjonskode = getDiskresjonsKode(),
        erUtland = hentGeografiskTilknytning?.gtType == "UTLAND",
    )

    private fun getDiskresjonsKode() = toDisreksjonsKode(hentPerson.oppholdsadresse, hentPerson.adressebeskyttelse)
}

data class HentPersonDiskresjon(
    val adressebeskyttelse: List<Adressebeskyttelse> = emptyList(),
    val oppholdsadresse: List<OppholdsadresseCommon> = emptyList(),
)

data class HentGeografiskTilknytning(val gtKommune: String?, val gtBydel: String?, val gtType: String?, val gtLand: String?) {
    fun hentGeografiskTilknytning() = (gtKommune ?: gtBydel ?: gtLand)
}
