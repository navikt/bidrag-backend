package no.nav.bidrag.regnskap.dto.patch

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.ident.Personident
import no.nav.bidrag.domene.sak.Saksnummer

data class PatchMottakerRequest(

    @field:Schema(
        description = "Saksnummer som skal korrigeres.",
        example = "123456",
    )
    val saksnummer: Saksnummer,

    @field:Schema(
        description = "Ident til kravhaver.",
    )
    val kravhaver: Personident,

    @field:Schema(
        description = "Ident til ny mottaker.",
    )
    val mottaker: Personident,
)
