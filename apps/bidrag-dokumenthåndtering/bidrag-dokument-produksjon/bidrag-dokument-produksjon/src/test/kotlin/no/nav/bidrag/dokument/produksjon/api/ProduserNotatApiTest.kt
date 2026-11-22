package no.nav.bidrag.dokument.produksjon.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.dokument.produksjon.service.PdfProducerService
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.nio.file.Paths
import kotlin.io.path.readText

private const val KATEGORI = "notat"

class ProduserNotatApiTest {
    private lateinit var pdfProducerService: PdfProducerService
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        pdfProducerService = mockk()
        every { pdfProducerService.generatePDFResponseV2(any(), any(), any(), any(), any()) } returns ResponseEntity.ok(byteArrayOf(1))
        every { pdfProducerService.generateHTMLResponseV2(any(), any(), any(), any(), any()) } returns ResponseEntity.ok("<html/>")
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(ProduserNotatApi(pdfProducerService))
                .setMessageConverters(MappingJackson2HttpMessageConverter(commonObjectmapper))
                .build()
    }

    private fun eksempel(navn: String) = Paths.get("data/notat/$navn.json").readText()

    @Test
    fun `skal produsere komplett notat når del mangler`() {
        // When
        mockMvc
            .post("/api/v2/notat/pdf") {
                contentType = MediaType.APPLICATION_JSON
                content = eksempel("forskudd_innvilget")
            }.andExpect { status { isOk() } }

        // Then
        verify(exactly = 1) { pdfProducerService.generatePDFResponseV2(KATEGORI, "forskudd", any(), false, null) }
    }

    @ParameterizedTest
    @CsvSource(
        "forskudd_innvilget, KOMPLETT, forskudd,",
        "forskudd_innvilget, BEGRUNNELSE, forskudd, begrunnelse",
        "saerbidrag, OPPLYSNINGER, særbidrag, opplysninger",
        "bidrag, BEGRUNNELSE, bidrag, begrunnelse",
        "bidrag, OPPLYSNINGER, bidrag, opplysninger",
    )
    fun `skal produsere riktig del av notatet som PDF`(
        eksempel: String,
        del: String,
        dokumentmal: String,
        innhold: String?,
    ) {
        // When
        mockMvc
            .post("/api/v2/notat/pdf?del=$del") {
                contentType = MediaType.APPLICATION_JSON
                content = eksempel(eksempel)
            }.andExpect { status { isOk() } }

        // Then
        verify(exactly = 1) { pdfProducerService.generatePDFResponseV2(KATEGORI, dokumentmal, any(), false, innhold) }
    }

    @Test
    fun `skal produsere riktig del av notatet som HTML`() {
        // When
        mockMvc
            .post("/api/v2/notat/html?del=OPPLYSNINGER") {
                contentType = MediaType.APPLICATION_JSON
                content = eksempel("bidrag")
            }.andExpect { status { isOk() } }

        // Then
        verify(exactly = 1) { pdfProducerService.generateHTMLResponseV2(KATEGORI, "bidrag", any(), false, "opplysninger") }
    }

    @Test
    fun `skal avvise ukjent del`() {
        mockMvc
            .post("/api/v2/notat/pdf?del=UKJENT") {
                contentType = MediaType.APPLICATION_JSON
                content = eksempel("forskudd_innvilget")
            }.andExpect { status { isBadRequest() } }

        verify(exactly = 0) { pdfProducerService.generatePDFResponseV2(any(), any(), any(), any(), any()) }
    }
}
