package no.nav.bidrag.dokument.produksjon.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

private val log = KotlinLogging.logger {}

enum class RenderPDFVersion { V1, V2 }

@Component
class BidragDokumentmalConsumer(
    @Value("\${bidrag-dokumentmal.url}") private val url: String,
) {
    fun hentDokumentmal(
        category: String,
        type: String,
        payload: String,
        renderforpdf: Boolean = true,
    ): String? = try {
        val malUrl = "$url/$category/$type"
        val restTemplate: RestTemplate =
            RestTemplateBuilder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                .defaultHeader("renderforpdf", renderforpdf.toString())
                .defaultHeader("renderPDFVersion", RenderPDFVersion.V2.name)
                .build()
        restTemplate.postForEntity<ByteArray>(malUrl, payload).body?.toString(Charsets.UTF_8)
    } catch (e: Exception) {
        log.error(e) { "Det skjedde en feil ved henting av dokumentmal fra url $url" }
        null
    }
}
