package no.nav.bidrag.admin.produksjonsoppfølging.jira.dto

data class SearchResponse(
    var startAt: Int? = null,
    var maxResults: Int? = null,
    var total: Int? = null,
    var issues: List<Issue> = emptyList(),
)
