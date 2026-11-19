package no.nav.bidrag.dokument.service

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.every
import io.mockk.mockk
import no.nav.bidrag.dokument.consumer.BidragDokumentConsumer
import no.nav.bidrag.dokument.dto.DocumentProperties
import no.nav.bidrag.dokument.dto.DokumentRef
import no.nav.bidrag.transport.dokument.DokumentArkivSystemDto
import no.nav.bidrag.transport.dokument.DokumentFormatDto
import no.nav.bidrag.transport.dokument.DokumentMetadata
import no.nav.bidrag.transport.dokument.DokumentStatusDto
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import java.io.ByteArrayOutputStream

class DokumentServiceMergeFeilTest {
    private val arkiv = mockk<BidragDokumentConsumer>()
    private val brevlager = mockk<BidragDokumentConsumer>()
    private val forsendelse = mockk<BidragDokumentConsumer>()
    private val service = DokumentService(mockk(), arkiv, brevlager, forsendelse)
    private val appender = ListAppender<ILoggingEvent>()
    private val logger = LoggerFactory.getLogger(DokumentService::class.java) as Logger

    @BeforeEach
    fun setUp() {
        appender.start()
        logger.addAppender(appender)
    }

    @AfterEach
    fun tearDown() {
        logger.detachAppender(appender)
    }

    private fun metadata(
        journalpostId: String,
        dokumentreferanse: String,
    ) = DokumentMetadata(
        journalpostId = journalpostId,
        dokumentreferanse = dokumentreferanse,
        format = DokumentFormatDto.PDF,
        status = DokumentStatusDto.FERDIGSTILT,
        arkivsystem = DokumentArkivSystemDto.BIDRAG,
    )

    private fun pdfMedEnSide(): ByteArray = PDDocument().use { dokument ->
        dokument.addPage(PDPage())
        ByteArrayOutputStream().also { dokument.save(it) }.toByteArray()
    }

    private fun gittForsendelseDerHentingAvDokumentFeiler(status: HttpStatus) {
        every { forsendelse.hentDokumentMetadata(any(), any()) } returns listOf(metadata("BIF-1", "D1"))
        every { forsendelse.hentDokument(any(), any()) } throws HttpClientErrorException(status)
    }

    private fun loggedeFeil() = appender.list.filter { it.level == Level.ERROR }

    @ParameterizedTest
    @EnumSource(value = HttpStatus::class, names = ["FORBIDDEN", "NOT_FOUND"])
    fun `sammenslåing av journalposter uten dokumentId svarer med statusen fra feilet henting`(status: HttpStatus) {
        gittForsendelseDerHentingAvDokumentFeiler(status)

        val response = service.hentDokumenter(listOf("BIF-1", "BIF-2"), DocumentProperties())

        assertThat(response.statusCode).isEqualTo(status)
        assertThat(loggedeFeil()).hasSize(1)
        assertThat(loggedeFeil().single().throwableProxy.className).isEqualTo(HttpClientErrorException::class.java.name)
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus::class, names = ["FORBIDDEN", "NOT_FOUND"])
    fun `henting av én journalpost uten dokumentId svarer med statusen fra feilet henting`(status: HttpStatus) {
        gittForsendelseDerHentingAvDokumentFeiler(status)

        val response = service.hentDokument(DokumentRef("BIF-1", null), DocumentProperties())

        assertThat(response.statusCode).isEqualTo(status)
        assertThat(loggedeFeil()).hasSize(1)
    }

    @Test
    fun `sammenslåing av journalposter uten dokumentId gir ett dokument med alle sidene`() {
        every { forsendelse.hentDokumentMetadata("BIF-1", any()) } returns listOf(metadata("BIF-1", "D1"))
        every { forsendelse.hentDokumentMetadata("BIF-2", any()) } returns listOf(metadata("BIF-2", "D2"))
        every { forsendelse.hentDokument(any(), any()) } answers { ResponseEntity.ok(pdfMedEnSide()) }

        val response = service.hentDokumenter(listOf("BIF-1", "BIF-2"), DocumentProperties())

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        Loader.loadPDF(response.body).use { assertThat(it.numberOfPages).isEqualTo(2) }
        assertThat(loggedeFeil()).isEmpty()
    }

    @Test
    fun `sammenslåing der ett dokument mangler innhold svarer 500 og logger hvilket dokument`() {
        every { forsendelse.hentDokumentMetadata(any(), any()) } returns listOf(metadata("BIF-1", "D1"))
        every { forsendelse.hentDokument(any(), any()) } returns ResponseEntity.ok().build()

        val response = service.hentDokumenter(listOf("BIF-1", "BIF-2"), DocumentProperties())

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(loggedeFeil()).hasSize(1)
        assertThat(loggedeFeil().single().throwableProxy.className).isEqualTo(IllegalStateException::class.java.name)
        assertThat(loggedeFeil().single().throwableProxy.message).contains("D1").contains("har ikke innhold")
    }
}
