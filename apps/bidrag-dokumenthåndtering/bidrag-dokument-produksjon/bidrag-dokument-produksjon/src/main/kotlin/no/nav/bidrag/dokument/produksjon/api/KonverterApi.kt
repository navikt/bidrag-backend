package no.nav.bidrag.dokument.produksjon.api

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.dokument.produksjon.SIKKER_LOGG
import no.nav.bidrag.dokument.produksjon.service.PdfProducerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v2/konverter")
class KonverterApi(
    val pdfProducerService: PdfProducerService,
) {
    @PostMapping("/html")
    fun htmlToPdf(
        @org.springframework.web.bind.annotation.RequestBody payload: ByteArray,
    ): ResponseEntity<ByteArray> {
        SIKKER_LOGG.info {
            "Konverterer HTML til PDF"
        }
        log.info { "Produserer notat HTML for dokumentmal" }
        return pdfProducerService.htmlToPDF(payload)
    }

    @PostMapping("/flatten")
    fun flattenPDF(
        @org.springframework.web.bind.annotation.RequestBody payload: ByteArray,
    ): ResponseEntity<ByteArray> {
        log.info { "Flattening PDF" }
        return pdfProducerService.flattenPDF(payload)
    }
}
