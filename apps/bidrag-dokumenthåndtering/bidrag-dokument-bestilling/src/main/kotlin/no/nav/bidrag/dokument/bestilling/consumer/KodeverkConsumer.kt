package no.nav.bidrag.dokument.bestilling.consumer

import jakarta.annotation.PostConstruct
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import no.nav.bidrag.dokument.bestilling.config.CacheConfig.Companion.LANDKODER_CACHE
import no.nav.bidrag.dokument.bestilling.config.CacheConfig.Companion.LANDKODER_ISO2_CACHE
import no.nav.bidrag.dokument.bestilling.consumer.dto.KodeverkResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.DefaultUriBuilderFactory

@Service
class KodeverkConsumer(
    @Value($$"${KODEVERK_URL}") kodeverkUrl: String,
    val cacheManager: CacheManager,
) {
    private final val restTemplate: RestTemplate

    init {
        val restTemplate = HttpHeaderRestTemplate()
        restTemplate.uriTemplateHandler = DefaultUriBuilderFactory("$kodeverkUrl/kodeverk")
        this.restTemplate = restTemplate
    }

    @PostConstruct
    fun preloadKodeverkValues() {
        loadLandkoder()
        loadLandkoderISO2()
    }

    private fun loadLandkoder() {
        LOGGER.info("Henter Landkoder fra kodeverk")
        val response =
            restTemplate.exchange<KodeverkResponse>(
                "/Landkoder",
                HttpMethod.GET,
                null,
            )
        val kommuner = response.body
        cacheManager.getCache(LANDKODER_CACHE)?.put(DEFAULT_CACHE, kommuner)
    }

    fun hentLandkoder(): KodeverkResponse? = cacheManager
        .getCache(LANDKODER_CACHE)
        ?.get(DEFAULT_CACHE, KodeverkResponse::class.java)

    private fun loadLandkoderISO2() {
        LOGGER.info("Henter LandkoderISO2 fra kodeverk")
        val response =
            restTemplate.exchange(
                "/LandkoderISO2",
                HttpMethod.GET,
                null,
                KodeverkResponse::class.java,
            )
        val landkoder = response.body
        cacheManager.getCache(LANDKODER_ISO2_CACHE)?.put(DEFAULT_CACHE, landkoder)
    }

    fun hentLandkoderISO2(): KodeverkResponse? = cacheManager
        .getCache(LANDKODER_ISO2_CACHE)
        ?.get(DEFAULT_CACHE, KodeverkResponse::class.java)

    companion object {
        private val LOGGER = LoggerFactory.getLogger(KodeverkConsumer::class.java)
        private const val DEFAULT_CACHE = "DEFAULT"
    }
}
