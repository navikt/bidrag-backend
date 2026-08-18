package no.nav.bidrag.sak.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.domene.sak.Saksnummer
import no.nav.bidrag.sak.security.authentication.ldap.annotation.ProtectedWithBasic
import no.nav.bidrag.sak.service.BidragSakService
import no.nav.bidrag.transport.sak.BidragssakPipDto
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class PipController(
    private val bidragSakService: BidragSakService,
) {
    @GetMapping("/bidrag-sak$PIP_SAK/{saksnummer}")
    @Operation(
        description = "Finn metadata om for en bidragssak",
        security = [SecurityRequirement(name = "basic-auth")],
    )
    @ProtectedWithBasic(groups = [AD_GROUP_PIP_BIDRAGSAK])
    fun hentSakPip(
        @PathVariable saksnummer: Saksnummer,
    ): ResponseEntity<BidragssakPipDto> {
        if (saksnummer.gyldig()) {
            val muligBidragSakDto = bidragSakService.finnPipFor(saksnummer)
            return ResponseEntity(
                muligBidragSakDto,
                if (muligBidragSakDto != null) HttpStatus.OK else HttpStatus.NOT_FOUND,
            )
        }
        return ResponseEntity(HttpStatus.NOT_FOUND)
    }

    @GetMapping("$PIP_SAK_V2/{saksnummer}")
    @Operation(
        description = "Finn metadata om for en bidragssak",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ProtectedWithClaims(issuer = "aad", claimMap = ["roles=sak-pip"])
    fun hentSakPipMedAzureToken(
        @PathVariable saksnummer: Saksnummer,
    ): ResponseEntity<BidragssakPipDto> = hentSakPip(saksnummer)

    companion object {
        const val AD_GROUP_PIP_BIDRAGSAK = "0000-GA-PIP_BIDRAGSAK"
        const val PIP_SAK = "/pip/sak"
        const val PIP_SAK_V2 = "/v2/pip/sak"
    }
}
