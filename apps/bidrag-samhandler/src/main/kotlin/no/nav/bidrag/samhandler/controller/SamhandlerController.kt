package no.nav.bidrag.samhandler.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.domene.ident.Ident
import no.nav.bidrag.samhandler.service.SamhandlerService
import no.nav.bidrag.transport.samhandler.SamhandlerDto
import no.nav.bidrag.transport.samhandler.SamhandlerSøk
import no.nav.bidrag.transport.samhandler.SamhandlerValideringsfeil
import no.nav.bidrag.transport.samhandler.SamhandlersøkeresultatDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class SamhandlerController(
    private val samhandlerService: SamhandlerService,
) {
    @PostMapping("/samhandler")
    @Operation(
        description = "Henter samhandler for ident.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returnerer samhandler.",
                content = [
                    ((Content(schema = Schema(implementation = SamhandlerDto::class)))),
                ],
            ),
            ApiResponse(
                responseCode = "204",
                description = "Samhandler finnes ikke.",
            ),
            ApiResponse(
                responseCode = "404",
                description =
                "Innsendt ident er ikke en gyldig samhandler. Samhandlere begynner alltid på 8 eller 9 og har 11 siffer. " +
                    "Evalueres mot regex ^[89]\\d{10}$",
            ),
        ],
    )
    fun hentSamhandler(
        @RequestBody ident: Ident,
        inkluderAuditLog: Boolean = false,
    ): ResponseEntity<*> {
        if (!ident.erSamhandlerId()) return ResponseEntity.badRequest().build<Any>()
        val samhandler =
            samhandlerService.hentSamhandler(ident, inkluderAuditLog) ?: return ResponseEntity.noContent().build<Any>()
        return ResponseEntity.ok(samhandler)
    }

    @PostMapping("/samhandlersok")
    @Operation(
        description = "Søker etter samhandlere.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun samhandlerSøk(
        @RequestBody samhandlerSøk: SamhandlerSøk,
    ): SamhandlersøkeresultatDto = samhandlerService.samhandlerSøk(samhandlerSøk)

    @PostMapping("/opprettSamhandler")
    @Operation(
        description = "Oppretter samhandler.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
            ),
            ApiResponse(
                responseCode = "409",
                description = "Det finnes en identisk samhandler fra før.",
                content = [
                    Content(
                        schema =
                        Schema(
                            implementation =
                            SamhandlerValideringsfeil::class,
                        ),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Validering av samhandlerDto feilet",
                content = [
                    Content(
                        schema = Schema(implementation = SamhandlerValideringsfeil::class),
                    ),
                ],
            ),
        ],
    )
    fun opprettSamhandler(
        @RequestBody samhandlerDto: SamhandlerDto,
    ): ResponseEntity<*> {
        samhandlerService.validerInput(samhandlerDto)

        val samhandlerId = samhandlerService.opprettSamhandler(samhandlerDto)
        return ResponseEntity.ok(samhandlerService.hentSamhandlerPåId(samhandlerId))
    }

    @PostMapping("/oppdaterSamhandler")
    @Operation(
        description = "Oppdaterer samhandler.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Validering av samhandlerDto feilet",
                content = [
                    Content(
                        schema = Schema(implementation = SamhandlerValideringsfeil::class),
                    ),
                ],
            ),
        ],
    )
    fun oppdaterSamhandler(
        @RequestBody samhandlerDto: SamhandlerDto,
    ): ResponseEntity<*> {
        samhandlerService.validerInput(samhandlerDto)
        return samhandlerService.oppdaterSamhandler(samhandlerDto)
    }
}
