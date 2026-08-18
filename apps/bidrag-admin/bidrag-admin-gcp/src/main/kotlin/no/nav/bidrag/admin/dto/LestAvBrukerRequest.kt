package no.nav.bidrag.admin.dto

data class LestAvBrukerRequest(
    val lesetidVarighetMs: Long = 0,
    val enhet: String?,
)
