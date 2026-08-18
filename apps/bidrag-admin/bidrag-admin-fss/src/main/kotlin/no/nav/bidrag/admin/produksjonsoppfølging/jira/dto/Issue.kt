package no.nav.bidrag.admin.produksjonsoppfølging.jira.dto

data class Issue(
    var id: String? = null,
    var self: String? = null,
    var key: String? = null,
    val fields: Fields? = null,
)
