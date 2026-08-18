package no.nav.bidrag.dokument.journalpost.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.bidrag.dokument.journalpost.consumer.BrevserverConsumer
import no.nav.bidrag.dokument.journalpost.dokument.DokumentConsumer
import no.nav.bidrag.dokument.journalpost.dokument.DokumentTilgangConsumer
import no.nav.bidrag.dokument.journalpost.dto.Brev
import no.nav.bidrag.dokument.journalpost.dto.Dokumentbestilling
import no.nav.bidrag.dokument.journalpost.dto.Dokumenttilgang
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.util.Optional

@DisplayName("DokumentService henting av dokument med brevserver toggle")
class DokumentServiceBrevserverTest {
    private val journalpostService: JournalpostService = mockk(relaxed = true)
    private val dokumentTilgangConsumer: DokumentTilgangConsumer = mockk()
    private val dokumentConsumer: DokumentConsumer = mockk()
    private val brevserverConsumer: BrevserverConsumer = mockk()

    private val dokumentreferanse = "BID-12345"
    private val brevreferanse = "brevref-12345"

    private fun dokumentService(brukBrevserverRest: Boolean) = DokumentService(
        "http://nav.no/brevserverUrl",
        "brevSys",
        journalpostService,
        dokumentTilgangConsumer,
        dokumentConsumer,
        brevserverConsumer,
        brukBrevserverRest,
    )

    private fun stubDokumenttilgang() {
        every { dokumentTilgangConsumer.bestillDokumenttilgang(dokumentreferanse) } returns
            Dokumenttilgang(
                "jactor",
                "passord",
                "token-123",
                "brevSys",
                "frabrevlager",
                Brev(brevreferanse),
            )
    }

    @Test
    @DisplayName("skal hente dokument fra brevserver via REST når toggle er på")
    fun skalHenteDokumentFraBrevserverNarToggleErPa() {
        stubDokumenttilgang()
        every { brevserverConsumer.hentDokument(brevreferanse) } returns "pdf-fra-rest".toByteArray()

        val response = dokumentService(brukBrevserverRest = true).hentDokument(dokumentreferanse)

        response.statusCode.value() shouldBe 200
        response.headers.contentType shouldBe MediaType.APPLICATION_PDF
        response.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION) shouldBe "inline; filename=$dokumentreferanse.pdf"
        String(response.body!!) shouldBe "pdf-fra-rest"

        verify(exactly = 1) { brevserverConsumer.hentDokument(brevreferanse) }
        verify(exactly = 0) { dokumentConsumer.henteDokument(any()) }
    }

    @Test
    @DisplayName("skal hente dokument fra midlertidig brevlager når toggle er av")
    fun skalHenteDokumentFraBrevlagerNarToggleErAv() {
        stubDokumenttilgang()
        every { dokumentConsumer.henteDokument(any<Dokumentbestilling>()) } returns Optional.of("pdf-fra-mq".toByteArray())

        val response = dokumentService(brukBrevserverRest = false).hentDokument(dokumentreferanse)

        String(response.body!!) shouldBe "pdf-fra-mq"

        verify(exactly = 1) { dokumentConsumer.henteDokument(any()) }
        verify(exactly = 0) { brevserverConsumer.hentDokument(any()) }
    }

    @Test
    @DisplayName("skal hente RTF fra midlertidig brevlager selv om toggle er på")
    fun skalAlltidHenteRtfFraBrevlager() {
        stubDokumenttilgang()
        every { dokumentConsumer.henteDokumentRTF(any<Dokumentbestilling>()) } returns Optional.of("rtf".toByteArray())

        val response = dokumentService(brukBrevserverRest = true).hentDokumentRTF(dokumentreferanse)

        response.headers.contentType shouldBe MediaType.APPLICATION_OCTET_STREAM
        String(response.body!!) shouldBe "rtf"

        verify(exactly = 0) { brevserverConsumer.hentDokument(any()) }
    }
}
