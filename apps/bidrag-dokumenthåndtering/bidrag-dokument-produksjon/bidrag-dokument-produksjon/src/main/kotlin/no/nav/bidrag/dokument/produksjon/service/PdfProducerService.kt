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
    private val pdfProduksjonMetrikker: PdfProduksjonMetrikker,
) {
    fun generateHTMLResponseDokument(
        type: String,
        dokumentmal: String,
        payload: String?,
        useHottemplate: Boolean = false,
    ): ResponseEntity<String> {
        if (payload == null && !useHottemplate) {
            throw RuntimeException(
                "Mangler data for å generere brev",
            )
        }
        val jsonPayload = payload ?: hotTemplateData(type, dokumentmal)
        return bidragDokumentmalConsumer
            .hentDokumentmal(
                type,
                dokumentmal,
                jsonPayload,
                false,
            )?.let {
                ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(it)
            }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND).body("Template or type not found")
    }

    fun generateHTMLResponseV2(
        type: String,
        dokumentmal: String,
        payload: String?,
        useHottemplate: Boolean = false,
        innhold: String? = null,
    ): ResponseEntity<String> {
        if (payload == null && !useHottemplate) {
            throw RuntimeException(
                "Mangler data for å generere brev",
            )
        }
        val jsonPayload = payload ?: hotTemplateData(type, dokumentmal)
        return bidragDokumentmalConsumer
            .hentDokumentmal(
                type,
                dokumentmal,
                jsonPayload,
                false,
                innhold,
            )?.let {
                ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(it)
            }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND).body("Template or type not found")
    }

    fun generatePDFResponseDokument(
        type: String,
        dokumentmal: String,
        payload: String?,
        useHottemplate: Boolean = false,
    ): ResponseEntity<ByteArray> = genererPdf(type, dokumentmal, payload, useHottemplate)

    fun generatePDFResponseV2(
        type: String,
        dokumentmal: String,
        payload: String?,
        useHottemplate: Boolean = false,
        innhold: String? = null,
    ): ResponseEntity<ByteArray> = genererPdf(type, dokumentmal, payload, useHottemplate, innhold)

    private fun genererPdf(
        type: String,
        dokumentmal: String,
        payload: String?,
        useHottemplate: Boolean,
        innhold: String? = null,
    ): ResponseEntity<ByteArray> {
        if (payload == null && !useHottemplate) {
            throw RuntimeException(
                "Mangler data for å generere brev",
            )
        }
        val jsonPayload = payload ?: hotTemplateData(type, dokumentmal)
        val startTime = System.currentTimeMillis()
        val metrikktype = listOfNotNull(type, innhold).joinToString("-")
        return pdfProduksjonMetrikker.målSteg(
            PdfProduksjonSteg.TOTALT,
            metrikktype,
            dokumentmal,
            utfallFor = { if (it.statusCode.is2xxSuccessful && it.body != null) PdfProduksjonUtfall.OK else PdfProduksjonUtfall.FEIL },
        ) {
            pdfProduksjonMetrikker
                .målSteg(PdfProduksjonSteg.HENT_HTML, metrikktype, dokumentmal) {
                    bidragDokumentmalConsumer.hentDokumentmal(type, dokumentmal, jsonPayload, innhold = innhold)
                }?.let { document ->
                    val html =
                        pdfProduksjonMetrikker.målSteg(PdfProduksjonSteg.RENS_HTML, metrikktype, dokumentmal) {
                            document.fjernKontrollTegn()
                        }
                    pdfProduksjonMetrikker.registrerStørrelse(
                        PdfProduksjonDokumenttype.HTML,
                        metrikktype,
                        dokumentmal,
                        html.toByteArray(Charsets.UTF_8).size,
                    )
                    val bytes =
                        pdfProduksjonMetrikker.målSteg(PdfProduksjonSteg.KONVERTER_PDF, metrikktype, dokumentmal) {
                            bidragPdfGenConsumer.produserPdf(html, Configuration(BigDecimal(1), false))
                        }
                    bytes?.let {
                        pdfProduksjonMetrikker.registrerStørrelse(PdfProduksjonDokumenttype.PDF, metrikktype, dokumentmal, it.size)
                    }
                    log.info {
                        "Done generating PDF for type $type and dokumentmal $dokumentmal in ${System.currentTimeMillis() - startTime}ms"
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
                    ).body("Template or type not found".toByteArray())
        }
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
    type: String,
    dokumentmal: String,
): String {
    val safeNamePattern = Regex("^[\\w\\-]+$")
    val safeType = safeNamePattern.matchEntire(type)?.value ?: throw IllegalArgumentException("Invalid type: $type")
    val safeDokumentmal = safeNamePattern.matchEntire(dokumentmal)?.value ?: throw IllegalArgumentException("Invalid dokumentmal: $dokumentmal")
    val dataFile = getPath("$safeType/$safeDokumentmal.json")
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
