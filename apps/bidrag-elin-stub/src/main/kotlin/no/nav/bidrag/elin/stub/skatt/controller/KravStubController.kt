package no.nav.bidrag.elin.stub.skatt.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.nav.bidrag.elin.stub.skatt.dto.OppdatertStatus
import no.nav.bidrag.elin.stub.skatt.service.KravStubService
import no.nav.bidrag.transport.regnskap.behandlingsstatus.BehandlingsstatusResponse
import no.nav.bidrag.transport.regnskap.krav.KravResponse
import no.nav.bidrag.transport.regnskap.krav.Kravliste
import no.nav.bidrag.transport.regnskap.vedlikeholdsmodus.Vedlikeholdsmodus
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("bidrag/v1/bidragskrav")
@ProtectedWithClaims(issuer = "maskinporten", claimMap = ["scope=nav:bidrag/v1/bidragskrav"])
class KravStubController(
    val kravStubService: KravStubService,
) {
    @PostMapping("/api/krav")
    @Operation(
        description = "Stub for mottak av krav fra Bidrag-Regnskap som skal sendes til Skatteetaten.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "202",
                description = "Alle konteringer ble overført OK. Returnerer en BatchUid.",
                content = [
                    Content(
                        mediaType = "application/json",
                        array = ArraySchema(schema = Schema(implementation = KravResponse::class)),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "En av konteringene gikk ikke gjennom validering. Liste over feilede konteringer returneres.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Sikkerhetstoken er ikke gyldig",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Sikkerhetstoken er ikke gyldig, eller det er ikke gitt adgang til kode 6 og 7 (nav-ansatt)",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Ukjent feil.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "503",
                description = "Påløpsmodusen er på.",
                content = [Content()],
            ),
        ],
    )
    @ResponseBody
    fun lagreKrav(@RequestBody kravliste: Kravliste): ResponseEntity<Any> = kravStubService.lagreKrav(kravliste)

    @PutMapping("/api/feilPaKrav")
    @Operation(
        description = "Endre om innsending av krav mot stub skal feile eller ikke.",
    )
    fun endreFeilPåKrav(@RequestParam(required = true) skalFeilePåInnsendingAvKrav: Boolean): ResponseEntity<OppdatertStatus> = ResponseEntity.ok(
        OppdatertStatus(
            "Feil ved oversending av krav slått ${
                if (kravStubService.oppdaterFeilPåKrav(
                        skalFeilePåInnsendingAvKrav,
                    )
                ) {
                    "PÅ"
                } else {
                    "AV"
                }
            }",
        ),
    )

    @PutMapping("/api/behandlingsstatus")
    @Operation(
        description = "Endre om kall på behandlingsstatus med batch-uid skal feile eller ikke.",
    )
    fun endreBehandlingsstatus(@RequestParam(required = true) skalFeilePåKallMotBehandlingsstatus: Boolean): ResponseEntity<OppdatertStatus> = ResponseEntity.ok(
        OppdatertStatus(
            "Feil ved kall mot behandlingsstatus slått ${
                if (kravStubService.oppdatertFeilPåBehandlingsstatus(skalFeilePåKallMotBehandlingsstatus)) "PÅ" else "AV"
            }",
        ),
    )

    @PostMapping("api/vedlikeholdsmodus")
    @Operation(
        description = "Stub for slå av og på vedlikeholdsmodus.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Oppdateringen av vedlikeholdsmodus er OK.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Sikkerhetstoken er ikke gyldig",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Sikkerhetstoken er ikke gyldig, eller det er ikke gitt adgang til kode 6 og 7 (nav-ansatt)",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Ukjent feil.",
                content = [Content()],
            ),
        ],
    )
    @ResponseBody
    fun oppdaterVedlikeholdsmodus(@RequestBody vedlikeholdsmodus: Vedlikeholdsmodus): ResponseEntity<Any> = kravStubService.oppdaterVedlikeholdsmodus(vedlikeholdsmodus)

    @GetMapping("api/liveness")
    @Operation(
        description = "Stub for å sjekke status på vedlikeholdsmodus.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Vedlikeholdsmodus er ikke på.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "400",
                description =
                "Dersom én av konteringene ikke går gjennom validering forkastes alle konteringene i kravet " +
                    "og en liste over konteringer som har feilet returneres, sammen med informasjon om hva som er feil.\n" +
                    "\n" +
                    "Det er ingen garanti for at konteringer som ikke kommer med på listen over feilede konteringer er feilfrie.\n " +
                    "NB: Dette er ikke implementert i stub.",
                content = [
                    Content(),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Sikkerhetstoken er ikke gyldig",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Sikkerhetstoken er ikke gyldig, eller det er ikke gitt adgang til kode 6 og 7 (nav-ansatt)",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "500",
                description = "Ukjent feil.",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "503",
                description = "Påløpsmodus er på.",
                content = [Content()],
            ),
        ],
    )
    @ResponseBody
    fun liveness(): ResponseEntity<Any> = kravStubService.liveness()

    @GetMapping("api/krav/{batchUid}")
    @Operation(
        description = "Stub for å sjekke behandlingsstatus for batch-uid.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Behandlingsstatus for tidligere leverte konteringer.",
                content = [
                    Content(
                        mediaType = "application/json",
                        array = ArraySchema(schema = Schema(implementation = BehandlingsstatusResponse::class)),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "503",
                description = "Påløpsmodus er på.",
                content = [Content()],
            ),
        ],
    )
    @ResponseBody
    fun hentBehandlingsstatus(@PathVariable batchUid: String): ResponseEntity<BehandlingsstatusResponse> = kravStubService.hentBehandlingsstatus(batchUid)
}
