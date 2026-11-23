package no.nav.bidrag.behandling.consumer

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.bidrag.behandling.dto.v2.notat.NotatDel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.net.URI

class BidragDokumentProduksjonConsumerTest {
    private val restTemplate = mockk<RestTemplate>()
    private val kaltUri = slot<URI>()
    private val consumer = BidragDokumentProduksjonConsumer(URI("http://dokumentproduksjon"), restTemplate)

    @BeforeEach
    fun init() {
        every {
            restTemplate.exchange(capture(kaltUri), HttpMethod.POST, any<HttpEntity<*>>(), any<ParameterizedTypeReference<ByteArray>>())
        } returns ResponseEntity.ok("PDF".toByteArray())
    }

    @Test
    fun `skal ikke sende del når samlet notat opprettes`() {
        consumer.opprettNotat(mockk(relaxed = true))

        kaltUri.captured.toString() shouldBe "http://dokumentproduksjon/api/v2/notat/pdf"
    }

    @Test
    fun `skal sende del når begrunnelse opprettes`() {
        consumer.opprettNotat(mockk(relaxed = true), NotatDel.BEGRUNNELSE)

        kaltUri.captured.toString() shouldBe "http://dokumentproduksjon/api/v2/notat/pdf?del=BEGRUNNELSE"
    }

    @Test
    fun `skal sende del når opplysninger opprettes`() {
        consumer.opprettNotat(mockk(relaxed = true), NotatDel.OPPLYSNINGER)

        kaltUri.captured.toString() shouldBe "http://dokumentproduksjon/api/v2/notat/pdf?del=OPPLYSNINGER"
    }
}
