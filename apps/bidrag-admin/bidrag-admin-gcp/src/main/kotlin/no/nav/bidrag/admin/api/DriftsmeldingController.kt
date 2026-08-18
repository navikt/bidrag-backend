package no.nav.bidrag.admin.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.admin.dto.LeggTilDriftsmeldingHistorikkRequest
import no.nav.bidrag.admin.dto.OppdaterDriftsmeldingHistorikkRequest
import no.nav.bidrag.admin.dto.OppdaterDriftsmeldingRequest
import no.nav.bidrag.admin.dto.OpprettDriftsmeldingRequest
import no.nav.bidrag.admin.dto.toDto
import no.nav.bidrag.admin.service.DriftsmeldingService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Unprotected
@RequestMapping("/driftsmelding")
class DriftsmeldingController(
    private val driftsmeldingService: DriftsmeldingService,
) {
    @GetMapping
    @Operation(
        summary = "Hent alle aktive driftsmeldinger",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun hentAlleAktiveDriftsmeldinger() = driftsmeldingService
        .hentAlleAktiv()
        .sortedBy { it.aktivFraTidspunkt }
        .map { it.toDto() }

    @GetMapping("/{driftsmeldingId}")
    @Operation(
        summary = "Hent en enkel driftsmelding",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun hentDriftsmelding(
        @PathVariable driftsmeldingId: Long,
    ) = driftsmeldingService.hentDriftsmelding(driftsmeldingId).toDto()

    @PostMapping
    @Operation(
        summary = "Opprett driftsmelding",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun opprettDriftsmelding(
        @RequestBody request: OpprettDriftsmeldingRequest,
    ) = driftsmeldingService.opprettDriftsmelding(request).toDto()

    @PostMapping("/{driftsmeldingId}/historikk")
    @Operation(
        summary = "Opprett driftsmelding historikk",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun opprettDriftsmeldingHistorikk(
        @PathVariable driftsmeldingId: Long,
        @RequestBody request: LeggTilDriftsmeldingHistorikkRequest,
    ) = driftsmeldingService.leggTilDriftsmeldingHistorikk(driftsmeldingId, request).toDto()

    @PutMapping("/{driftsmeldingId}")
    @Operation(
        summary = "Oppdater driftsmelding",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun oppdaterDriftsmelding(
        @PathVariable driftsmeldingId: Long,
        @RequestBody request: OppdaterDriftsmeldingRequest,
    ) = driftsmeldingService.oppdaterDriftsmelding(driftsmeldingId, request).toDto()

    @DeleteMapping("/{driftsmeldingId}/historikk/{driftsmeldingHistorikkId}")
    @Operation(
        summary = "Slett driftsmelding historikk",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun slettDriftsmeldingHistorikk(
        @PathVariable driftsmeldingId: Long,
        @PathVariable driftsmeldingHistorikkId: Long,
    ) = driftsmeldingService.slettDriftsmeldingHistorikk(driftsmeldingId, driftsmeldingHistorikkId).toDto()

    @PutMapping("/{driftsmeldingId}/historikk/{driftsmeldingHistorikkId}")
    @Operation(
        summary = "Oppdater driftsmelding historikk",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun oppdaterDriftsmeldingHistorikk(
        @PathVariable driftsmeldingId: Long,
        @PathVariable driftsmeldingHistorikkId: Long,
        @RequestBody request: OppdaterDriftsmeldingHistorikkRequest,
    ) = driftsmeldingService.oppdaterDriftsmeldingHistorikk(driftsmeldingId, driftsmeldingHistorikkId, request).toDto()

    @PostMapping("/{driftsmeldingId}/{driftsmeldingHistorikkId}/lest")
    @Operation(
        summary = "Oppdater at driftsmelding er lest av bruker. Brukerdetaljer hentes fra token",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun oppdaterLestAvBruker(
        @PathVariable driftsmeldingId: Long,
        @PathVariable driftsmeldingHistorikkId: Long,
    ) = driftsmeldingService.oppdaterLestAvBruker(driftsmeldingId, driftsmeldingHistorikkId)

    @PatchMapping("/{driftsmeldingId}/deaktiver")
    @Operation(
        summary = "Deaktiver driftsmelding",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun deaktiverDriftsmelding(
        @PathVariable driftsmeldingId: Long,
    ) = driftsmeldingService.deaktiverDriftsmelding(driftsmeldingId).toDto()

    @PatchMapping("/{driftsmeldingId}/aktiver")
    @Operation(
        summary = "Aktiver driftsmelding",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun aktiverDriftsmelding(
        @PathVariable driftsmeldingId: Long,
    ) = driftsmeldingService.aktiverDriftsmelding(driftsmeldingId).toDto()
}
