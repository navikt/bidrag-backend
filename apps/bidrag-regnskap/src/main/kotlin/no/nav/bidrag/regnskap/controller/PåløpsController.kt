package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.regnskap.dto.påløp.PåløpRequest
import no.nav.bidrag.regnskap.dto.påløp.PåløpResponse
import no.nav.bidrag.regnskap.service.PåløpsService
import no.nav.security.token.support.core.api.Protected
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.YearMonth

@RestController
@Protected
@Tag(name = "Påløp", description = "Oppslag og registrering av planlagte og gjennomførte påløpskjøringer.")
class PåløpsController(
    val påløpsService: PåløpsService,
) {

    @GetMapping("/palop")
    @Operation(
        summary = "Hent påløp",
        description = "Returnerer en liste over alle registrerte påløpskjøringer, inkludert planlagte og allerede gjennomførte.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Liste over påløpskjøringer returnert.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = ArraySchema(schema = Schema(implementation = PåløpResponse::class)))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun hentPåløp(): ResponseEntity<List<PåløpResponse>> = ResponseEntity.ok(påløpsService.hentPåløp())

    @PostMapping("/palop")
    @Operation(
        summary = "Lagre nytt påløp",
        description = "Registrerer en ny planlagt påløpskjøring for en gitt periode. Kjøredato angir når kjøringen er planlagt.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Påløp opprettet. Returnerer ID til det nye påløpet.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = Int::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på kjøredato eller forPeriode.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun lagrePåløp(
        @Parameter(description = "Planlagt kjøredato og -tidspunkt (ISO 8601).", required = true, example = "2022-01-01T10:00:00")
        @RequestParam(required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        kjøredato: LocalDateTime,
        @Parameter(description = "Perioden påløpet gjelder for, på format YYYY-MM.", required = true, example = "2022-02")
        @RequestParam(required = true)
        forPeriode: String,
    ): ResponseEntity<Int> = ResponseEntity.ok(påløpsService.lagrePåløp(PåløpRequest(kjøredato, YearMonth.parse(forPeriode))))
}
