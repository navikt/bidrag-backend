package no.nav.bidrag.tilgangskontroll.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.domene.enums.behandling.Behandlingstema
import no.nav.bidrag.tilgangskontroll.tjeneste.TilgangskontrollService
import no.nav.bidrag.transport.tilgang.TilgangskontrollResponse
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@Protected
@RequestMapping("/v2/api/behandlingstilgang")
@Tag(
    name = "Behandlingstilgang",
    description = "Endepunkter for å kontrollere saksbehandlers lese- og behandlingstilgang til behandlingstemaer",
)
class BehandlingstilgangController(
    private val tilgangskontrollService: TilgangskontrollService,
) {
    @PostMapping("/les")
    @Operation(
        summary = "Sjekk lesetilgang for behandlingstema",
        description =
        "Kontrollerer om innlogget saksbehandler har lesetilgang til de oppgitte behandlingstemaene. " +
            "Validerer brukerens AD-gruppemedlemskap mot tilgangskrav for hvert tema.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Tilgangssjekk utført. Se 'harTilgang'-feltet for resultat. Ved avslag inneholder 'detaljer' begrunnelse.",
                content = [Content(schema = Schema(implementation = TilgangskontrollResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Manglende eller ugyldig token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Intern serverfeil ved tilgangssjekk", content = [Content()]),
        ],
    )
    fun sjekkLesetilgangBehandlingstema(
        @RequestBody behandlingstemaRequest: BehandlingstemaRequest,
    ): ResponseEntity<TilgangskontrollResponse> = ResponseEntity.ok(tilgangskontrollService.sjekkLesetilgangTilBehandlingstema(behandlingstemaRequest.behandlingstemaer))

    @PostMapping("/skriv")
    @Operation(
        summary = "Sjekk skrivetilgang for behandlingstema",
        description =
        "Kontrollerer om innlogget saksbehandler har tilgang til å behandle (opprette/endre vedtak) " +
            "for de oppgitte behandlingstemaene. Krever høyere tilgangsnivå enn lesetilgang.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Tilgangssjekk utført. Se 'harTilgang'-feltet for resultat. Ved avslag inneholder 'detaljer' begrunnelse.",
                content = [Content(schema = Schema(implementation = TilgangskontrollResponse::class))],
            ),
            ApiResponse(responseCode = "401", description = "Manglende eller ugyldig token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Intern serverfeil ved tilgangssjekk", content = [Content()]),
        ],
    )
    fun sjekkbehandlingstilgangBehandlingstema(
        @RequestBody behandlingstemaRequest: BehandlingstemaRequest,
    ): ResponseEntity<TilgangskontrollResponse> = ResponseEntity.ok(tilgangskontrollService.sjekkSkrivetilgangTilBehandlingstema(behandlingstemaRequest.behandlingstemaer))
}

data class BehandlingstemaRequest(
    val behandlingstemaer: List<Behandlingstema>,
)
