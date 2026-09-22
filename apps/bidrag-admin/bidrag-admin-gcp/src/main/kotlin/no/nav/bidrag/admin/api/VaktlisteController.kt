package no.nav.bidrag.admin.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.admin.service.VaktlisteException
import no.nav.bidrag.admin.service.VaktlisteService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@Tag(name = "Vaktliste", description = "Endepunkter for manuell trigging av vaktrotasjon.")
class VaktlisteController(
    private val vaktlisteService: VaktlisteService,
) {
    @PostMapping("/vaktliste/rotasjon/trigger")
    @Operation(
        summary = "Trigg vaktrotasjon manuelt",
        description = "Finner personen som har vaktet lengst siden i \"Bidrag utviklere\"-listen, poster vaktmelding i Slack-kanalen " +
            "og oppdaterer \"Sist vaktdato\" på raden.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vaktrotasjon kjørt."),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun triggVaktrotasjon(): ResponseEntity<String> = try {
        vaktlisteService.roterVakthavende()
        ResponseEntity.ok("Vaktrotasjon kjørt.")
    } catch (e: VaktlisteException) {
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message)
    }
}
