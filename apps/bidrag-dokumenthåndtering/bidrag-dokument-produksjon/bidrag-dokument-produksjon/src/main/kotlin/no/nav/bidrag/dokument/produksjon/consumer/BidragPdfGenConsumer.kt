package no.nav.bidrag.dokument.produksjon.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import org.springframework.web.util.UriComponentsBuilder
import java.math.BigDecimal

private val log = KotlinLogging.logger {}

data class Configuration(
    val scale: BigDecimal = BigDecimal.ONE,
    val convertToPDFA: Boolean = false,
)

@Component
class BidragPdfGenConsumer(
    @Value("\${bidrag-pdfgen.url}") private val url: String,
) {
    // Singleton RestTemplate — avoids TCP connection overhead on every request
    private val restTemplate: RestTemplate = RestTemplateBuilder().build()

    fun flattenPDF(pdf: ByteArray): ByteArray? = try {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_PDF }
        restTemplate.postForEntity<ByteArray>("$url/flatten", HttpEntity(pdf, headers)).body
    } catch (e: Exception) {
        log.error(e) { "Feil ved flattening av PDF mot $url" }
        null
    }

    fun produserPdf(
        html: String,
        configuration: Configuration = Configuration(),
    ): ByteArray? = try {
        val convertUrl =
            UriComponentsBuilder
                .fromUriString("$url/convert")
                .queryParam("scale", configuration.scale)
                .queryParam("pdfa", configuration.convertToPDFA)
                .toUriString()

        // Charset må angis eksplisitt. Uten den tolkes text/html som
        // ISO-8859-1 av HTTP-standarden, og norske tegn (æøå) blir ødelagt.
        val headers =
            HttpHeaders().apply {
                contentType = MediaType(MediaType.TEXT_HTML, Charsets.UTF_8)
            }
        restTemplate
            .postForEntity<ByteArray>(
                convertUrl,
                HttpEntity(html.toByteArray(Charsets.UTF_8), headers),
            ).body
    } catch (e: Exception) {
        log.error(e) { "Feil ved generering av PDF mot $url" }
        null
    }
}
