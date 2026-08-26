package no.nav.bidrag.transport.reskontro.request

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.sak.Saksnummer

data class SaksnummerRequest(
    @field:Schema(
        description = "Saksnummer, refereres til hos skatt som bidragssaksnummer.",
    )
    val saksnummer: Saksnummer,
)
