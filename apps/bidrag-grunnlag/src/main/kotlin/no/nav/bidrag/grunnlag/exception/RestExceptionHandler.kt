package no.nav.bidrag.grunnlag.exception

import no.nav.bidrag.commons.util.LoggingRetryListener
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryPolicy
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.context.RetryContextSupport
import org.springframework.retry.support.RetryTemplate
import org.springframework.util.ClassUtils
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.net.SocketTimeoutException
import java.net.URI

sealed class RestResponse<T> {
    data class Success<T>(val body: T) : RestResponse<T>()
    data class Failure<T>(val message: String?, val statusCode: HttpStatusCode, val restClientException: Exception) : RestResponse<T>()
}

fun httpRetryTemplate(details: String? = null): RetryTemplate {
    val retryTemplate = RetryTemplate()
    val fixedBackOffPolicy = FixedBackOffPolicy()
    fixedBackOffPolicy.backOffPeriod = 500L
    retryTemplate.setBackOffPolicy(fixedBackOffPolicy)
    val retryPolicy = HttpRetryPolicy()
    retryTemplate.setRetryPolicy(retryPolicy)
    retryTemplate.setThrowLastExceptionOnExhausted(true)
    retryTemplate.registerListener(LoggingRetryListener(details))
    return retryTemplate
}

class HttpRetryPolicy(
    private val maxAttempts: Int = 3,
    private val ignoreHttpStatus: List<HttpStatus> = listOf(HttpStatus.NOT_FOUND, HttpStatus.BAD_REQUEST),
) : RetryPolicy {
    internal class HttpRetryContext(parent: RetryContext?) : RetryContextSupport(parent)

    override fun canRetry(context: RetryContext): Boolean {
        val throwable = context.lastThrowable
        val ignoreException =
            (throwable != null) &&
                (
                    (throwable.cause is SocketTimeoutException) ||
                        ((throwable is HttpStatusCodeException) && ignoreHttpStatus.contains(throwable.statusCode))
                    )
        val shouldRetry = context.retryCount < maxAttempts &&
            (context.lastThrowable == null || !ignoreException)
        if (!shouldRetry && throwable != null) {
            context.setAttribute(RetryContext.NO_RECOVERY, true)
        } else {
            context.removeAttribute(RetryContext.NO_RECOVERY)
        }
        return shouldRetry
    }

    override fun close(status: RetryContext) {
    }

    override fun registerThrowable(context: RetryContext, throwable: Throwable?) {
        val httpRetryContext = (context as HttpRetryContext)
        httpRetryContext.registerThrowable(throwable)
    }

    override fun open(parent: RetryContext?): RetryContext = HttpRetryContext(parent)

    override fun toString(): String = ClassUtils.getShortName(javaClass) + "[maxAttempts=$maxAttempts, ignoreHttpStatus=$ignoreHttpStatus]"
}

fun <T : Any> RestTemplate.tryExchange(
    url: String,
    httpMethod: HttpMethod,
    httpEntity: HttpEntity<*>,
    responseType: Class<T>,
    fallbackBody: T,
): RestResponse<T> = try {
    val requestUri = URI.create(url)
    val response = httpRetryTemplate(
        url,
    ).execute<ResponseEntity<T>, HttpClientErrorException> {
        exchange(requestUri, httpMethod, httpEntity, responseType)
    }
    if (response.statusCode == HttpStatus.OK) {
        RestResponse.Success(response.body ?: fallbackBody)
    } else {
        RestResponse.Failure(
            message = response.headers.getOrEmpty(HttpHeaders.WARNING).joinToString(","),
            statusCode = response.statusCode,
            restClientException = HttpClientErrorException(response.statusCode),
        )
    }
} catch (e: HttpClientErrorException) {
    RestResponse.Failure("Message: ${e.message}", e.statusCode, e)
} catch (e: HttpServerErrorException) {
    RestResponse.Failure("Message: ${e.message}", e.statusCode, e)
} catch (e: Exception) {
    RestResponse.Failure("Message: ${e.message}", HttpStatus.INTERNAL_SERVER_ERROR, e)
}

// Brukes hvis responseType er en liste
fun <T : Any> RestTemplate.tryExchange(
    url: String,
    httpMethod: HttpMethod,
    httpEntity: HttpEntity<*>,
    responseType: ParameterizedTypeReference<T>,
    fallbackBody: T,
): RestResponse<T> = try {
    val requestUri = URI.create(url)
    val response = httpRetryTemplate(
        url,
    ).execute<ResponseEntity<T>, HttpClientErrorException> {
        exchange(requestUri, httpMethod, httpEntity, responseType)
    }
    if (response.statusCode == HttpStatus.OK) {
        RestResponse.Success(response.body ?: fallbackBody)
    } else {
        RestResponse.Failure(
            message = response.headers.getOrEmpty(HttpHeaders.WARNING).joinToString(","),
            statusCode = response.statusCode,
            restClientException = HttpClientErrorException(response.statusCode),
        )
    }
} catch (e: HttpClientErrorException) {
    RestResponse.Failure("Message: ${e.message}", e.statusCode, e)
} catch (e: HttpServerErrorException) {
    RestResponse.Failure("Message: ${e.message}", e.statusCode, e)
}
