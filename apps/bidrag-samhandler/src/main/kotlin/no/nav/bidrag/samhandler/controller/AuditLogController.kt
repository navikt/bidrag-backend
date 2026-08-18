package no.nav.bidrag.samhandler.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.samhandler.service.AuditLogService
import no.nav.bidrag.transport.felles.AuditLogDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
class AuditLogController(
    private val auditLogService: AuditLogService,
) {
    @PostMapping("/auditlog")
    @Operation(
        description = "Henter audit log for samhandler.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Returnerer audit log.",
                content = [
                    ((Content(schema = Schema(implementation = AuditLogDto::class)))),
                ],
            ),
            ApiResponse(
                responseCode = "204",
                description = "Audit log finnes ikke.",
            ),
        ],
    )
    fun hentAuditLog(
        @RequestBody samhandlerId: Int,
    ): ResponseEntity<*> {
        val auditLog = auditLogService.hentAuditLogForSamhandler(samhandlerId)
        return if (auditLog.isEmpty()) {
            ResponseEntity.notFound().build<Any>()
        } else {
            ResponseEntity.ok(auditLog)
        }
    }
}
