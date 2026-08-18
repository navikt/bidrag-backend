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
import no.nav.bidrag.regnskap.dto.patch.OppdaterReferanseRequest
import no.nav.bidrag.regnskap.dto.patch.PatchMottakerRequest
import no.nav.bidrag.regnskap.dto.patch.ReferanseForVedtakResponse
import no.nav.bidrag.regnskap.service.OppdragService
import no.nav.bidrag.regnskap.service.PatchService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@Tag(name = "Patch", description = "Manuelle korrigeringer av mottaker, kravhaver og referanser på oppdrag og oppdragsperioder.")
class PatchController(
    private val oppdragService: OppdragService,
    private val patchService: PatchService,
) {

    @PostMapping("/patchMottaker")
    @Operation(
        summary = "Oppdater mottaker for sak",
        description = "Oppdaterer mottaker og kravhaver på alle oppdrag tilknyttet et saksnummer.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Mottaker ble oppdatert."),
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel.", content = [Content()]),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "404", description = "Sak ikke funnet.", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun patchMottaker(@RequestBody patchMottakerRequest: PatchMottakerRequest) {
        oppdragService.patchMottaker(patchMottakerRequest.saksnummer, patchMottakerRequest.kravhaver, patchMottakerRequest.mottaker)
    }

    @GetMapping("/hentReferanseForVedtak")
    @Operation(
        summary = "Hent referanser for vedtak",
        description = "Returnerer alle oppdragsperiode-referanser knyttet til et vedtak.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Referanser returnert.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = ArraySchema(schema = Schema(implementation = ReferanseForVedtakResponse::class)))],
            ),
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel.", content = [Content()]),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun hentReferanseForVedtak(
        @Parameter(description = "ID for vedtaket.", required = true, example = "42")
        @RequestParam(required = true)
        vedtakId: Int,
    ): ResponseEntity<List<ReferanseForVedtakResponse>> {
        val referanseForVedtak = patchService.hentReferanseForVedtak(vedtakId)
        return ResponseEntity.ok(referanseForVedtak)
    }

    @GetMapping("/hentReferanseForSak")
    @Operation(
        summary = "Hent referanser for sak",
        description = "Returnerer alle oppdragsperiode-referanser knyttet til et saksnummer.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Referanser returnert.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = ArraySchema(schema = Schema(implementation = ReferanseForVedtakResponse::class)))],
            ),
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel.", content = [Content()]),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun hentReferanseForSak(
        @Parameter(description = "Saksnummeret.", required = true, example = "2200001")
        @RequestParam(required = true)
        saksnummer: String,
    ): ResponseEntity<List<ReferanseForVedtakResponse>> {
        val referanseForVedtak = patchService.hentReferanseForSak(saksnummer)
        return ResponseEntity.ok(referanseForVedtak)
    }

    @GetMapping("/hentAlleVedtakMedTommeReferanser")
    @Operation(
        summary = "Hent vedtak med tomme referanser",
        description = "Returnerer en liste over vedtaks-IDer som har oppdragsperioder uten referanse.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Vedtaks-IDer returnert.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = ArraySchema(schema = Schema(implementation = Int::class)))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun hentAlleVedtakMedTommeReferanser(): ResponseEntity<List<Int>> {
        val tommeRefernaser = patchService.hentAlleTommeReferanser()
        return ResponseEntity.ok(tommeRefernaser)
    }

    @PostMapping("/patchReferanseForOppdragsperiode")
    @Operation(
        summary = "Oppdater referanse for oppdragsperiode",
        description = "Setter referansen på en oppdragsperiode basert på innsendt forespørsel.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Referanse ble oppdatert."),
            ApiResponse(responseCode = "400", description = "Ugyldig forespørsel.", content = [Content()]),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "404", description = "Oppdragsperiode ikke funnet.", content = [Content()]),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun patchReferanseForOppdragsperiode(@RequestBody oppdaterReferanseRequest: OppdaterReferanseRequest) {
        patchService.oppdaterReferanseForOppdragsperiode(oppdaterReferanseRequest)
    }

    @PostMapping("/patchTommerReferanser")
    @Operation(
        summary = "Patch alle tomme referanser",
        description = "Kjører en oppryddingsjobb som setter referanse på alle oppdragsperioder som mangler det.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Oppryddingsjobb kjørte."),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun patchTommerReferanser() {
        patchService.patchTommeReferanser()
    }
}
