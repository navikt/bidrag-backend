package no.nav.bidrag.dokument.arkivering.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.dokument.arkivering.dto.ArkivereJournalpostResponse
import no.nav.bidrag.dokument.arkivering.service.ArkiveringService
import no.nav.security.token.support.core.api.Protected
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@Protected
@RequestMapping("/api/v1/arkivere")
class ArkiveringController {
    @Autowired
    private val arkiveringService: ArkiveringService? = null

    @PostMapping("/journalpost/{id}")
    @Operation(
        description = "Arkivere journalpost med tilhørende dokument i Joark",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Journalpost arkivert",
            ), ApiResponse(
                responseCode = "400",
                description = "Ugyldig format på journalpost-id i spørring (skal bare bestå av tall)",
            ), ApiResponse(
                responseCode = "404",
                description = "Journalposten enten finnes ikke, eller har ikke status reservert",
            ), ApiResponse(
                responseCode = "500",
                description = "Feil oppstod ved arkivering av journalpost",
            ),
        ],
    )
    fun arkivereJournalpost(
        @PathVariable id: String,
    ): ResponseEntity<ArkivereJournalpostResponse> {
        log.info { "Arkiverer journalpost $id sammen med dokument" }
        if (idIkkeBareTall(id)) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val respons = arkiveringService!!.arkivereJournalpost(id)
        return ResponseEntity(respons, HttpStatus.OK)
    }

    private fun idIkkeBareTall(id: String): Boolean {
        val bareTall = id.replace("\\D+".toRegex(), "")
        return id.length != bareTall.length
    }
}
