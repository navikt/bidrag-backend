package no.nav.bidrag.dokument.arkiv.query

data class JournalpostEksternReferanseIdQuery(val eksternReferanseId: String) : GraphQuery() {

    override fun getQuery(): String = this.graphqlQuery("journalpost")

    override fun getVariables(): HashMap<String, Any> = hashMapOf("eksternReferanseId" to eksternReferanseId)
}
