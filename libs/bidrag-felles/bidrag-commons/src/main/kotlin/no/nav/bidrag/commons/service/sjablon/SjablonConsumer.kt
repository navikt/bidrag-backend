package no.nav.bidrag.commons.service.sjablon

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.service.retryTemplateSynchronous
import no.nav.bidrag.commons.util.secureLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

private val logger = KotlinLogging.logger {}

/**
 * SjablonConsumer brukes for å hente sjablontall.
 *
 * For å ta i bruker provideren må følgende annotering legges til i konfigurasjon
 *
 * ```kotlin
 * @EnableSjablonProvider
 * ```
 *
 * Hvor følgende miljøvariabler er definert i nais konfigurasjonen og outbound traffik er tillatt for BIDRAG_SJABLON_URL
 * ```yaml
 *   BIDRAG_SJABLON_URL: https://bidrag-sjablon.<cluster>-pub.nais.io/bidrag-sjablon
 * ```
 */
@Component("CommonsSjablonConsumer")
class SjablonConsumer(
    @Value("\${BIDRAG_SJABLON_URL}") url: String,
) {
    companion object {
        const val SJABLONTALL_CACHE = "SJABLONTALL_CACHE"
        const val SJABLONSAMVÆRSFRADRAG_CACHE = "SJABLONSAMVÆRSFRADRAG_CACHE"
        const val SJABLONBIDRAGSEVNE_CACHE = "SJABLONBIDRAGSEVNE_CACHE"
        const val SJABLONTRINNVISSKATTESATS_CACHE = "SJABLONTRINNVISSKATTESATS_CACHE"
        const val SJABLONBARNETILSYN_CACHE = "SJABLONBARNETILSYN_CACHE"
        const val SJABLONFORBRUKSUTGIFTER_CACHE = "SJABLONFORBRUKSUTGIFTER_CACHE"
        const val SJABLONMAKSFRADRAG_CACHE = "SJABLONMAKSFRADRAG_CACHE"
        const val SJABLONMAKSTILSYN_CACHE = "SJABLONMAKSTILSYN_CACHE"
    }

    private val sjablontallUrl = "/sjablontall/all"
    private val sjablonSamværsfradragUrl = "/samvaersfradrag/all"
    private val sjablonBidragsevneUrl = "/bidragsevner/all"
    private val sjablonTrinnvisSkattesatsUrl = "/trinnvisskattesats/all"
    private val sjablonBarnetilsynUrl = "/barnetilsyn/all"
    private val sjablonForbruksutgifterUrl = "/forbruksutgifter/all"
    private val sjablonMaksFradragUrl = "/maksfradrag/all"
    private val sjablonMaksTilsynUrl = "/makstilsyn/all"

    val restTemplate: RestTemplate =
        RestTemplateBuilder()
            .rootUri(url)
            .defaultHeader("Nav-Call-Id", CorrelationId.fetchCorrelationIdForThread())
            .defaultHeader("Nav-Consumer-Id", System.getenv("NAIS_APP_NAME") ?: "bidrag-commons")
            .build()

    @Cacheable(SJABLONTALL_CACHE)
    fun hentSjablontall(): ResponseEntity<List<Sjablontall>> {
        try {
            val sjablonResponse =
                restTemplate.exchange(
                    sjablontallUrl,
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<List<Sjablontall>>() {},
                )
            logger.debug { "Hentet sjablontall fra bidrag-sjablon" }
            return sjablonResponse
        } catch (exception: RestClientResponseException) {
            logger.error(exception) {
                "Det skjedde en feil ved henting av sjablontall med feil ${exception.statusText} og feilmelding " +
                    "${exception.message}"
            }
            throw exception
        }
    }

    @Cacheable(SJABLONSAMVÆRSFRADRAG_CACHE)
    fun hentSjablonSamværsfradrag(): ResponseEntity<List<Samværsfradrag>> {
        try {
            val sjablonResponse =
                restTemplate.exchange(
                    sjablonSamværsfradragUrl,
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<List<Samværsfradrag>>() {},
                )

            logger.debug { "Hentet sjabloner for samværsfradrag fra bidrag-sjablon" }
            return sjablonResponse
        } catch (exception: RestClientResponseException) {
            logger.error(exception) {
                "Det skjedde en feil ved henting av sjabloner for samværsfradrag med feil ${exception.statusText} og feilmelding " +
                    "${exception.message}"
            }
            throw exception
        }
    }

    @Cacheable(SJABLONBIDRAGSEVNE_CACHE)
    fun hentSjablonBidragsevne(): ResponseEntity<List<Bidragsevne>> {
        try {
            val sjablonResponse =
                restTemplate.exchange(
                    sjablonBidragsevneUrl,
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<List<Bidragsevne>>() {},
                )

            logger.debug { "Hentet sjabloner for bidragsevne fra bidrag-sjablon" }
            return sjablonResponse
        } catch (exception: RestClientResponseException) {
            logger.error(exception) {
                "Det skjedde en feil ved henting av sjabloner for bidragsevne med feil ${exception.statusText} og feilmelding " +
                    "${exception.message}"
            }
            throw exception
        }
    }

    @Cacheable(SJABLONTRINNVISSKATTESATS_CACHE)
    fun hentSjablonTrinnvisSkattesats(): ResponseEntity<List<TrinnvisSkattesats>> {
        try {
            val sjablonResponse =
                restTemplate.exchange(
                    sjablonTrinnvisSkattesatsUrl,
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<List<TrinnvisSkattesats>>() {},
                )

            logger.debug { "Hentet sjabloner for trinnvis skattesats fra bidrag-sjablon" }
            return sjablonResponse
        } catch (exception: RestClientResponseException) {
            logger.error(exception) {
                "Det skjedde en feil ved henting av sjabloner for trinnvis skattesats med feil ${exception.statusText} og feilmelding " +
                    "${exception.message}"
            }
            throw exception
        }
    }

    @Cacheable(SJABLONBARNETILSYN_CACHE)
    fun hentSjablonBarnetilsyn(): ResponseEntity<List<Barnetilsyn>> {
        try {
            val sjablonResponse =
                restTemplate.exchange(
                    sjablonBarnetilsynUrl,
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<List<Barnetilsyn>>() {},
                )

            logger.debug { "Hentet sjabloner for barnetilsyn fra bidrag-sjablon" }
            return sjablonResponse
        } catch (exception: RestClientResponseException) {
            logger.error(exception) {
                "Det skjedde en feil ved henting av sjabloner for barnetilsyn med feil ${exception.statusText} og feilmelding " +
                    "${exception.message}"
            }
            throw exception
        }
    }

    @Cacheable(SJABLONFORBRUKSUTGIFTER_CACHE)
    fun hentSjablonForbruksutgifter(): ResponseEntity<List<Forbruksutgifter>> {
        try {
            val sjablonResponse =
                restTemplate.exchange(
                    sjablonForbruksutgifterUrl,
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<List<Forbruksutgifter>>() {},
                )

            logger.debug { "Hentet sjabloner for forbruksutgifter fra bidrag-sjablon" }
            return sjablonResponse
        } catch (exception: RestClientResponseException) {
            logger.error(exception) {
                "Det skjedde en feil ved henting av sjabloner for forbruksutgifter med feil ${exception.statusText} og feilmelding " +
                    "${exception.message}"
            }
            throw exception
        }
    }

    @Cacheable(SJABLONMAKSFRADRAG_CACHE)
    fun hentSjablonMaksFradrag(): ResponseEntity<List<MaksFradrag>> {
        try {
            val sjablonResponse =
                restTemplate.exchange(
                    sjablonMaksFradragUrl,
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<List<MaksFradrag>>() {},
                )

            logger.debug { "Hentet sjabloner for maks fradrag fra bidrag-sjablon" }
            return sjablonResponse
        } catch (exception: RestClientResponseException) {
            logger.error(exception) {
                "Det skjedde en feil ved henting av sjabloner for maks fradrag med feil ${exception.statusText} og feilmelding " +
                    "${exception.message}"
            }
            throw exception
        }
    }

    @Cacheable(SJABLONMAKSTILSYN_CACHE)
    fun hentSjablonMaksTilsyn(): ResponseEntity<List<MaksTilsyn>> {
        try {
            val sjablonResponse =
                restTemplate.exchange(
                    sjablonMaksTilsynUrl,
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<List<MaksTilsyn>>() {},
                )

            logger.debug { "Hentet sjabloner for maks tilsyn fra bidrag-sjablon" }
            return sjablonResponse
        } catch (exception: RestClientResponseException) {
            logger.error(exception) {
                "Det skjedde en feil ved henting av sjabloner for maks tilsyn med feil ${exception.statusText} og feilmelding " +
                    "${exception.message}"
            }
            throw exception
        }
    }
}

class SjablonProvider {
    companion object {
        /**
         * Hent liste over alle sjablonverdier
         */
        fun hentSjablontall(): List<Sjablontall> =
            try {
                retryTemplateSynchronous(
                    "SjablonProvider.hentSjablontall",
                ).execute<List<Sjablontall>, HttpClientErrorException> {
                    AppContext.getBean("CommonsSjablonConsumer", SjablonConsumer::class.java).hentSjablontall().body
                }
            } catch (e: Exception) {
                secureLogger.error(e) { "Det skjedde en feil ved henting av sjablontall" }
                emptyList()
            }

        fun hentSjablonSamværsfradrag(): List<Samværsfradrag> =
            try {
                retryTemplateSynchronous(
                    "SjablonProvider.hentSjablonSamværsfradrag",
                ).execute<List<Samværsfradrag>, HttpClientErrorException> {
                    AppContext.getBean("CommonsSjablonConsumer", SjablonConsumer::class.java).hentSjablonSamværsfradrag().body
                }
            } catch (e: Exception) {
                secureLogger.error(e) { "Det skjedde en feil ved henting av sjablon for samværsfradrag" }
                emptyList()
            }

        fun hentSjablonBidragsevne(): List<Bidragsevne> =
            try {
                retryTemplateSynchronous(
                    "SjablonProvider.hentSjablonBidragsevne",
                ).execute<List<Bidragsevne>, HttpClientErrorException> {
                    AppContext.getBean("CommonsSjablonConsumer", SjablonConsumer::class.java).hentSjablonBidragsevne().body
                }
            } catch (e: Exception) {
                secureLogger.error(e) { "Det skjedde en feil ved henting av sjablon for bidragsevne" }
                emptyList()
            }

        fun hentSjablonTrinnvisSkattesats(): List<TrinnvisSkattesats> =
            try {
                retryTemplateSynchronous(
                    "SjablonProvider.hentSjablonTrinnvisSkattesats",
                ).execute<List<TrinnvisSkattesats>, HttpClientErrorException> {
                    AppContext.getBean("CommonsSjablonConsumer", SjablonConsumer::class.java).hentSjablonTrinnvisSkattesats().body
                }
            } catch (e: Exception) {
                secureLogger.error(e) { "Det skjedde en feil ved henting av sjablon for trinnvis skattesats" }
                emptyList()
            }

        fun hentSjablonBarnetilsyn(): List<Barnetilsyn> =
            try {
                retryTemplateSynchronous(
                    "SjablonProvider.hentSjablonBarnetilsyn",
                ).execute<List<Barnetilsyn>, HttpClientErrorException> {
                    AppContext.getBean("CommonsSjablonConsumer", SjablonConsumer::class.java).hentSjablonBarnetilsyn().body
                }
            } catch (e: Exception) {
                secureLogger.error(e) { "Det skjedde en feil ved henting av sjablon for barnetilsyn" }
                emptyList()
            }

        fun hentSjablonForbruksutgifter(): List<Forbruksutgifter> =
            try {
                retryTemplateSynchronous(
                    "SjablonProvider.hentSjablonForbruksutgifter",
                ).execute<List<Forbruksutgifter>, HttpClientErrorException> {
                    AppContext.getBean("CommonsSjablonConsumer", SjablonConsumer::class.java).hentSjablonForbruksutgifter().body
                }
            } catch (e: Exception) {
                secureLogger.error(e) { "Det skjedde en feil ved henting av sjablon for forbruksutgifter" }
                emptyList()
            }

        fun hentSjablonMaksFradrag(): List<MaksFradrag> =
            try {
                retryTemplateSynchronous(
                    "SjablonProvider.hentSjablonMaksFradrag",
                ).execute<List<MaksFradrag>, HttpClientErrorException> {
                    AppContext.getBean("CommonsSjablonConsumer", SjablonConsumer::class.java).hentSjablonMaksFradrag().body
                }
            } catch (e: Exception) {
                secureLogger.error(e) { "Det skjedde en feil ved henting av sjablon for maks fradrag" }
                emptyList()
            }

        fun hentSjablonMaksTilsyn(): List<MaksTilsyn> =
            try {
                retryTemplateSynchronous(
                    "SjablonProvider.hentSjablonMaksTilsyn",
                ).execute<List<MaksTilsyn>, HttpClientErrorException> {
                    AppContext.getBean("CommonsSjablonConsumer", SjablonConsumer::class.java).hentSjablonMaksTilsyn().body
                }
            } catch (e: Exception) {
                secureLogger.error(e) { "Det skjedde en feil ved henting av sjablon for maks tilsyn" }
                emptyList()
            }
    }
}
