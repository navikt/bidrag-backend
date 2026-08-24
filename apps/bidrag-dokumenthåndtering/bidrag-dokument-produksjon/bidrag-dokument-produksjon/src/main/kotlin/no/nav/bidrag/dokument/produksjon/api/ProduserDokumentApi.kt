package no.nav.bidrag.dokument.produksjon.api

import com.fasterxml.jackson.databind.JsonNode
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.dokument.produksjon.SIKKER_LOGG
import no.nav.bidrag.dokument.produksjon.service.PdfProducerService
import no.nav.bidrag.dokument.produksjon.util.getObjectmapper
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/dokument")
class ProduserDokumentApi(
    val pdfProducerService: PdfProducerService,
) {
    @PostMapping(
        "/pdf/{category}/{dokumentmal}",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun generatePDF(
        @PathVariable category: String,
        @PathVariable dokumentmal: String,
        @org.springframework.web.bind.annotation.RequestBody payload: JsonNode,
    ): ResponseEntity<ByteArray> {
        log.info { "Produserer notat PDF 2 for dokumentmal $dokumentmal" }
        return pdfProducerService.generatePDFResponseDokument(
            category,
            dokumentmal,
            commonObjectmapper.writeValueAsString(payload),
        )
    }

    @PostMapping(
        "/html/{category}/{dokumentmal}",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun generateHTML(
        @PathVariable category: String,
        @PathVariable dokumentmal: String,
        @org.springframework.web.bind.annotation.RequestBody payload: JsonNode,
    ): ResponseEntity<String> {
        SIKKER_LOGG.info {
            "Produserer notat HTML for dokumentmal $dokumentmal for input $payload"
        }
        log.info { "Produserer notat HTML for dokumentmal $dokumentmal" }
        return pdfProducerService.generateHTMLResponseDokument(
            category,
            dokumentmal,
            getObjectmapper().writeValueAsString(payload),
        )
    }

    @GetMapping(
        "/html/test/{category}/{dokumentmal}",
    )
    fun generateHTMLTest(
        @PathVariable category: String,
        @PathVariable dokumentmal: String,
    ): ResponseEntity<String> {
        SIKKER_LOGG.info {
            "Produserer notat HTML for dokumentmal $dokumentmal for input"
        }
        log.info { "Produserer notat HTML for dokumentmal $dokumentmal" }
        return pdfProducerService.generateHTMLResponseDokument(
            category,
            dokumentmal,
            getObjectmapper().writeValueAsString("{}"),
        )
    }

    @PostMapping(
        "/pdf/test/{category}/{dokumentmal}",
    )
    fun generatePDFTest(
        @PathVariable category: String,
        @PathVariable dokumentmal: String,
        @org.springframework.web.bind.annotation.RequestBody payload: JsonNode,
    ): ResponseEntity<ByteArray> {
        log.info { "Produserer notat PDF 2 for dokumentmal $dokumentmal" }

        return pdfProducerService.generatePDFResponseDokument(
            category,
            dokumentmal,
            commonObjectmapper.writeValueAsString(payload),
        )
    }
}
