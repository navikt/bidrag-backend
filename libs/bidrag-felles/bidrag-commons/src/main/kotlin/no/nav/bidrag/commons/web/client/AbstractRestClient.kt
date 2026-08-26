package no.nav.bidrag.commons.web.client

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import no.nav.bidrag.commons.util.secureLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestOperations
import org.springframework.web.client.exchange
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Abstract klasse for å kalle rest-tjenester med metrics og utpakking av ev. body.
 */
abstract class AbstractRestClient(
    val operations: RestOperations,
    metricsPrefix: String,
) {
    protected val responstid: Timer = Metrics.timer("$metricsPrefix.tid")
    protected val responsSuccess: Counter =
        Metrics.counter("$metricsPrefix.response", "status", "success")
    protected val responsFailure: Counter =
        Metrics.counter("$metricsPrefix.response", "status", "failure")

    protected val log: Logger = LoggerFactory.getLogger(this::class.java)

    protected inline fun <reified T : Any> getForEntity(uri: URI): T? = getForEntity(uri, null)

    protected inline fun <reified T : Any> getForNonNullEntity(uri: URI): T =
        getForEntity(uri, null) ?: throw HttpServerErrorException(HttpStatus.NOT_FOUND, uri.toString())

    protected inline fun <reified T : Any> getForEntity(
        uri: URI,
        httpHeaders: HttpHeaders?,
    ): T? =
        executeMedMetrics(uri) {
            operations.exchange(
                uri,
                HttpMethod.GET,
                HttpEntity(null, httpHeaders),
            )
        }

    protected inline fun <reified T : Any> optionsForEntity(uri: URI): T? = optionsForEntity(uri, null)

    protected inline fun <reified T : Any> optionsForEntity(
        uri: URI,
        httpHeaders: HttpHeaders?,
    ): T? =
        executeMedMetrics(uri) {
            operations.exchange(
                uri,
                HttpMethod.OPTIONS,
                HttpEntity(null, httpHeaders),
            )
        }

    protected inline fun <reified T : Any> postForNonNullEntity(
        uri: URI,
        payload: Any?,
    ): T = postForEntity(uri, payload, null) ?: throw HttpServerErrorException(HttpStatus.NOT_FOUND, uri.toString())

    protected inline fun <reified T : Any> postForNonNullEntity(
        uri: URI,
        payload: Any?,
        httpHeaders: HttpHeaders?,
    ): T = postForEntity(uri, payload, httpHeaders) ?: throw HttpServerErrorException(HttpStatus.NOT_FOUND, uri.toString())

    protected inline fun <reified T : Any> postForEntity(
        uri: URI,
        payload: Any?,
    ): T? = postForEntity(uri, payload, null)

    protected inline fun <reified T : Any> postForEntity(
        uri: URI,
        payload: Any?,
        httpHeaders: HttpHeaders?,
    ): T? =
        executeMedMetrics(uri) {
            operations.exchange(
                uri,
                HttpMethod.POST,
                HttpEntity(payload, httpHeaders),
            )
        }

    protected inline fun <reified T : Any> putForEntity(
        uri: URI,
        payload: Any,
    ): T? = putForEntity(uri, payload, null)

    protected inline fun <reified T : Any> putForEntity(
        uri: URI,
        payload: Any,
        httpHeaders: HttpHeaders?,
    ): T? =
        executeMedMetrics(uri) {
            operations.exchange(
                uri,
                HttpMethod.PUT,
                HttpEntity(payload, httpHeaders),
            )
        }

    protected inline fun <reified T : Any> patchForEntity(
        uri: URI,
        payload: Any,
    ): T? = patchForEntity(uri, payload, null)

    protected inline fun <reified T : Any> patchForEntity(
        uri: URI,
        payload: Any,
        httpHeaders: HttpHeaders?,
    ): T? =
        executeMedMetrics(uri) {
            operations.exchange(
                uri,
                HttpMethod.PATCH,
                HttpEntity(payload, httpHeaders),
            )
        }

    protected inline fun <reified T : Any> deleteForEntity(
        uri: URI,
        payload: Any? = null,
    ): T? = deleteForEntity(uri, payload, null)

    protected inline fun <reified T : Any> deleteForEntity(
        uri: URI,
        payload: Any?,
        httpHeaders: HttpHeaders?,
    ): T? =
        executeMedMetrics(uri) {
            operations.exchange(
                uri,
                HttpMethod.DELETE,
                HttpEntity(payload, httpHeaders),
            )
        }

    private fun <T : Any> validerOgPakkUt(
        respons: ResponseEntity<T>,
        uri: URI,
    ): T? {
        if (!respons.statusCode.is2xxSuccessful) {
            secureLogger.debug { "Kall mot $uri feilet:  ${respons.body}" }
            log.debug("Kall mot $uri feilet: ${respons.statusCode}")
            throw HttpServerErrorException(
                respons.statusCode,
                "",
                respons.body?.toString()?.toByteArray(),
                Charsets.UTF_8,
            )
        }
        return respons.body
    }

    protected fun <T : Any> executeMedMetrics(
        uri: URI,
        function: () -> ResponseEntity<T>,
    ): T? {
        try {
            val startTime = System.nanoTime()
            val responseEntity = function.invoke()
            responstid.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)
            responsSuccess.increment()
            return validerOgPakkUt(responseEntity, uri)
        } catch (e: RestClientResponseException) {
            responsFailure.increment()
            log.warn("RestClientResponseException ved kall mot uri=$uri. ${hentFeilmeldingFraWarningHeader(e)}", e)
            throw e
        } catch (e: Exception) {
            responsFailure.increment()
            log.warn("Feil ved kall mot uri=$uri", e)
            throw RuntimeException("Feil ved kall mot uri=$uri", e)
        }
    }

    private fun hentFeilmeldingFraWarningHeader(exception: RestClientResponseException): String =
        exception.responseHeaders?.get("Warning")?.let {
            "Detaljer: ${it.joinToString(", ")}"
        } ?: ""

    override fun toString(): String = this::class.simpleName + " [operations=" + operations + "]"
}
