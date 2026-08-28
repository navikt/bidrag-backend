package no.nav.bidrag.beregn.debug.app.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import no.nav.bidrag.beregn.barnebidrag.service.orkestrering.BidragsberegningOrkestrator
import no.nav.bidrag.transport.behandling.beregning.barnebidrag.BidragsberegningOrkestratorRequestV2
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Import(BidragsberegningOrkestrator::class)
@Unprotected
class BeregnController(val beregnBarnebidragApi: BidragsberegningOrkestrator) {

    @PostMapping("/api/beregn/bidragsberegning/v3")
    @Operation(
        description =
        "Opprett aldersjustering behandling for sak",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Forespørsel oppdatert uten feil",
            ),
        ],
    )
    @Transactional
    fun utførBidragsberegningV3(@RequestBody request: BidragsberegningOrkestratorRequestV2) = beregnBarnebidragApi.utførBidragsberegningV3(request)
}
