package no.nav.bidrag.sak.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.bidrag.domene.organisasjon.Enhetsnummer
import no.nav.bidrag.domene.sak.Saksnummer

@Schema(description = "Data som trengs for å opprette et saksnummer for en bidragssak")
@Deprecated("Bruk endepunkt som oppretter sak med tilhørende verdier.")
data class NySakCommandDto(
    @Schema(description = "Sakens eierfogd (enhetsnummeret som får tilgang til saken")
    val eierfogd: Enhetsnummer,
)

@Schema(description = "Response ved opprettelse av sak")
@Deprecated("Bruk endepunkt som oppretter sak med tilhørende verdier.")
data class NySakResponseDto(
    @Schema(description = "Saksnummer som ble tildelt ")
    val saksnummer: Saksnummer,
)
