package no.nav.bidrag.admin.produksjonsoppfølging.jira.dto

data class CodeField(
    val id: String? = null,
    val key: String? = null,
    val value: String? = null,
) {
    companion object {
        fun withKey(key: String) = CodeField(key = key)

        fun withId(id: String) = CodeField(id = id)
    }
}
