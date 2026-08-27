package no.nav.bidrag.transport.reskontro.response.transaksjoner

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "Transaksjoner",
)
data class TransaksjonerDto(
    @field:Schema(
        description = "Liste over alle transaksjoenen på bidragssak",
    )
    val transaksjoner: List<TransaksjonDto>,
)
