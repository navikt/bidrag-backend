package no.nav.bidrag.regnskap.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.bidrag.regnskap.service.VedtakshendelseService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@RestController
@Protected
@Tag(
    name = "Vedtak hendelse",
    description = "Manuell innsending av vedtakshendelser fra Kafka-topic. Beregnet for test og feilsøking.",
)
class VedtakshendelseController(
    private val vedtakshendelseService: VedtakshendelseService,
) {

    @PostMapping("/vedtakHendelse")
    @Operation(
        summary = "Manuelt legg inn meldinger fra kafka topic'en bidrag.vedtak",
        description = "Simulerer mottak av en vedtakshendelse fra Kafka-topic `bidrag.vedtak`. Hendelsen prosesseres på samme måte som ved automatisk mottak. Beregnet for testformål og manuell feilretting.",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Meldingen er lest vellykket."),
            ApiResponse(responseCode = "400", description = "Noe er galt med meldingen.", content = [Content()]),
            ApiResponse(
                responseCode = "401",
                description = "Manglende eller ugyldig Bearer-token. Autentiser på nytt og prøv igjen.",
                content = [Content()],
            ),
            ApiResponse(responseCode = "500", description = "Uventet feil på server.", content = [Content()]),
        ],
    )
    fun opprettHendelse(
        @SwaggerRequestBody(description = "Vedtakshendelsen som skal prosesseres manuelt.", required = true)
        @RequestBody vedtakHendelse: String,
    ): ResponseEntity<Any> {
        vedtakshendelseService.behandleHendelse(vedtakHendelse)
        return ResponseEntity.ok().build()
    }
}
