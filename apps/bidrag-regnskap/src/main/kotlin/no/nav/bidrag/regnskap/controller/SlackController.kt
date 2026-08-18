package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
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
@Tag(name = "Slack", description = "Sending av varselmeldinger til Slack-kanal.")
class SlackController(private val slackService: SlackService) {

    @PostMapping("/slack")
    @Operation(
        summary = "Send melding til Slack",
        description = "Sender en fritekstmelding til konfigurerert Slack-kanal. Brukes for varsler og driftsinformasjon.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Melding ble sendt."),
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel.", content = [Content()]),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun sendMelding(
        @Parameter(description = "Meldingsteksten som skal sendes til Slack.", required = true)
        @RequestParam(required = true)
        melding: String,
    ) {
        slackService.sendMelding(melding)
    }
}
