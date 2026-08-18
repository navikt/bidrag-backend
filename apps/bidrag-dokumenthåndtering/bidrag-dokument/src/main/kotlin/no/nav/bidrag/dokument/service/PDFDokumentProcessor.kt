package no.nav.bidrag.dokument.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.bidrag.dokument.dto.DocumentProperties
import org.apache.pdfbox.Loader
import org.apache.pdfbox.io.IOUtils
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.pdfwriter.compress.CompressParameters
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.util.Matrix
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Optional
import java.util.UUID
import kotlin.math.abs

private val log = KotlinLogging.logger {}

class PDFDokumentProcessor {
    private var document: PDDocument? = null
    private lateinit var originalDocument: ByteArray
    private var documentProperties: DocumentProperties? = null

    fun process(
        dokumentFil: ByteArray,
        documentProperties: DocumentProperties,
        withCompression: Boolean = true,
    ): ByteArray {
        val tracer = GlobalOpenTelemetry.getTracer(javaClass.canonicalName)

        val span =
            tracer
                .spanBuilder("process")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("dokumentFil.size", dokumentFil.size.toLong())
                .setAttribute("withCompression", withCompression)
                .setDocumentProperties(documentProperties)
                .startSpan()

        try {
            val res = processWithSpan(span, dokumentFil, documentProperties, withCompression, numRecursiveCalls = 0)
            span.setAttribute("result.size", res.size.toLong())
            return res
        } catch (e: Exception) {
            span.setStatus(StatusCode.ERROR, "${e.javaClass.simpleName}: ${e.message}")
            throw e
        } finally {
            span.end()
        }
    }

    private fun processWithSpan(
        span: Span,
        dokumentFil: ByteArray,
        documentProperties: DocumentProperties,
        withCompression: Boolean,
        numRecursiveCalls: Long,
    ): ByteArray {
        span.setAttribute("numRecursiveCalls", numRecursiveCalls)

        if (!documentProperties.shouldProcess()) {
            return dokumentFil
        }
        originalDocument = dokumentFil
        this.documentProperties = documentProperties
        val documentByteStream = ByteArrayOutputStream()
        try {
            loadPdf(dokumentFil).use { document ->
                document?.let { span.setAttribute("document.numberOfPages", document.numberOfPages.toLong()) }

                this.document = document
                if (documentProperties.resizeToA4()) {
                    konverterAlleSiderTilA4()
                }
                if (documentProperties.optimizeForPrint()) {
                    optimaliserForDobbelsidigPrinting()
                }
                val compression =
                    if (withCompression) CompressParameters.DEFAULT_COMPRESSION else CompressParameters.NO_COMPRESSION
                this.document?.isAllSecurityToBeRemoved = true

                log.info { "Lagrer dokument med kompresjon ${compression.isCompress}" }
                saveDocument(documentByteStream, compression)

                this.document?.close()
                return documentByteStream.toByteArray()
            }
        } catch (e: Exception) {
            log.error(e) { "Det skjedde en feil ved prossesering av PDF dokument" }
            return dokumentFil
        } catch (e: StackOverflowError) {
            log.error(e) { "Det skjedde en feil ved lagring av dokument med kompresjon. Forsøker å lagree dokument uten kompresjon" }
            return if (withCompression) {
                processWithSpan(span, dokumentFil, documentProperties, false, numRecursiveCalls = numRecursiveCalls + 1)
            } else {
                dokumentFil
            }
        } finally {
            IOUtils.closeQuietly(documentByteStream)
        }
    }

    @WithSpan
    private fun saveDocument(
        documentByteStream: ByteArrayOutputStream,
        compression: CompressParameters?,
    ) {
        this.document?.save(documentByteStream, compression)
    }

    @WithSpan
    private fun loadPdf(dokumentFil: ByteArray): PDDocument? = Loader
        .loadPDF(
            RandomAccessReadBuffer(dokumentFil),
            MemoryUsageSetting.setupTempFileOnly().streamCache,
        )

    @WithSpan
    fun optimaliserForDobbelsidigPrinting() {
        if (documentHasOddNumberOfPages() && documentProperties!!.hasMoreThanOneDocument() && !documentProperties!!.isLastDocument()) {
            log.debug { "Dokumentet har oddetall antall sider. Legger til en blank side på slutten av dokumentet." }
            document?.addPage(PDPage(PDRectangle.A4))
        }
    }

    private fun documentHasOddNumberOfPages(): Boolean = document != null && document!!.numberOfPages % 2 != 0

    private fun isPageSizeA4(pdPage: PDPage): Boolean {
        val a4PageMediaBox = PDRectangle.A4
        val pageMediaBox = pdPage.mediaBox
        val hasSameHeightAndWidth =
            isSameWithMargin(pageMediaBox.height, a4PageMediaBox.height, 1f) &&
                isSameWithMargin(
                    pageMediaBox.width,
                    a4PageMediaBox.width,
                    1f,
                )
        val hasSameHeightAndWidthRotated =
            isSameWithMargin(pageMediaBox.width, a4PageMediaBox.height, 1f) &&
                isSameWithMargin(
                    pageMediaBox.height,
                    a4PageMediaBox.width,
                    1f,
                )
        return hasSameHeightAndWidth || hasSameHeightAndWidthRotated
    }

    @WithSpan
    @Throws(IOException::class)
    private fun konverterAlleSiderTilA4() {
        log.debug {
            "Konverterer ${document!!.numberOfPages} sider til A4 størrelse. Filstørrelse ${
                bytesIntoHumanReadable(
                    originalDocument.size.toLong(),
                )
            }"
        }
        for (pageNumber in 0 until document!!.numberOfPages) {
            val page = document!!.getPage(pageNumber)
            updatePageRotationToVertical(page)
            if (!isPageSizeA4(page)) {
                convertPageToA4(page)
            }
        }
    }

    private fun updatePageRotationToVertical(page: PDPage) {
        if (isVertical(page) && page.rotation != 0) {
            page.rotation = 0
        } else if (isHorizontal(page) && page.rotation == 0 && !isPageSizeA4(page)) {
            page.rotation = 90
        }
    }

    private fun isHorizontal(page: PDPage): Boolean = !isVertical(page)

    /*
     En side skal roteres til å være vertikalt kun hvis siden er dimensjonert slik at høyden > bredden. Ellers skal det ignoreres
     */
    private fun isVertical(page: PDPage): Boolean = Optional
        .ofNullable(page.mediaBox)
        .map { mediaBox: PDRectangle -> mediaBox.height > mediaBox.width }
        .orElse(false)

    @Throws(IOException::class)
    private fun convertPageToA4(page: PDPage) {
        val matrix = Matrix()
        val xScale = PDRectangle.A4.width / page.mediaBox.width
        val yScale = PDRectangle.A4.height / page.mediaBox.height
        matrix.scale(xScale, yScale)
        PDPageContentStream(
            document,
            page,
            AppendMode.PREPEND,
            false,
        ).use { contentStream -> contentStream.transform(matrix) }
        page.mediaBox = PDRectangle.A4
        page.cropBox = PDRectangle.A4
    }

    private fun isSameWithMargin(
        val1: Float,
        val2: Float,
        margin: Float,
    ): Boolean = abs(val1 - val2) < margin

    companion object {
        @Throws(IOException::class)
        fun fileToByte(file: File): ByteArray {
            val inputStream = FileInputStream(file)
            val byteArray = ByteArray(file.length().toInt())
            inputStream.read(byteArray)
            inputStream.close()
            return byteArray
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun bytesIntoHumanReadable(bytes: Long): String {
            val kilobyte: Long = 1024
            val megabyte = kilobyte * 1024
            val gigabyte = megabyte * 1024
            val terabyte = gigabyte * 1024
            return if (bytes in 0..<kilobyte) {
                "$bytes B"
            } else if (bytes in kilobyte..<megabyte) {
                (bytes / kilobyte).toString() + " KB"
            } else if (bytes in megabyte..<gigabyte) {
                (bytes / megabyte).toString() + " MB"
            } else if (bytes in gigabyte..<terabyte) {
                (bytes / gigabyte).toString() + " GB"
            } else if (bytes >= terabyte) {
                (bytes / terabyte).toString() + " TB"
            } else {
                "$bytes Bytes"
            }
        }
    }
}

class PDFDokumentMerger {
    companion object {
        fun merge(
            dokumentBytes: List<ByteArray>,
            documentProperties: DocumentProperties,
            withCompression: Boolean = true,
        ): ByteArray {
            val tracer = GlobalOpenTelemetry.getTracer(javaClass.canonicalName)

            val span =
                tracer
                    .spanBuilder("merge")
                    .setSpanKind(SpanKind.INTERNAL)
                    .setAttribute("input.numDocuments", dokumentBytes.size.toLong())
                    .setAttribute("input.numDocuments.totalSize", dokumentBytes.sumOf { it.size }.toLong())
                    .setAttribute("withCompression", withCompression)
                    .setDocumentProperties(documentProperties)
                    .startSpan()

            try {
                val res = mergeWithSpan(dokumentBytes, documentProperties, withCompression)
                span.setAttribute("result.size", res.size.toLong())
                return res
            } catch (e: Exception) {
                span.setStatus(StatusCode.ERROR, "${e.javaClass.simpleName}: ${e.message}")
                throw e
            } finally {
                span.end()
            }
        }

        private fun mergeWithSpan(
            dokumentBytes: List<ByteArray>,
            documentProperties: DocumentProperties,
            withCompression: Boolean,
        ): ByteArray {
            documentProperties.numberOfDocuments = dokumentBytes.size
            if (dokumentBytes.size == 1) {
                return dokumentBytes[0]
            }
            val tempfiles = mutableListOf<File>()
            try {
                val mergedFileName = "/tmp/" + UUID.randomUUID()
                val mergedDocument = PDFMergerUtility()
                mergedDocument.destinationFileName = mergedFileName
                for (dokument in dokumentBytes) {
                    val tempFile = File.createTempFile("/tmp/" + UUID.randomUUID(), null)
                    tempFile.appendBytes(dokument)
                    tempfiles.add(tempFile)
                    mergedDocument.addSource(tempFile)
                }
                val compression =
                    if (withCompression) {
                        CompressParameters.DEFAULT_COMPRESSION
                    } else {
                        CompressParameters.NO_COMPRESSION
                    }
                log.info { "Lagrer merget dokumenter med kompresjon ${compression.isCompress}" }
                mergeDocuments(mergedDocument, compression)
                return getByteDataAndDeleteFile(mergedFileName)
            } catch (e: StackOverflowError) {
                log.error(e) { "Det skjedde en feil ved merging av dokumenter. Forsøker å merge dokumenter uten kompresjon i legacy modus" }
                return mergeLegacy(dokumentBytes)
            } catch (e: IOException) {
                log.error(
                    e,
                ) { "Det skjedde en IOException ved merging av dokumenter (trolig ugyldig PDF-strukturtre). Forsøker legacy modus" }
                return mergeLegacy(dokumentBytes)
            } finally {
                tempfiles.forEach { it.delete() }
            }
        }

        @WithSpan
        private fun mergeDocuments(
            mergedDocument: PDFMergerUtility,
            compression: CompressParameters?,
        ) {
            mergedDocument.mergeDocuments(
                MemoryUsageSetting.setupTempFileOnly().streamCache,
                compression,
            )
        }

        @WithSpan
        private fun mergeLegacy(dokumentBytes: List<ByteArray>): ByteArray {
            if (dokumentBytes.size == 1) {
                return dokumentBytes[0]
            }
            val mergedDocument = PDDocument()
            val mergerUtility = PDFMergerUtility()
            val documentByteStream = ByteArrayOutputStream()
            log.info { "Merger ${dokumentBytes.size} dokumenter via legacy modus" }

            try {
                loadPDFs(dokumentBytes, mergerUtility, mergedDocument)
                savePDF(mergedDocument, documentByteStream)
                return documentByteStream.toByteArray()
            } catch (e: StackOverflowError) {
                log.error(e) { "Det skjedde en feil ved merging av dokumenter i legacy modus." }
                throw e
            } catch (e: Exception) {
                log.error(e) { "Det skjedde en feil ved merging av dokumenter i legacy modus." }
                throw e
            } finally {
                IOUtils.closeQuietly(documentByteStream)
            }
        }

        @WithSpan
        private fun savePDF(
            mergedDocument: PDDocument,
            documentByteStream: ByteArrayOutputStream,
        ) {
            mergedDocument.save(documentByteStream, CompressParameters.NO_COMPRESSION)
        }

        @WithSpan
        private fun loadPDFs(
            dokumentBytes: List<ByteArray>,
            mergerUtility: PDFMergerUtility,
            mergedDocument: PDDocument,
        ) {
            dokumentBytes.forEach {
                Loader
                    .loadPDF(
                        RandomAccessReadBuffer(it),
                        MemoryUsageSetting.setupTempFileOnly().streamCache,
                    ).use { document ->
                        // Strip tagged PDF structure tree to avoid PDFBox failing on complex
                        // table/accessibility attributes (IOException: Expected string, found COSDictionary)
                        document.documentCatalog.structureTreeRoot = null
                        mergerUtility.appendDocument(mergedDocument, document)
                        document.close()
                    }
            }
        }

        private fun getByteDataAndDeleteFile(filename: String): ByteArray {
            val file = File(filename)
            return try {
                PDFDokumentProcessor.fileToByte(File(filename))
            } finally {
                file.delete()
            }
        }
    }
}

private fun SpanBuilder.setDocumentProperties(documentProperties: DocumentProperties): SpanBuilder = this
    .setAttribute("documentProperties.resizeToA4", documentProperties.resizeToA4())
    .setAttribute("documentProperties.optimizeForPrint", documentProperties.optimizeForPrint())
    .setAttribute("documentProperties.shouldProcess", documentProperties.shouldProcess())
