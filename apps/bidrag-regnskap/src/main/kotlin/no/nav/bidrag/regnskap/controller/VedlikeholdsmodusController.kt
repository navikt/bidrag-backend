package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.domene.enums.regnskap.Årsakskode
import no.nav.bidrag.regnskap.consumer.SkattConsumer
import no.nav.bidrag.transport.regnskap.vedlikeholdsmodus.Vedlikeholdsmodus
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Tag(
    name = "Vedlikeholdsmodus",
    description = "Styring av vedlikeholdsmodus i Skatteetatens regnskap-API (ELIN). Vedlikeholdsmodus stopper oversending av konteringer.",
)
class VedlikeholdsmodusController(
    private val skattConsumer: SkattConsumer,
) {

    @PostMapping("/vedlikeholdsmodus")
    @Operation(
        summary = "Manuelt endrer status på vedlikeholdsmodus",
        description = "Slår vedlikeholdsmodus av eller på i ELIN. Når vedlikeholdsmodus er aktiv stoppes oversending av konteringer.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Vedlikeholdsmodus ble oppdatert."),
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel.", content = [Content()]),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
            ApiResponse(responseCode = "504", description = "Tidsavbrudd (Gateway Timeout) mot ELIN.", content = [Content()]),
        ],
    )
    fun endreVedlikeholdsmodus(
        @Parameter(description = "Sett til `true` for å aktivere vedlikeholdsmodus, `false` for å deaktivere.", required = true, example = "true")
        @RequestParam(required = true)
        aktiv: Boolean,
        @Parameter(description = "Kode som beskriver årsaken til vedlikeholdsmodusen.", required = true)
        @RequestParam(required = true)
        årsakskode: Årsakskode,
        @Parameter(description = "Fritekstkommentar som beskriver årsaken til endringen av vedlikeholdsmodus.", required = true, example = "Påløp for 2022-12 genereres hos NAV.")
        @RequestParam(required = true)
        kommentar: String,
    ): Any? = skattConsumer.oppdaterVedlikeholdsmodus(Vedlikeholdsmodus(aktiv, årsakskode, kommentar)).body

    @GetMapping("/vedlikeholdsmodus")
    @Operation(
        summary = "Sjekker status på vedlikeholdsmodus",
        description = "Sjekker om vedlikeholdsmodus er aktiv i ELIN. 200 betyr at systemet er operativt. 503 betyr at ELIN er i vedlikeholdsmodus.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "ELIN er operativt – vedlikeholdsmodus er ikke aktiv."),
            ApiResponse(responseCode = "401", description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
            ApiResponse(responseCode = "503", description = "ELIN er i vedlikeholdsmodus – oversending av konteringer er stoppet."),
            ApiResponse(responseCode = "504", description = "Tidsavbrudd (Gateway Timeout) mot ELIN.", content = [Content()]),
        ],
    )
    fun sjekkStatusPåVedlikeholdsmodus(): ResponseEntity<Any> = skattConsumer.hentStatusPåVedlikeholdsmodus()
}
