package no.nav.bidrag.dokument.produksjon.api

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.models.examples.Example
import no.nav.bidrag.dokument.produksjon.SIKKER_LOGG
import no.nav.bidrag.dokument.produksjon.service.PdfProducerService
import no.nav.bidrag.dokument.produksjon.util.genererFødselsnummer
import no.nav.bidrag.dokument.produksjon.util.getObjectmapper
import no.nav.bidrag.transport.dokumentmaler.notat.VedtakNotatDto
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.context.annotation.Bean
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Paths
import kotlin.io.path.readText

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v2/notat")
class ProduserNotatApi(
    val pdfProducerService: PdfProducerService,
) {

    // Fyller inn genererte identer i eksempelfilen.
    private fun fyllInnGenererteIdenter(json: String): String = Regex("PERSONIDENT_\\d+")
        .findAll(json)
        .map { it.value }
        .distinct()
        .fold(json) { oppdatertJson, token -> oppdatertJson.replace(token, genererFødselsnummer()) }

    @PostMapping("/pdf")
    fun generatePDF(
        @org.springframework.web.bind.annotation.RequestBody payload: VedtakNotatDto,
    ): ResponseEntity<ByteArray> {
        log.info { "Produserer notat PDF 2 for dokumentmal ${payload.type}" }
        return pdfProducerService.generatePDFResponseV2(
            "notat",
            payload.type.name.lowercase(),
            commonObjectmapper.writeValueAsString(payload),
        )
    }

    @PostMapping("/html")
    fun generateHTML(
        @Parameter(name = "dokumentmal", example = "forskudd")
        @org.springframework.web.bind.annotation.RequestBody payload: VedtakNotatDto,
    ): ResponseEntity<String> {
        SIKKER_LOGG.info {
            "Produserer notat HTML for dokumentmal ${payload.type} for input $payload"
        }
        log.info { "Produserer notat HTML for dokumentmal ${payload.type}" }
        return pdfProducerService.generateHTMLResponseV2(
            "notat",
            payload.type.name.lowercase(),
            getObjectmapper().writeValueAsString(payload),
        )
    }
}
