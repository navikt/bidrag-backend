package no.nav.bidrag.dokument.produksjon.service

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.dokument.produksjon.consumer.BidragDokumentmalConsumer
import no.nav.bidrag.dokument.produksjon.consumer.BidragPdfGenConsumer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus

private const val KATEGORI = "notat"
private const val DOKUMENTMAL = "særbidrag"
private const val PAYLOAD = "{}"
private const val HTML_MED_KONTROLLTEGN = "<html>a\u0000b</html>"
private const val RENSET_HTML = "<html>ab</html>"
private val PDF = byteArrayOf(1, 2, 3, 4, 5)

class PdfProducerServiceTest {
    private lateinit var meterRegistry: SimpleMeterRegistry
    private lateinit var dokumentmalConsumer: BidragDokumentmalConsumer
    private lateinit var pdfGenConsumer: BidragPdfGenConsumer
    private lateinit var service: PdfProducerService

    @BeforeEach
    fun setup() {
        meterRegistry = SimpleMeterRegistry()
        dokumentmalConsumer = mockk()
        pdfGenConsumer = mockk()
        service = PdfProducerService(dokumentmalConsumer, pdfGenConsumer, PdfProduksjonMetrikker(meterRegistry))
    }

    private fun timer(
        steg: PdfProduksjonSteg,
        utfall: PdfProduksjonUtfall,
    ): Timer? = meterRegistry
        .find(METRIKK_PDF_PRODUKSJON)
        .tags(TAG_STEG, steg.tagverdi, TAG_KATEGORI, KATEGORI, TAG_DOKUMENTMAL, DOKUMENTMAL, TAG_UTFALL, utfall.tagverdi)
        .timer()

    private fun størrelse(dokumenttype: PdfProduksjonDokumenttype) = meterRegistry
        .find(METRIKK_PDF_PRODUKSJON_STØRRELSE)
        .tags(TAG_DOKUMENTTYPE, dokumenttype.tagverdi, TAG_KATEGORI, KATEGORI, TAG_DOKUMENTMAL, DOKUMENTMAL)
        .summary()

    @Test
    fun `skal måle hvert steg og størrelser når PDF produseres`() {
        // Given
        every { dokumentmalConsumer.hentDokumentmal(KATEGORI, DOKUMENTMAL, PAYLOAD) } returns HTML_MED_KONTROLLTEGN
        every { pdfGenConsumer.produserPdf(RENSET_HTML, any()) } returns PDF

        // When
        val respons = service.generatePDFResponseV2(KATEGORI, DOKUMENTMAL, PAYLOAD)

        // Then
        assertSoftly {
            respons.statusCode shouldBe HttpStatus.OK
            respons.body shouldBe PDF
            PdfProduksjonSteg.entries.forEach { steg ->
                timer(steg, PdfProduksjonUtfall.OK).shouldNotBeNull().count() shouldBe 1L
            }
            størrelse(PdfProduksjonDokumenttype.HTML).shouldNotBeNull().totalAmount() shouldBe RENSET_HTML.length.toDouble()
            størrelse(PdfProduksjonDokumenttype.PDF).shouldNotBeNull().totalAmount() shouldBe PDF.size.toDouble()
        }
        verify(exactly = 1) { pdfGenConsumer.produserPdf(RENSET_HTML, any()) }
    }

    @Test
    fun `skal registrere feil på henting og totalt når dokumentmal ikke returnerer HTML`() {
        // Given
        every { dokumentmalConsumer.hentDokumentmal(KATEGORI, DOKUMENTMAL, PAYLOAD) } returns null

        // When
        val respons = service.generatePDFResponseDokument(KATEGORI, DOKUMENTMAL, PAYLOAD)

        // Then
        assertSoftly {
            respons.statusCode shouldBe HttpStatus.NOT_FOUND
            timer(PdfProduksjonSteg.HENT_HTML, PdfProduksjonUtfall.FEIL).shouldNotBeNull().count() shouldBe 1L
            timer(PdfProduksjonSteg.TOTALT, PdfProduksjonUtfall.FEIL).shouldNotBeNull().count() shouldBe 1L
            timer(PdfProduksjonSteg.KONVERTER_PDF, PdfProduksjonUtfall.OK).shouldBeNull()
            timer(PdfProduksjonSteg.KONVERTER_PDF, PdfProduksjonUtfall.FEIL).shouldBeNull()
        }
        verify(exactly = 0) { pdfGenConsumer.produserPdf(any(), any()) }
    }

    @Test
    fun `skal registrere feil på konvertering og totalt når pdfgen ikke returnerer PDF`() {
        // Given
        every { dokumentmalConsumer.hentDokumentmal(KATEGORI, DOKUMENTMAL, PAYLOAD) } returns RENSET_HTML
        every { pdfGenConsumer.produserPdf(RENSET_HTML, any()) } returns null

        // When
        val respons = service.generatePDFResponseV2(KATEGORI, DOKUMENTMAL, PAYLOAD)

        // Then
        assertSoftly {
            respons.statusCode shouldBe HttpStatus.OK
            respons.body.shouldBeNull()
            timer(PdfProduksjonSteg.HENT_HTML, PdfProduksjonUtfall.OK).shouldNotBeNull().count() shouldBe 1L
            timer(PdfProduksjonSteg.KONVERTER_PDF, PdfProduksjonUtfall.FEIL).shouldNotBeNull().count() shouldBe 1L
            timer(PdfProduksjonSteg.TOTALT, PdfProduksjonUtfall.FEIL).shouldNotBeNull().count() shouldBe 1L
            størrelse(PdfProduksjonDokumenttype.PDF).shouldBeNull()
        }
    }

    @Test
    fun `skal registrere feil og kaste videre når dokumentmal kaster exception`() {
        // Given
        every { dokumentmalConsumer.hentDokumentmal(KATEGORI, DOKUMENTMAL, PAYLOAD) } throws IllegalStateException("nede")

        // When
        assertThrows<IllegalStateException> { service.generatePDFResponseV2(KATEGORI, DOKUMENTMAL, PAYLOAD) }

        // Then
        assertSoftly {
            timer(PdfProduksjonSteg.HENT_HTML, PdfProduksjonUtfall.FEIL).shouldNotBeNull().count() shouldBe 1L
            timer(PdfProduksjonSteg.TOTALT, PdfProduksjonUtfall.FEIL).shouldNotBeNull().count() shouldBe 1L
        }
    }

    @Test
    fun `skal normalisere kategori med mellomrom og store bokstaver i tag`() {
        // Given
        every { dokumentmalConsumer.hentDokumentmal("Notat ", DOKUMENTMAL, PAYLOAD) } returns RENSET_HTML
        every { pdfGenConsumer.produserPdf(RENSET_HTML, any()) } returns PDF

        // When
        service.generatePDFResponseV2("Notat ", DOKUMENTMAL, PAYLOAD)

        // Then
        timer(PdfProduksjonSteg.TOTALT, PdfProduksjonUtfall.OK).shouldNotBeNull().count() shouldBe 1L
    }

    @Test
    fun `skal kaste exception uten å registrere metrikk når payload mangler`() {
        // When
        assertThrows<RuntimeException> { service.generatePDFResponseV2(KATEGORI, DOKUMENTMAL, null) }

        // Then
        meterRegistry.find(METRIKK_PDF_PRODUKSJON).timers() shouldBe emptyList()
    }

    @Test
    fun `skal erstatte ugyldige tagverdier så antall tidsserier er avgrenset`() {
        assertSoftly {
            tagverdi("særbidrag") shouldBe "særbidrag"
            tagverdi("vedtak ") shouldBe "vedtak"
            tagverdi("../etc/passwd") shouldBe UGYLDIG_TAGVERDI
            tagverdi("a".repeat(65)) shouldBe UGYLDIG_TAGVERDI
            tagverdi("") shouldBe UGYLDIG_TAGVERDI
        }
    }
}
