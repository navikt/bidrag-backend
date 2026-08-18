package no.nav.bidrag.aktoerregister.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.aktoerregister.batch.person.PersonBatch
import no.nav.bidrag.aktoerregister.batch.samhandler.SamhandlerBatch
import no.nav.bidrag.aktoerregister.service.DuplikathåndteringService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture

private val LOGGER = KotlinLogging.logger {}

@RestController
@Protected
@Tag(name = "Batch", description = "Endepunkter for manuell start av batch-jobber og håndtering av duplikater i aktørregisteret.")
class BatchController(
    private val samhandlerBatch: SamhandlerBatch,
    private val personBatch: PersonBatch,
    private val duplikathåndteringService: DuplikathåndteringService,
) {

    @Operation(
        summary = "Start kjøring av Samhandler batch",
        description = "Samhandler-batchen startes asynkront og oppdaterer alle samhandlere mot kildesystem. Feil under batch-kjøring reflekteres ikke i HTTP-responskoden.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Samhandler batchen ble startet."),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    @PostMapping("/samhandlerBatch")
    fun startSamhandlerBatch(): ResponseEntity<*> {
        CompletableFuture.runAsync {
            try {
                samhandlerBatch.startSamhandlerBatch()
            } catch (e: Exception) {
                LOGGER.error(e) { "Manuell start av batchen feilet med følgende feilkode: ${e.message}" }
            }
        }
        return ResponseEntity.ok().build<Any>()
    }

    @Operation(
        summary = "Start kjøring av Person batch",
        description = "Person-batchen startes asynkront og oppdaterer alle personer mot kildesystem. Feil under batch-kjøring reflekteres ikke i HTTP-responskoden.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Person batchen ble startet."),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    @PostMapping("/personBatch")
    fun startPersonBatch(): ResponseEntity<*> {
        CompletableFuture.runAsync {
            try {
                personBatch.startPersonBatch()
            } catch (e: Exception) {
                LOGGER.error(e) { "Manuell start av batchen feilet med følgende feilkode: ${e.message}" }
            }
        }
        return ResponseEntity.ok().build<Any>()
    }

    @Operation(
        summary = "Finn duplikate aktører",
        description = "Henter en liste over aktører som er registrert med duplikat-ident i aktørregisteret.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Duplikate aktører returnert."),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    @GetMapping("/duplikater")
    fun finnDuplikater(): ResponseEntity<*> = ResponseEntity.ok().body(duplikathåndteringService.finnDuplikater())

    @Operation(
        summary = "Rydd opp duplikate aktører",
        description = "Kjører opprydding av duplikate aktører i aktørregisteret. Duplikater slås sammen eller fjernes.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Opprydding av duplikate aktører startet."),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    @PostMapping("/duplikatopprydding")
    fun duplikathåndtering(): ResponseEntity<*> {
        duplikathåndteringService.ryddOppDuplikater()
        return ResponseEntity.ok().build<Any>()
    }
}
