package no.nav.bidrag.dokument.journalpost.controller

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.junit5.MockKExtension
import no.nav.bidrag.dokument.journalpost.dokument.DokumentConsumer
import no.nav.bidrag.dokument.journalpost.dokument.DokumentTilgangConsumer
import no.nav.bidrag.dokument.journalpost.dto.Brev
import no.nav.bidrag.dokument.journalpost.dto.Dokumenttilgang
import no.nav.bidrag.dokument.journalpost.entity.JournalpostBygger
import no.nav.bidrag.dokument.journalpost.model.Journalstatus
import no.nav.bidrag.dokument.journalpost.service.JournalpostService
import no.nav.bidrag.transport.dokument.DokumentArkivSystemDto
import no.nav.bidrag.transport.dokument.DokumentFormatDto
import no.nav.bidrag.transport.dokument.DokumentMetadata
import no.nav.bidrag.transport.dokument.DokumentStatusDto
import no.nav.bidrag.transport.dokument.DokumentTilgangResponse
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.resttestclient.exchange
import org.springframework.boot.resttestclient.getForEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Optional

@ExtendWith(MockKExtension::class)
internal class DokumentControllerTest : AbstractControllerTest() {
    @MockkBean
    private val dokumentTilgangConsumer: DokumentTilgangConsumer? = null

    @MockkBean
    lateinit var dokumentConsumer: DokumentConsumer

    @MockkBean
    private val journalpostService: JournalpostService? = null

    @Test
    @DisplayName("skal lage dokumenturl dto med tilgang til dokument")
    fun skalLageDokumentUrlDto() {
        every { dokumentTilgangConsumer!!.bestillDokumenttilgang(any()) } returns Dokumenttilgang(null, null, "urlMedTilgangToken", null, null, null)
        val dokUrlResponse =
            httpHeaderTestRestTemplate.getForEntity<DokumentTilgangResponse>(
                tilgangUrl() + "/journalpostId/dokumentreferanse",
            )
        Assertions.assertThat(dokUrlResponse.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
        val dokumentUrl = dokUrlResponse.body
        Assertions
            .assertThat(dokumentUrl)
            .extracting { it!!.dokumentUrl }
            .`as`("url")
            .isEqualTo("mbdok://brevklient/system/brukernavn/dokument/dokumentreferanse?token=urlMedTilgangToken&server=http%3A%2F%2Fbrevserver")
    }

    @Test
    @DisplayName("skal hente dokument")
    fun skalHenteDokument() {
        val dokumentReferanse = "11111111"
        every { dokumentTilgangConsumer!!.bestillDokumenttilgang(any()) } returns Dokumenttilgang(null, null, "urlMedTilgangToken", null, null, Brev())
        every { dokumentConsumer!!.henteDokument(any()) } returns Optional.of("test".toByteArray(StandardCharsets.UTF_8))
        every { journalpostService!!.hentJournalpostEntitet(any()) } returns
            Optional.of(
                JournalpostBygger
                    .enJournalfortJournalpost()
                    .medJournalpostId(123213)
                    .medDokumentreferanse(dokumentReferanse)
                    .hent(),
            )
        val dokumentResponse = httpHeaderTestRestTemplate.getForEntity<ByteArray>(dokumentUrl() + "/BID-111111/" + dokumentReferanse)
        Assertions.assertThat(dokumentResponse.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
        val dokumentByte = dokumentResponse.body!!
        Assertions.assertThat(String(dokumentByte)).isEqualTo("test")
    }

    @Test
    @DisplayName("skal hente dokument uten dokumentreferanse")
    fun skalHenteDokumentUtenDokref() {
        val dokumentReferanse = "11111111"
        every { dokumentTilgangConsumer!!.bestillDokumenttilgang(any()) } returns Dokumenttilgang(null, null, "urlMedTilgangToken", null, null, Brev())
        every { dokumentConsumer!!.henteDokument(any()) } returns Optional.of("test".toByteArray(StandardCharsets.UTF_8))
        every { journalpostService!!.hentJournalpostEntitet(any()) } returns
            Optional.of(
                JournalpostBygger
                    .enJournalfortJournalpost()
                    .medJournalpostId(123213)
                    .medDokumentreferanse(dokumentReferanse)
                    .hent(),
            )
        val dokumentResponse = httpHeaderTestRestTemplate.getForEntity<ByteArray>(dokumentUrl() + "/BID-111111")
        Assertions.assertThat(dokumentResponse.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
        val dokumentByte = dokumentResponse.body!!
        Assertions.assertThat(String(dokumentByte)).isEqualTo("test")
    }

    @Test
    @DisplayName("skal ikke hente dokument under produksjon")
    fun skalIkkeHenteDokumentUnderProduksjon() {
        val dokumentReferanse = "11111111"
        every { dokumentTilgangConsumer!!.bestillDokumenttilgang(any()) } returns Dokumenttilgang(null, null, "urlMedTilgangToken", null, null, Brev())
        every { dokumentConsumer!!.henteDokument(any()) } returns Optional.of("test".toByteArray(StandardCharsets.UTF_8))
        every { journalpostService!!.hentJournalpostEntitet(any()) } returns
            Optional.of(
                JournalpostBygger
                    .enJournalpost()
                    .medJournalstatus(Journalstatus.UNDER_PRODUKSJON)
                    .medJournalpostId(123213)
                    .medDokumentreferanse(dokumentReferanse)
                    .hent(),
            )
        val dokumentResponse = httpHeaderTestRestTemplate.getForEntity<ByteArray>(dokumentUrl() + "/BID-111111")
        Assertions.assertThat(dokumentResponse.statusCode).`as`("status").isEqualTo(HttpStatus.BAD_REQUEST)
        Assertions
            .assertThat(dokumentResponse.headers[HttpHeaders.WARNING]!![0])
            .`as`("warning")
            .isEqualTo("Kan ikke hente dokument under produksjon, journalpostId=123213")
    }

    @Test
    @DisplayName("skal hente dokument metadata")
    fun skalHenteDokumentMetadata() {
        val dokumentReferanse = "11111111"
        every { journalpostService!!.hentJournalpostEntitetForId(any()) } returns
            JournalpostBygger
                .enJournalfortJournalpost()
                .medJournalpostId(123213)
                .medDokumentreferanse(dokumentReferanse)
                .medJournalstatus("D")
                .hent()

        val dokumentResponse = httpHeaderTestRestTemplate.exchange<List<DokumentMetadata>>(URI.create(dokumentUrl() + "/BID-111111"), HttpMethod.OPTIONS)
        Assertions.assertThat(dokumentResponse.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
        val dokumentMetadata = dokumentResponse.body!!
        Assertions.assertThat(dokumentMetadata.size).isEqualTo(1)
        Assertions.assertThat(dokumentMetadata[0].dokumentreferanse).isEqualTo(dokumentReferanse)
        Assertions.assertThat(dokumentMetadata[0].journalpostId).isEqualTo("BID-123213")
        Assertions.assertThat(dokumentMetadata[0].format).isEqualTo(DokumentFormatDto.MBDOK)
        Assertions.assertThat(dokumentMetadata[0].status).isEqualTo(DokumentStatusDto.UNDER_REDIGERING)
        Assertions.assertThat(dokumentMetadata[0].arkivsystem).isEqualTo(DokumentArkivSystemDto.MIDLERTIDLIG_BREVLAGER)
    }

    @Test
    @DisplayName("skal hente dokument metadata med journalpostid og dokumentreferanse")
    fun skalHenteDokumentMetadataMedJournalpostIdOgDokumentReferanse() {
        val dokumentReferanse = "11111111"
        every { journalpostService!!.hentJournalpostEntitetForId(any()) } returns
            JournalpostBygger
                .enJournalfortJournalpost()
                .medJournalpostId(123213)
                .medDokumentreferanse(dokumentReferanse)
                .medJournalstatus("D")
                .hent()

        val dokumentResponse = httpHeaderTestRestTemplate.exchange<List<DokumentMetadata>>(URI.create(dokumentUrl() + "/BID-111111/11111111"), HttpMethod.OPTIONS)
        Assertions.assertThat(dokumentResponse.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
        val dokumentMetadata = dokumentResponse.body!!
        Assertions.assertThat(dokumentMetadata.size).isEqualTo(1)
        Assertions.assertThat(dokumentMetadata[0].dokumentreferanse).isEqualTo(dokumentReferanse)
        Assertions.assertThat(dokumentMetadata[0].journalpostId).isEqualTo("BID-123213")
        Assertions.assertThat(dokumentMetadata[0].format).isEqualTo(DokumentFormatDto.MBDOK)
        Assertions.assertThat(dokumentMetadata[0].status).isEqualTo(DokumentStatusDto.UNDER_REDIGERING)
        Assertions.assertThat(dokumentMetadata[0].arkivsystem).isEqualTo(DokumentArkivSystemDto.MIDLERTIDLIG_BREVLAGER)
    }

    @Test
    @DisplayName("skal hente dokument metadata med dokumentreferanse")
    fun skalHenteDokumentMetadataMedDokumentReferanse() {
        val dokumentReferanse = "11111111"
        every { journalpostService!!.hentJournalpostEntitetForId(any()) } returns
            JournalpostBygger
                .enJournalfortJournalpost()
                .medJournalpostId(123213)
                .medDokumentreferanse(dokumentReferanse)
                .medJournalstatus("D")
                .hent()

        every { journalpostService!!.hentJournalpostForDokumentReferanse(any()) } returns
            JournalpostBygger
                .enJournalfortJournalpost()
                .medJournalpostId(123213)
                .medDokumentreferanse(dokumentReferanse)
                .medJournalstatus("D")
                .hent()

        val dokumentResponse = httpHeaderTestRestTemplate.exchange<List<DokumentMetadata>>(URI.create(dokumentReferanseUrl() + "11111111"), HttpMethod.OPTIONS)
        Assertions.assertThat(dokumentResponse.statusCode).`as`("status").isEqualTo(HttpStatus.OK)
        val dokumentMetadata = dokumentResponse.body!!
        Assertions.assertThat(dokumentMetadata.size).isEqualTo(1)
        Assertions.assertThat(dokumentMetadata[0].dokumentreferanse).isEqualTo(dokumentReferanse)
        Assertions.assertThat(dokumentMetadata[0].journalpostId).isEqualTo("BID-123213")
        Assertions.assertThat(dokumentMetadata[0].format).isEqualTo(DokumentFormatDto.MBDOK)
        Assertions.assertThat(dokumentMetadata[0].status).isEqualTo(DokumentStatusDto.UNDER_REDIGERING)
        Assertions.assertThat(dokumentMetadata[0].arkivsystem).isEqualTo(DokumentArkivSystemDto.MIDLERTIDLIG_BREVLAGER)
    }

    @Test
    @DisplayName("Hent dokument metadata skal feile hvis dokument ikke finnes")
    fun hentDokumentMetadataSkalFeileHvisDokumentIkkeFinnes() {
        val dokumentReferanse = "11111111"
        every { journalpostService!!.hentJournalpostEntitet(any()) } returns
            Optional.of(
                JournalpostBygger
                    .enJournalfortJournalpost()
                    .medJournalpostId(123213)
                    .medDokumentreferanse(dokumentReferanse)
                    .medJournalstatus("D")
                    .hent(),
            )

        val dokumentResponse = httpHeaderTestRestTemplate.getForEntity<Void>(dokumentUrl() + "/BID-111111/123213")
        Assertions.assertThat(dokumentResponse.statusCode).`as`("status").isEqualTo(HttpStatus.NOT_FOUND)
    }

    private fun tilgangUrl(): String = "/tilgang/"

    private fun dokumentUrl(): String = "/dokument"

    private fun dokumentReferanseUrl(): String = "/dokumentreferanse/"
}
