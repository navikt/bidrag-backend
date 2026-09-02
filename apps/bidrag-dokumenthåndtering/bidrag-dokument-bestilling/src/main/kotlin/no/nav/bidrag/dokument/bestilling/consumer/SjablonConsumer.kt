package no.nav.bidrag.dokument.bestilling.consumer

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.dokument.bestilling.config.CacheConfig.Companion.SJABLONGER_CACHE
import no.nav.bidrag.dokument.bestilling.consumer.dto.SjablongerDto
import no.nav.bidrag.dokument.bestilling.model.parameterizedTypeReference
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.DefaultUriBuilderFactory

private val log = KotlinLogging.logger {}

@Service
class SjablonConsumer(
    @Value($$"${SJABLON_URL}") url: String,
    val cacheManager: CacheManager,
) {
    private final val restTemplate: RestTemplate

    init {
        val restTemplate = HttpHeaderRestTemplate()
        restTemplate.uriTemplateHandler = DefaultUriBuilderFactory(url)
        restTemplate.addHeaderGenerator("Nav-Call-Id") { CorrelationId.generateTimestamped("bidrag-dokument-bestilling").get() }
        restTemplate.addHeaderGenerator("Nav-Consumer-Id") { "bidrag-dokument-bestilling" }
        this.restTemplate = restTemplate
        loadAllSjablonValues()
    }

    private fun loadAllSjablonValues() {
        log.info { "Henter alle sjablonger fra bidrag-sjablon" }
        val response = restTemplate.exchange("/sjablontall/all", HttpMethod.GET, null, parameterizedTypeReference<SjablongerDto>())
        cacheManager.getCache(SJABLONGER_CACHE)?.put(DEFAULT_CACHE, response.body)
    }

    fun hentSjablonger(): SjablongerDto? = cacheManager.getCache(SJABLONGER_CACHE)?.get(DEFAULT_CACHE, SjablongerDto::class.java)

    companion object {
        private const val DEFAULT_CACHE = "DEFAULT"
    }
}
