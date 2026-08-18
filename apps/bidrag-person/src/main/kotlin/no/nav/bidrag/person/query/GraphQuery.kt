package no.nav.bidrag.person.query

import org.apache.commons.lang3.StringUtils

abstract class GraphQuery {
    abstract fun getQuery(): String

    abstract fun getVariables(): Map<String, Any>

    fun graphqlQuery(pdlResource: String): String = this::class.java.getResource("/graphql/$pdlResource.graphql")!!
        .readText()
        .graphqlCompatible()

    private fun String.graphqlCompatible(): String = StringUtils.normalizeSpace(this.replace("\n", ""))
}
