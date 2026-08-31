package no.nav.bidrag.dokument.produksjon.api

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.models.examples.Example
import no.nav.bidrag.dokument.produksjon.SIKKER_LOGG
import no.nav.bidrag.dokument.produksjon.service.PdfProducerService
import no.nav.bidrag.dokument.produksjon.util.getObjectmapper
import no.nav.bidrag.transport.dokumentmaler.DokumentBestilling
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.context.annotation.Bean
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Paths
import kotlin.io.path.readText

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v2/dokumentmal")
class ProduserDokumentmalApi(
    val pdfProducerService: PdfProducerService,
) {

    @PostMapping("/pdf/{dokumentmal}")
    @RequestBody(
        content =
        [
            Content(
                examples =
                [
                    ExampleObject(
                        ref = "#/components/examples/Vedtak bidrag",
                        name = "Vedtak bidrag",
                    ),
                ],
            ),
        ],
    )
    fun generatePDF(
        @PathVariable dokumentmal: String,
        @org.springframework.web.bind.annotation.RequestBody payload: DokumentBestilling,
    ): ResponseEntity<ByteArray> {
        log.info { "Produserer notat PDF 2 for dokumentmal $dokumentmal" }
        return pdfProducerService.generatePDFResponseV2(
            "vedtak ",
            dokumentmal,
            commonObjectmapper.writeValueAsString(payload),
        )
    }

    @PostMapping("/html/{dokumentmal}")
    @Operation(
        description = "Beregn bidrag",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    @RequestBody(
        content =
        [
            Content(
                examples =
                [
                    ExampleObject(
                        ref = "#/components/examples/Vedtak bidrag",
                        name = "Vedtak bidrag",
                    ),
                ],
            ),
        ],
    )
    fun generateHTML(
        @PathVariable dokumentmal: String,
        @org.springframework.web.bind.annotation.RequestBody payload: DokumentBestilling,
    ): ResponseEntity<String> {
        SIKKER_LOGG.info {
            "Produserer notat HTML for dokumentmal $dokumentmal for input $payload"
        }
        log.info { "Produserer notat HTML for dokumentmal $dokumentmal" }
        return pdfProducerService.generateHTMLResponseV2(
            "vedtak",
            dokumentmal,
            getObjectmapper().writeValueAsString(payload),
        )
    }

    @GetMapping("/html/{dokumentmal}")
    @Operation(
        description = "Beregn bidrag",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun generateHTMLDebug(
        @PathVariable dokumentmal: String,
    ): ResponseEntity<String> {
        log.info { "Produserer notat HTML for dokumentmal $dokumentmal" }
        return pdfProducerService.generateHTMLResponseV2(
            "vedtak",
            dokumentmal,
            Paths.get("data/vedtak/klage_orkestrering.json").readText(),
        )
    }

    @GetMapping("/pdf/{dokumentmal}")
    @Operation(
        description = "Beregn bidrag",
        security = [SecurityRequirement(name = "bearer-key")],
    )
    fun generatePDFDebug(
        @PathVariable dokumentmal: String,
    ): ResponseEntity<ByteArray> {
        log.info { "Produserer notat HTML for dokumentmal $dokumentmal" }
        return pdfProducerService.generatePDFResponseV2(
            "vedtak",
            dokumentmal,
            Paths.get("data/vedtak/klage_orkestrering.json").readText(),
        )
    }
}
