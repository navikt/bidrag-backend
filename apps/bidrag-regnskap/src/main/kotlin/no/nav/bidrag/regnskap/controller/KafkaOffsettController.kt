package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@Tag(name = "Kafka offset", description = "Manuell styring av Kafka-offset for vedtakshendelser. Skal kun brukes i eksepsjonelle situasjoner.")
class KafkaOffsettController {

    @Volatile
    private var hoppOverNesteMelding = false

    @Volatile
    private var settOffsett = false

    @Volatile
    private var nyOffsett = -1L

    @PostMapping("/hoppOverNesteVedtakhendselse")
    @Operation(
        summary = "Hopper over neste offset som er lagt inn på kafka-topic",
        description = "Dette endepunktet må kun brukes om man er helt sikker på at neste offset skal hoppes over.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Offset er hoppet over."),
            ApiResponse(responseCode = "400", description = "Klarte ikke å markere neste melding for hopping.", content = [Content()]),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun hoppOverNesteMelding(): ResponseEntity<Any> {
        markerHoppOverNesteMelding()
        return ResponseEntity.ok().build()
    }

    @PostMapping("/settOffsett")
    @Operation(
        summary = "Sett ny Kafka-offset manuelt",
        description = "Setter Kafka-offset til et spesifikt tall for vedtaks-topic. Skal kun brukes om man er helt sikker på at offset skal endres.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Kafka-offset ble satt."),
            ApiResponse(responseCode = "400", description = "Ugyldig offsett-verdi.", content = [Content()]),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun hoppOverNesteMelding(
        @Parameter(description = "Nytt Kafka-offset som skal settes.", required = true, example = "1000")
        @RequestParam(required = true)
        offsett: Long,
    ): ResponseEntity<Any> {
        aktiverOffsettEndring(offsett)
        return ResponseEntity.ok().build()
    }

    fun skalHoppeOverNesteMelding(): Boolean = hoppOverNesteMelding

    fun skalSetteNyOffset(): Boolean = settOffsett

    fun hentNyOffset(): Long = nyOffsett

    fun markerHoppOverNesteMelding() {
        hoppOverNesteMelding = true
    }

    fun tilbakestillHoppOverNesteMelding() {
        hoppOverNesteMelding = false
    }

    fun aktiverOffsettEndring(offsett: Long) {
        settOffsett = true
        nyOffsett = offsett
    }

    fun tilbakestillOffsettEndring() {
        settOffsett = false
    }
}
