package no.nav.bidrag.aktoerregister.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.aktoerregister.service.PersonHendelseService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@Protected
@Tag(name = "Hendelse", description = "Endepunkter for manuell behandling av personhendelser. Beregnet for test og feilsøking.")
class HendelseController(
    private val personHendelseService: PersonHendelseService,
) {
    @Operation(
        summary = "Behandle personhendelse manuelt",
        description = "Behandler en personhendelse manuelt. Endepunktet er beregnet for testformål og feilsøking – hendelser behandles normalt automatisk fra Kafka.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Hendelse har blitt behandlet."),
            ApiResponse(responseCode = "400", description = "Ugyldig hendelsesformat.", content = [Content()]),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    @PostMapping("/personhendelse")
    fun behandleHendelse(
        @SwaggerRequestBody(description = "Personhendelsen som skal behandles manuelt.", required = true)
        @RequestBody hendelse: String,
    ): ResponseEntity<*> {
        personHendelseService.behandleHendelse(hendelse)
        return ResponseEntity.ok().build<Any>()
    }
}
