package no.nav.bidrag.tilgangskontroll.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.tilgangskontroll.tjeneste.SporingsdataService
import no.nav.bidrag.transport.tilgang.Sporingsdata
import no.nav.bidrag.transport.tilgang.SporingsdataPersonRequest
import no.nav.bidrag.transport.tilgang.SporingsdataSakRequest
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@RequestMapping("/v2/api/sporingsdata")
@Tag(name = "Sporingsdata", description = "Endepunkter for å hente sporingsdata til auditlogging ved oppslag på person og sak")
class SporingsdataControllerV2(
    private val sporingsdataService: SporingsdataService,
) {
    @PostMapping("/sak")
    @Operation(
        summary = "Hent sporingsdata for sak",
        description =
        "Henter sporingsdata for en bidragssak som brukes til auditlogging. " +
            "Returnerer personident for primærperson i saken, tilgangsstatus og relevante ekstrafelter (f.eks. saksnummer).",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Sporingsdata for saken, inkludert tilgangsstatus og personident for auditlogging",
                content = [Content(schema = Schema(implementation = Sporingsdata::class))],
            ),
            ApiResponse(responseCode = "401", description = "Manglende eller ugyldig token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Intern serverfeil ved henting av sporingsdata", content = [Content()]),
        ],
    )
    fun hentSakSporingsdata(
        @RequestBody sporingsdataSakRequest: SporingsdataSakRequest,
    ): ResponseEntity<Sporingsdata> = ResponseEntity.ok(sporingsdataService.hentSakSporingsdata(sporingsdataSakRequest.saksnummer.verdi))

    @PostMapping("/person")
    @Operation(
        summary = "Hent sporingsdata for person",
        description =
        "Henter sporingsdata for en person som brukes til auditlogging. " +
            "Returnerer personident, tilgangsstatus og relevante ekstrafelter.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Sporingsdata for personen, inkludert tilgangsstatus for auditlogging",
                content = [Content(schema = Schema(implementation = Sporingsdata::class))],
            ),
            ApiResponse(responseCode = "401", description = "Manglende eller ugyldig token", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Intern serverfeil ved henting av sporingsdata", content = [Content()]),
        ],
    )
    fun hentPersonSporingsdata(
        @RequestBody sporingsdataPersonRequest: SporingsdataPersonRequest,
    ): ResponseEntity<Sporingsdata> = ResponseEntity.ok(sporingsdataService.hentPersonSporingsdata(sporingsdataPersonRequest.personIdent.verdi))
}
