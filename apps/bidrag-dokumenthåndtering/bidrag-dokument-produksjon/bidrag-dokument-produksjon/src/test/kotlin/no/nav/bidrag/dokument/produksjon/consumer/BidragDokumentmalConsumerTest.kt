package no.nav.bidrag.dokument.produksjon.consumer

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

private const val URL = "http://dokumentmal"
private const val HTML = "<html/>"

class BidragDokumentmalConsumerTest {
    private val servere = mutableListOf<MockRestServiceServer>()
    private lateinit var consumer: BidragDokumentmalConsumer

    // Klienten med renderforpdf = true bygges først.
    private val serverForPdf get() = servere.first()

    @BeforeEach
    fun setup() {
        servere.clear()
        val builder = RestTemplateBuilder({ servere += MockRestServiceServer.bindTo(it).build() })
        consumer = BidragDokumentmalConsumer(URL, builder)
    }

    private fun forventKall(sti: String) = serverForPdf
        .expect(requestTo("$URL$sti"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(HTML, MediaType.TEXT_HTML))

    @Test
    fun `skal hente komplett notat fra notat og type`() {
        // Given
        forventKall("/notat/bidrag")

        // When
        val html = consumer.hentDokumentmal("notat", "bidrag", "{}")

        // Then
        html shouldBe HTML
        serverForPdf.verify()
    }

    @Test
    fun `skal hente begrunnelsen fra egen sti`() {
        // Given
        forventKall("/notat/begrunnelse/forskudd")

        // When
        val html = consumer.hentDokumentmal("notat", "forskudd", "{}", innhold = "begrunnelse")

        // Then
        html shouldBe HTML
        serverForPdf.verify()
    }

    @Test
    fun `skal hente opplysningene fra egen sti med kodet særbidrag`() {
        // Given
        forventKall("/notat/opplysninger/s%C3%A6rbidrag")

        // When
        val html = consumer.hentDokumentmal("notat", "særbidrag", "{}", innhold = "opplysninger")

        // Then
        html shouldBe HTML
        serverForPdf.verify()
    }
}
