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
import no.nav.bidrag.regnskap.persistence.entity.Driftsavvik
import no.nav.bidrag.regnskap.service.DriftsavvikService
import no.nav.security.token.support.core.api.Protected
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@Protected
@Tag(name = "Driftsavvik", description = "Administrasjon og oppslag av driftsavvik som påvirker konteringsbehandling.")
class DriftsavvikController(
    private val driftsavvikService: DriftsavvikService,
) {

    @GetMapping("/aktiveDriftsavvik")
    @Operation(
        summary = "Henter alle aktive driftsavvik",
        description = "Returnerer alle driftsavvik som er aktive på nåværende tidspunkt, dvs. der tidspunktFra er passert og tidspunktTil enten ikke er satt eller ikke er passert.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Aktive driftsavvik returnert.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = ArraySchema(schema = Schema(implementation = Driftsavvik::class)))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun hentAlleAktiveDriftsavvik(): ResponseEntity<List<Driftsavvik>> = ResponseEntity.ok(driftsavvikService.hentAlleAktiveDriftsavvik())

    @GetMapping("/driftsavvik")
    @Operation(
        summary = "Henter første driftsavvik",
        description = "Returnerer de siste N driftsavvikene, sortert med nyeste først. Standard er 1000.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Driftsavvik returnert.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = ArraySchema(schema = Schema(implementation = Driftsavvik::class)))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun hentDriftsavvik(
        @Parameter(description = "Maks antall driftsavvik som returneres.", required = false, example = "1000")
        @RequestParam(required = false)
        antallDriftsavvik: Int = 1000,
    ): ResponseEntity<List<Driftsavvik>> = ResponseEntity.ok(driftsavvikService.hentFlereDriftsavvik(antallDriftsavvik))

    @PostMapping("/driftsavvik")
    @Operation(
        summary = "Oppretter nytt driftsavvik",
        description = "Oppretter et nytt driftsavvik for angitt tidsperiode. Returnerer ID-en til det opprettede driftsavviket. Dersom `skalStoppeInnlesning` er satt til `true`, vil nye vedtakshendelser ikke prosesseres i perioden.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Driftsavvik opprettet. Returnerer ID til det nye driftsavviket.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = Int::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på tidspunktFra eller tidspunktTil.",
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
    fun lagreDriftsavvik(
        @Parameter(description = "Starttidspunkt for driftsavviket (ISO 8601).", required = true, example = "2022-01-01T10:00:00")
        @RequestParam(required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        tidspunktFra: LocalDateTime,
        @Parameter(description = "Sluttidspunkt for driftsavviket (ISO 8601). Kan settes i etterkant via PUT.", required = false, example = "2022-01-02T10:00:00")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        tidspunktTil: LocalDateTime?,
        @Parameter(description = "Navn eller ID på den som oppretter driftsavviket.", required = false)
        @RequestParam(required = false)
        opprettetAv: String?,
        @Parameter(description = "Fritekstbeskrivelse av årsaken til driftsavviket.", required = false)
        @RequestParam(required = false)
        årsak: String?,
        @Parameter(description = "Angir om nye vedtakshendelser skal stoppes fra å prosesseres i avviksperioden.", required = false)
        @RequestParam(required = false)
        skalStoppeInnlesning: Boolean?,
    ): ResponseEntity<Int> = ResponseEntity.ok(driftsavvikService.lagreDriftsavvik(tidspunktFra, tidspunktTil, opprettetAv, årsak, skalStoppeInnlesning))

    @PutMapping("/driftsavvik")
    @Operation(
        summary = "Sett tidspunktTil for et driftsavvik",
        description = "Oppdaterer tidspunktTil og/eller skalStoppeInnlesning for et eksisterende driftsavvik. Brukes typisk for å sette sluttidspunkt når et driftsavvik er over.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Driftsavvik ble oppdatert. Returnerer det oppdaterte driftsavviket.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = Driftsavvik::class))],
            ),
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel.", content = [Content()]),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "404", description = "Fant ingen driftsavvik med oppgitt ID.", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun endreDriftsavvik(
        @Parameter(description = "ID for driftsavviket som skal endres.", required = true, example = "1")
        @RequestParam(required = true)
        driftsavvikId: Int,
        @Parameter(description = "Nytt sluttidspunkt for driftsavviket (ISO 8601).", required = false, example = "2022-01-02T10:00:00")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        tidspunktTil: LocalDateTime?,
        @Parameter(description = "Angir om innlesning skal stoppes.", required = false)
        @RequestParam(required = false)
        skalStoppeInnlesning: Boolean?,
    ): ResponseEntity<*> {
        val driftsavvik = driftsavvikService.endreDriftsavvik(driftsavvikId, tidspunktTil, skalStoppeInnlesning) ?: return ResponseEntity.badRequest()
            .body("Finner ingen driftsavvik med id: $driftsavvikId")

        return ResponseEntity.ok(driftsavvik)
    }

    @PostMapping("/driftsavvik/slippVedtakGjennom")
    @Operation(
        summary = "Slipp enkeltvedtak gjennom driftsavvik",
        description = "Tillater at et spesifikt vedtak behandles selv om det er et aktivt driftsavvik som stopper innlesning.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vedtaket ble sluppet gjennom."),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på vedtakId.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "404", description = "Fant ikke vedtak med oppgitt ID.", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun slippVedtakGjennomDriftsavvik(
        @Parameter(description = "ID for vedtaket som skal slippes gjennom driftsavviket.", required = true, example = "123")
        @RequestParam(required = true)
        vedtakId: Int,
    ): ResponseEntity<*> {
        driftsavvikService.slippVedtakGjennomDriftsavvik(vedtakId)
        return ResponseEntity.ok().build<Any>()
    }
}
