package no.nav.bidrag.transport.sak

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.sak.Saksnummer

@Schema(
    description =
        "Metadata for pip tjeneste " +
            "(fnr for involverte roller)",
)
data class BidragssakPipDto(
    @Schema(description = "Saksnummeret til bidragssaken")
    val saksnummer: Saksnummer,
    @Schema(description = "Om saken er avsluttet")
    val avsluttet: Boolean = false,
    @Schema(description = "Fødselsnummer til personer innvolvert i bidragssaken")
    val roller: List<String> = emptyList(),
)
