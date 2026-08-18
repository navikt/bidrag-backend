package no.nav.bidrag.admin.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.admin.dto.EndringsLoggDto
import no.nav.bidrag.admin.dto.LeggTilEndringsloggEndring
import no.nav.bidrag.admin.dto.LestAvBrukerRequest
import no.nav.bidrag.admin.dto.toDto
import no.nav.bidrag.admin.service.EndringsloggService
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Protected
@RequestMapping("/endringslogg")
class EndringsloggEndringController(
    private val endringsloggService: EndringsloggService,
) {
    @PostMapping("/{endringsloggId}/lest/{endringId}")
    @Operation(
        summary = "Oppdater endringslogg at den er lest av bruker. Brukerdetaljer hentes fra token",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun oppdaterLestAvBrukerEndring(
        @PathVariable endringsloggId: Long,
        @PathVariable endringId: Long,
        @RequestBody request: LestAvBrukerRequest,
    ): EndringsLoggDto {
        endringsloggService.oppdaterLestAvBrukerEndring(endringsloggId, endringId, request)
        return endringsloggService.hentEndringslogg(endringsloggId).toDto()
    }

    @DeleteMapping("/{endringsloggId}/endring/{endringId}")
    @Operation(
        summary = "Slett endringslogg endring",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun slettEndring(
        @PathVariable endringsloggId: Long,
        @PathVariable endringId: Long,
    ) = endringsloggService.slettEndring(endringsloggId, endringId).toDto()

    @PostMapping("/{endringsloggId}/endring")
    @Operation(
        summary = "Legg til en ny endring i endringslogg",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun leggTilEndring(
        @PathVariable endringsloggId: Long,
        @RequestBody request: LeggTilEndringsloggEndring,
    ) = endringsloggService.leggTilEndringsloggEndring(endringsloggId, request).toDto()
}
