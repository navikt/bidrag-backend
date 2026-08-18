package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.bidrag.domene.enums.vedtak.Stønadstype
import no.nav.bidrag.regnskap.service.AvstemmingService
import no.nav.bidrag.transport.regnskap.avstemning.SumPrSakResponse
import no.nav.security.token.support.core.api.Protected
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@RestController
@Protected
@Tag(name = "Avstemming", description = "Generering og overføring av avstemmings- og summeringsfiler til ELIN via SFTP.")
class AvstemmingController(
    private val avstemmingService: AvstemmingService,
) {

    @OptIn(DelicateCoroutinesApi::class)
    @GetMapping("/avstemming")
    @Operation(
        summary = "Start manuell generering av avstemming- og summeringsfil for dato",
        description = "Operasjon for å starte generering av avstemmingsfil og summeringsfil for alle konteringer lest inn en spesifikk dato." +
            "Disse filene blir lastet opp i bucket på GCP og deretter overført til en sftp filsluse hvor ELIN plukker ned filene.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Avstemmingsfilene har blitt generert."),
            ApiResponse(
                responseCode = "400",
                description = "Dato er satt frem i tid. Generering blir derfor ikke startet.",
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
    fun startAvstemmingsgenerering(
        @Parameter(description = "Dato det skal genereres avstemmingsfil for.", required = true, example = "2022-01-01")
        @RequestParam(required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        dato: LocalDate,
        @Parameter(description = "Settes om man ønsker å starte fra ett spesifikt tidspunkt. Er avhengig av tomTidspunkt.", required = false, example = "2022-01-01T10:00:00")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        fomTidspunkt: LocalDateTime?,
        @Parameter(description = "Settes om man ønsker å slutte på ett spesifikt tidspunkt. Er avhengig av fomTidspunkt.", required = false, example = "2022-01-01T11:00:00")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        tomTidspunkt: LocalDateTime?,
    ): ResponseEntity<Any> {
        if (dato.isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest().build()
        }
        GlobalScope.launch {
            avstemmingService.startAvstemming(dato, fomTidspunkt, tomTidspunkt)
        }
        return ResponseEntity.ok().build()
    }

    @GetMapping("/manuellOverforingAvstemning")
    @Operation(
        summary = "Start manuell overføring av avstemming- og summeringsfil for dato fra GCP bucket til SFTP",
        description = "Operasjon for å starte manuell overføring av avstemming-og summeringsfil for en spesifikk dato." +
            "Hentes fra bucket på GCP og deretter overført til en sftp filsluse hvor ELIN plukker ned filene.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Avstemming- og summeringsfil har blitt overført."),
            ApiResponse(responseCode = "400", description = "Fil finnes ikke i GCP bucket for oppgitt dato.", content = [Content()]),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun startManuellOverføringAvstemning(
        @Parameter(description = "Dato for filen som skal overføres.", required = true, example = "2022-01-01")
        @RequestParam(required = true)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        dato: LocalDate,
    ): ResponseEntity<Any> {
        avstemmingService.startManuellOverføringAvstemingTilSftpFraGcpBucket(dato)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/sumprsak/{stønadstype}/{periode}")
    @Operation(
        summary = "Hent sum for saker per stønadstype og periode",
        description = "Henter alle saker med beregnet sum for en kombinasjon av stønadstype og periode.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Saker med sum for stønadstype og periode hentet.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = SumPrSakResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på stønadstype eller periode.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "404", description = "Ingen saker funnet for oppgitt stønadstype og periode.", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun hentSumForSaker(
        @Parameter(description = "Stønadstype det skal hentes sum for.", required = true, example = "FORSKUDD")
        @PathVariable(required = true)
        stønadstype: Stønadstype,
        @Parameter(description = "Periode på format YYYY-MM.", required = true, example = "2022-01")
        @PathVariable(required = true)
        periode: YearMonth,
    ): ResponseEntity<SumPrSakResponse> {
        val sumPrSakResponse = avstemmingService.hentSumForSaker(stønadstype, periode)
        return ResponseEntity.ok(sumPrSakResponse)
    }
}
