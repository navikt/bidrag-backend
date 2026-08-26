package no.nav.bidrag.transport.regnskap.krav

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "SkattKravRequest", description = "Et krav består av en liste med konteringer.")
data class Krav(
    val konteringer: List<Kravkontering>,
)
