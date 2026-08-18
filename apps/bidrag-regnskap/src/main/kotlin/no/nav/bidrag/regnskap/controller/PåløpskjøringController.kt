package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.regnskap.service.PåløpskjøringService
import no.nav.security.token.support.core.api.Protected
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@Protected
@Tag(name = "Påløpskjøring", description = "Start og administrasjon av påløpskjøringer mot ELIN.")
class PåløpskjøringController(
    private val påløpskjøringService: PåløpskjøringService,
) {

    @PostMapping("/palopskjoring")
    @Operation(
        summary = "Start manuel generering av påløpsfil",
        description = "Starter den eldste ikke-gjennomførte påløpskjøringen. Kontroller om fil skal genereres og/eller overføres til ELIN via SFTP.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Påløpskjøring startet. Returnerer ID til påløpet.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = Int::class))],
            ),
            ApiResponse(responseCode = "204", description = "Det finnes ingen ikke gjennomførte påløp.", content = [Content()]),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun startPåløpskjøring(
        @Parameter(description = "Angir om påløpsfil skal genereres.", required = true, example = "true")
        @RequestParam(required = true)
        genererFil: Boolean,
        @Parameter(description = "Angir om påløpsfil skal overføres til ELIN via SFTP.", required = true, example = "true")
        @RequestParam(required = true)
        overførFil: Boolean,
    ): ResponseEntity<Int> {
        val påløp = påløpskjøringService.hentPåløp()?.copy(startetTidspunkt = null) ?: return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
        påløpskjøringService.startPåløpskjøringManuelt(påløp, genererFil, overførFil)
        return ResponseEntity.status(HttpStatus.CREATED).body(påløp.påløpId)
    }

    @GetMapping("/manuellOverforingPåløp")
    @Operation(
        summary = "Start manuell overføring av påløpsfil for dato fra GCP bucket til SFTP",
        description = "Overfører en eksisterende påløpsfil fra GCP bucket til ELIN via SFTP for en spesifikk dato.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Påløpsfil har blitt overført."),
            ApiResponse(responseCode = "400", description = "Fil finnes ikke i GCP bucket for oppgitt dato.", content = [Content()]),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun startManuellOverføringPåløp(
        @Parameter(description = "Dato for påløpsfilen som skal overføres.", required = true, example = "2022-01-01")
        @RequestParam(required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        dato: LocalDate,
    ): ResponseEntity<Any> {
        påløpskjøringService.startManuellOverføringPåløp(dato)
        return ResponseEntity.ok().build()
    }
}
