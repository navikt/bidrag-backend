package no.nav.bidrag.admin.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.admin.service.BatchService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/bisys/batch")
@Unprotected
class BisysBatchController(
    val service: BatchService,
) {
    private fun requireNumericId(id: String) {
        if (!id.all { it.isDigit() }) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid id")
    }

    @GetMapping
    @Operation(
        description = "Henter alle batchnavn fra Bisys",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun getBatchNames() = service.getJobNames()

    @GetMapping("/launch/{jobName}")
    @Operation(
        description = "Starter en batchjobb i Bisys",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun launchJob(
        @PathVariable jobName: String,
    ) = service.launchJob(jobName)

    @GetMapping("/running/{jobName}")
    @Operation(
        description = "Henter kjørende batchjobber for et gitt jobbnavn i Bisys",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun runningExecutions(
        @PathVariable jobName: String,
    ) = service.runningExecutions(jobName)

    @GetMapping("/stop/{executionId}")
    @Operation(
        description = "Stopper en kjørende batchjobb i Bisys",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun stopExecution(
        @PathVariable executionId: String,
    ): Boolean? {
        requireNumericId(executionId)
        return service.stopExecution(executionId)
    }

    @GetMapping("/parameters/{executionId}")
    @Operation(
        description = "Hent parametere til jobben som kjører",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun executionParameters(
        @PathVariable executionId: String,
    ): TekstSvar {
        requireNumericId(executionId)
        return TekstSvar(service.getExecutionParameters(executionId))
    }

    @GetMapping("/summary/{executionId}")
    @Operation(
        description = "Hent sammendrag av kjøring",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun executionSummary(
        @PathVariable executionId: String,
    ): TekstSvar {
        requireNumericId(executionId)
        return TekstSvar(service.getExecutionSummary(executionId))
    }

    @GetMapping("/summaries/{executionId}")
    @Operation(
        description = "Hent sammendrag av kjøring",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun executionSummaries(
        @PathVariable executionId: String,
    ): Map<Long, String> {
        requireNumericId(executionId)
        return service.getExecutionSummaries(executionId)
    }
}

// Wrapper som tvinger JSON-serialisering av innholdet fra Bisys for å hindrer cross-site scripting.
data class TekstSvar(val verdi: String?)
