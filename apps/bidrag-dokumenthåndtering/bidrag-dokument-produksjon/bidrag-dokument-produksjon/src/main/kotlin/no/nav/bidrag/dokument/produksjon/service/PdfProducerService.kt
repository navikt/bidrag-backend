package no.nav.bidrag.dokument.produksjon.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.dokument.produksjon.consumer.BidragDokumentmalConsumer
import no.nav.bidrag.dokument.produksjon.consumer.BidragPdfGenConsumer
import no.nav.bidrag.dokument.produksjon.consumer.Configuration
import no.nav.bidrag.transport.dokumentmaler.notat.VedtakNotatDto
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.pathString

private val log = KotlinLogging.logger {}

@Service
class PdfProducerService(
    private val bidragDokumentmalConsumer: BidragDokumentmalConsumer,
    private val bidragPdfGenConsumer: BidragPdfGenConsumer,
) {
    fun generateHTMLResponseDokument(
        category: String,
        dokumentmal: String,
        payload: String?,
        useHottemplate: Boolean = false,
    ): ResponseEntity<String> {
        if (payload == null && !useHottemplate) {
            throw RuntimeException(
                "Mangler data for å generere brev",
            )
        }
        val jsonPayload = payload ?: hotTemplateData(category, dokumentmal)
        return bidragDokumentmalConsumer
            .hentDokumentmal(
                category,
                dokumentmal,
                jsonPayload,
                false,
            )?.let {
                ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(it)
            }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND).body("Template or category not found")
    }

    fun generateHTMLResponseV2(
        category: String,
        dokumentmal: String,
        payload: String?,
        useHottemplate: Boolean = false,
    ): ResponseEntity<String> {
        if (payload == null && !useHottemplate) {
            throw RuntimeException(
                "Mangler data for å generere brev",
            )
        }
        val jsonPayload = payload ?: hotTemplateData(category, dokumentmal)
        return bidragDokumentmalConsumer
            .hentDokumentmal(
                category,
                dokumentmal,
                jsonPayload,
                false,
            )?.let {
                ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(it)
            }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND).body("Template or category not found")
    }

    fun generatePDFResponseDokument(
        category: String,
        dokumentmal: String,
        payload: String?,
        useHottemplate: Boolean = false,
    ): ResponseEntity<ByteArray> {
        if (payload == null && !useHottemplate) {
            throw RuntimeException(
                "Mangler data for å generere brev",
            )
        }
        val jsonPayload = payload ?: hotTemplateData(category, dokumentmal)
        val startTime = System.currentTimeMillis()
        return bidragDokumentmalConsumer
            .hentDokumentmal(category, dokumentmal, jsonPayload)
            ?.let { document ->
                val bytes =
                    bidragPdfGenConsumer.produserPdf(
                        document.fjernKontrollTegn(),
                        Configuration(BigDecimal(1), false),
                    )
                log.info {
                    "Done generating PDF for category $category and template $dokumentmal in ${System.currentTimeMillis() - startTime}ms"
                }
                ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=dokumenter_sammenslatt.pdf",
                    ).body(bytes)
            }
            ?: ResponseEntity
                .status(
                    HttpStatus.NOT_FOUND,
                ).body("Template or category not found".toByteArray())
    }

    fun generatePDFResponseV2(
        category: String,
        dokumentmal: String,
        payload: String?,
        useHottemplate: Boolean = false,
    ): ResponseEntity<ByteArray> {
        if (payload == null && !useHottemplate) {
            throw RuntimeException(
                "Mangler data for å generere brev",
            )
        }
        val jsonPayload = payload ?: hotTemplateData(category, dokumentmal)
        val startTime = System.currentTimeMillis()
        return bidragDokumentmalConsumer
            .hentDokumentmal(category, dokumentmal, jsonPayload)
            ?.let { document ->
                val bytes =
                    bidragPdfGenConsumer.produserPdf(
                        document.fjernKontrollTegn(),
                        Configuration(BigDecimal(1), false),
                    )
                log.info {
                    "Done generating PDF for category $category and template $dokumentmal in ${System.currentTimeMillis() - startTime}ms"
                }
                ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=dokumenter_sammenslatt.pdf",
                    ).body(bytes)
            }
            ?: ResponseEntity
                .status(
                    HttpStatus.NOT_FOUND,
                ).body("Template or category not found".toByteArray())
    }

    fun flattenPDF(payload: ByteArray): ResponseEntity<ByteArray> {
        val bytes = bidragPdfGenConsumer.flattenPDF(payload)

        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=flattened_pdf.pdf",
            ).body(bytes)
    }

    fun htmlToPDF(payload: ByteArray): ResponseEntity<ByteArray> {
        val bytes =
            bidragPdfGenConsumer.produserPdf(
                payload.toString(Charsets.UTF_8),
                Configuration(BigDecimal(1), false),
            )

        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=dokumenter_sammenslatt.pdf",
            ).body(bytes)
    }
}

fun String.fjernKontrollTegn() = this.replace("\\p{C}".toRegex(), "")

fun hotTemplateData(
    foldername: String,
    template: String,
): String {
    val dataFile = getPath("$foldername/$template.json")
    val data =
        commonObjectmapper.readValue(
            if (Files.exists(dataFile)) {
                Files.readAllBytes(dataFile)
            } else {
                "{}".toByteArray(Charsets.UTF_8)
            },
            VedtakNotatDto::class.java,
        )
    return commonObjectmapper.writeValueAsString(data)
}

fun getPath(filename: String): Path {
    val filePath = filename.let { Paths.get("data/").resolve(it) }
    log.trace { "Reading file from path $filePath. File exists on path = ${filePath.exists()}" }
    return if (filePath.exists()) {
        filePath
    } else {
        PdfProducerService::class.java.classLoader.getResource(filePath.pathString)?.let {
            Path.of(it.toURI())
        }
            ?: throw RuntimeException("Could not find file at path $filePath")
    }
}
