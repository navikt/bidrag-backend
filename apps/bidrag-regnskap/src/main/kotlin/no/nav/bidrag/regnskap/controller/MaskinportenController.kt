package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.commons.security.maskinporten.MaskinportenClient
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Tag(name = "Maskinporten", description = "Henting av Maskinporten-tokens for tjeneste-til-tjeneste-kommunikasjon.")
class MaskinportenController(
    val maskinportenClient: MaskinportenClient,
) {

    @Operation(
        summary = "Hent Maskinporten-token med gitte scopes",
        description = "Henter et gyldig Maskinporten-token for de oppgitte scopes. Scopes må finnes i klientkonfigurasjonen.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Gyldig Maskinporten-token returnert.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(type = "string"))],
            ),
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel.", content = [Content()]),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    @GetMapping(value = ["/token"])
    fun hentToken(
        @Parameter(
            description = "Kommaseparert liste over scopes som tokenet skal inneholde.",
            required = true,
            example = "nav:bidrag/v2/regnskap",
        )
        @RequestParam(required = true)
        scopes: String,
    ): ResponseEntity<String> = ResponseEntity.ok(maskinportenClient.hentMaskinportenToken(scopes).parsedString)
}
