package no.nav.bidrag.admin.vaktrotasjon

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.commons.service.slack.SlackService
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@Tag(name = "Vaktrotasjon", description = "Endepunkter for manuell trigging av vaktrotasjon og test av Slack-varsling.")
class VaktrotasjonController(
    private val vaktrotasjonService: VaktrotasjonService,
    private val slackService: SlackService,
) {
    @PostMapping("/vakt/rotasjon/trigger")
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
    fun triggVaktrotasjon() {
        vaktrotasjonService.kjørRotasjon()
    }

    @PostMapping("/vakt/test-melding")
    @Operation(
        summary = "Send testmelding til vaktkanalen",
        description = "Sender en enkel testmelding til den konfigurerte Slack-kanalen uten å røre vaktlisten. Brukes for å " +
            "verifisere at Slack-oppsettet (token, kanal) fungerer.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Testmelding sendt."),
            ApiResponse(responseCode = "401", description = "Ikke autentisert."),
            ApiResponse(responseCode = "403", description = "Ikke autorisert."),
            ApiResponse(responseCode = "500", description = "Intern serverfeil."),
        ],
    )
    fun sendTestmelding(
        @Parameter(description = "Valgfri egendefinert testtekst.")
        @RequestParam(
            required = false,
            defaultValue = "Dette er en testmelding fra bidrag-admin for å verifisere vaktrotasjon-integrasjonen mot Slack.",
        )
        melding: String,
    ) {
        slackService.sendMelding(melding)
    }
}
