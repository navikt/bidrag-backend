package no.nav.bidrag.sak.dto

data class SamhandlerSakDto(
    val antallSaker: Int,
    val saksnummere: List<String>,
)

data class SamhandlarSakRequestDto(
    val samhandlerId: String,
)
