package no.nav.bidrag.person.query

import no.nav.bidrag.domene.ident.Personident

data class PersonBostedsadresseQuery(val personIdent: Personident) : GraphQuery() {
    private val query = """
       query PersonBostedsadresseQuery(${"$"}personId: ID!) 
        {
            hentPerson(ident:${"$"}personId) {
               bostedsadresse(historikk: true) {
                   angittFlyttedato
                   gyldigFraOgMed
                   gyldigTilOgMed
                   coAdressenavn
                   metadata {
                       master
                   }
                   vegadresse {
                       postnummer
                       adressenavn
                       husnummer
                       husbokstav
                       bruksenhetsnummer
                       tilleggsnavn
                       kommunenummer
                       bydelsnummer
                       matrikkelId
                   }
               }
        }
      }
        """

    override fun getQuery(): String = query

    override fun getVariables(): HashMap<String, Any> = hashMapOf("personId" to personIdent)
}

data class PersonBostedsadresse(var hentPerson: HentPersonBostedsadresse) {
    private fun hentBostedsAdresse(): Bostedsadresse? = hentPerson.bostedsadresse.sortedWith(
        compareByDescending { it.gyldigFraOgMed ?: it.angittFlyttedato },
    ).getOrNull(0)
}

data class HentPersonBostedsadresseResponse(val hentPerson: HentPersonBostedsadresse = HentPersonBostedsadresse())

data class HentPersonBostedsadresse(val bostedsadresse: List<Bostedsadresse> = emptyList())
