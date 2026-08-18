package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.regnskap.dto.oppdrag.OppdragResponse
import no.nav.bidrag.regnskap.dto.oppdrag.OppslagAvOppdragPåSakIdResponse
import no.nav.bidrag.regnskap.dto.patch.OppdaterUtsattTilDatoRequest
import no.nav.bidrag.regnskap.dto.vedtak.UtsatteOgFeiledeVedtak
import no.nav.bidrag.regnskap.dto.vedtak.UtsatteOppdragResponse
import no.nav.bidrag.regnskap.service.OppdragService
import no.nav.bidrag.regnskap.service.OppslagService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@Tag(name = "Oppslag og oppdatering", description = "Oppslag og oppdatering av oppdrag, oppdragsperioder og konteringer i regnskapet.")
class OppslagOgOppdateringController(
    private val oppslagService: OppslagService,
    private val oppdragService: OppdragService,
) {

    @GetMapping("/oppdrag")
    @Operation(
        summary = "Hent lagret oppdrag",
        description = "Operasjon for å hente lagrede oppdrag med tilhørende oppdragsperioder og konteringer. " +
            "Oppdraget returneres med alle historiske konteringer og oppdragsperioder. " +
            "Dette endepunktet er ment til bruk ved feilsøking.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Oppdrag med tilhørende oppdragsperioder og konteringer returnert.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = OppdragResponse::class))],
            ),
            ApiResponse(responseCode = "204", description = "Ingen oppdrag funnet med oppgitt oppdragsId.", content = [Content()]),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på oppdragId.",
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
    fun hentOppdrag(
        @Parameter(description = "ID for oppdraget som skal hentes.", required = true, example = "42")
        @RequestParam(required = true)
        oppdragId: Int,
    ): ResponseEntity<*> {
        val oppdragResponse = oppslagService.hentOppdrag(oppdragId)
        return if (oppdragResponse != null) {
            ResponseEntity.ok(oppdragResponse)
        } else {
            ResponseEntity.status(HttpStatus.NO_CONTENT)
                .header(HttpHeaders.WARNING, "Det finnes ingen oppdrag med angitt oppdragsId: $oppdragId")
                .build<Any>()
        }
    }

    @GetMapping("/sak")
    @Operation(
        summary = "Hent alle lagrede oppdrag, oppdragsperioder, konteringer og overførte konteringer for en sak",
        description = "Operasjon for å hente alle lagrede oppdrag med tilhørende oppdragsperioder, konteringer og " +
            "overførte konteringer for en sak.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returnerer alt lagret for saken.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = OppslagAvOppdragPåSakIdResponse::class))],
            ),
            ApiResponse(responseCode = "204", description = "Ingen data funnet for oppgitt sakId.", content = [Content()]),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på sakId.",
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
    fun hentSak(
        @Parameter(description = "Saksnummer for saken som skal hentes.", required = true, example = "2200001")
        @RequestParam(required = true)
        sakId: String,
    ): ResponseEntity<*> {
        val sakReponse = oppslagService.hentPåSakId(sakId)
        return if (sakReponse != null) {
            ResponseEntity.ok(sakReponse)
        } else {
            ResponseEntity.status(HttpStatus.NO_CONTENT)
                .header(HttpHeaders.WARNING, "Det finnes ingen oppdrag med angitt sakId: $sakId")
                .build<Any>()
        }
    }

    @GetMapping("/utsatteOgFeiledeVedtak")
    @Operation(
        summary = "Hent alle utsatte vedtak for en sak",
        description = "Returnerer alle vedtak knyttet til saksnummeret som er utsatt (ikke prosessert ennå) eller har feilet under prosessering.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returnerer alle utsatte og feilede vedtak for saksnummeret.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = UtsatteOgFeiledeVedtak::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel – f.eks. manglende eller feil format på saksnummer.",
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
    fun hentUtsatteOgFeiledeVedtakForSak(
        @Parameter(description = "Saksnummeret det skal hentes utsatte og feilede vedtak for.", required = true, example = "2200001")
        @RequestParam(required = true)
        saksnummer: Saksnummer,
    ): ResponseEntity<*> = ResponseEntity.ok(oppslagService.hentUtsatteOgFeiledeVedtakForSak(saksnummer))

    @GetMapping("/oppdrag/utsatte")
    @Operation(
        summary = "Henter alle utsatte oppdrag",
        description = "Returnerer alle oppdrag med en fremtidig utsatt-til-dato, dvs. oppdrag som er satt på vent.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Henter alle utsatte oppdrag.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = UtsatteOppdragResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun hentAlleUtsatteOppdrag(): ResponseEntity<*> = ResponseEntity.ok(oppslagService.hentAlleUtsatteOppdrag())

    @PostMapping("oppdrag/utsatte")
    @Operation(
        summary = "Oppdaterer utsatt til dato for et oppdrag",
        description = "Oppdaterer utsatt-til-dato for et oppdrag. Oppdrag med utsatt-til-dato i fortiden vil behandles ved neste kjøring.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Utsatt-til-dato ble oppdatert. Returnerer oppdragsId.",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = Schema(implementation = Int::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Ugyldig forespørsel.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Fant ikke oppdrag med innsendt oppdragsId.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Uventet feil på server.",
                content = [Content()],
            ),
        ],
    )
    fun oppdaterUtsattTilDato(@RequestBody oppdaterUtsattTilDatoRequest: OppdaterUtsattTilDatoRequest): ResponseEntity<*> {
        val oppdatertOppdrag = oppdragService.oppdaterUtsattTilDato(oppdaterUtsattTilDatoRequest) ?: return ResponseEntity.notFound().build<Any>()
        return ResponseEntity.ok(oppdatertOppdrag)
    }
}
