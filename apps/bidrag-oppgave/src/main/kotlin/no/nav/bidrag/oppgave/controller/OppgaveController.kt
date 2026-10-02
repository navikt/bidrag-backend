package no.nav.bidrag.oppgave.controller

import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import no.nav.bidrag.oppgave.service.OppgaveService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class OppgaveController(
    private val oppgaveService: OppgaveService,
) {

    @PostMapping("/oppgaver")
    @ApiResponse(
        responseCode = "200",
        description = "Oppgaver som matcher søket",
        headers = [
            Header(
                name = HEADER_OFFSET,
                description = "Antall oppgaver som ble hoppet over",
                schema = Schema(type = "integer"),
            ),
            Header(
                name = HEADER_LIMIT,
                description = "Maks antall oppgaver i svaret",
                schema = Schema(type = "integer"),
            ),
            Header(
                name = HEADER_TOTAL_COUNT,
                description = "Totalt antall oppgaver som matcher søket. Mangler hvis oppgave-API ikke oppgir det.",
                schema = Schema(type = "integer", format = "int64"),
            ),
        ],
    )
    fun finnOppgaver(
        @RequestBody @Valid request: FinnOppgaverRequest,
    ): ResponseEntity<List<BidragOppgaveDto>> {
        val resultat = oppgaveService.finnOppgaver(request)
        return ResponseEntity.ok()
            .header(HEADER_OFFSET, resultat.offset.toString())
            .header(HEADER_LIMIT, resultat.limit.toString())
            .apply { resultat.antallTreffTotalt?.let { header(HEADER_TOTAL_COUNT, it.toString()) } }
            .body(resultat.oppgaver)
    }

    companion object {
        const val HEADER_OFFSET = "X-Offset"
        const val HEADER_LIMIT = "X-Limit"
        const val HEADER_TOTAL_COUNT = "X-Total-Count"
    }
}
