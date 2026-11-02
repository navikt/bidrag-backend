package no.nav.bidrag.rest

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

private val LOGGER = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalRestControllerAdvice : ResponseEntityExceptionHandler() {

    companion object {
        private const val EXTERNAL_SERVICE_ERROR_PREFIX = "Det skjedde en feil ved kall mot ekstern tjeneste: "
    }

    @ExceptionHandler(RestClientResponseException::class)
    fun handleRestTemplateException(
        exception: RestClientResponseException,
    ): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(
            exception.statusCode,
            exception.responseBodyAsString,
        ).apply {
            title = "Feil mot ekstern tjeneste"
        }

        LOGGER.warn(exception) { problem.detail }

        return problem
    }

    @ExceptionHandler(HttpStatusCodeException::class)
    fun handleHttpStatusCodeException(exception: HttpStatusCodeException): ProblemDetail {
        val errorMessage = getErrorMessage(exception)
        LOGGER.warn(exception) { errorMessage }
        return ProblemDetail.forStatusAndDetail(
            exception.statusCode,
            errorMessage,
        ).apply {
            title = "Feil mot ekstern tjeneste"
        }
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ProblemDetail {
        LOGGER.warn(exception) { "Det skjedde en ukjent feil" }
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Det skjedde en ukjent feil: ${exception.message}",
        ).apply {
            title = "Ukjent feil"
        }
    }

    private fun getErrorMessage(exception: HttpStatusCodeException): String {
        val errorMessage = StringBuilder()
        errorMessage.append(EXTERNAL_SERVICE_ERROR_PREFIX)
        exception.responseHeaders?.get(HttpHeaders.WARNING)?.firstOrNull()?.let { errorMessage.append(it) }
        if (exception.statusText.isNotEmpty()) {
            errorMessage.append(exception.statusText)
        }
        if (exception.message?.isNotEmpty() == true) {
            errorMessage.append(exception.message)
        }
        return errorMessage.toString()
    }
}
