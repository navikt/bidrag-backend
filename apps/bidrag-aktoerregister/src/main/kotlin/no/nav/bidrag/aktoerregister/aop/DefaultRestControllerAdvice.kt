package no.nav.bidrag.aktoerregister.aop

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.server.ResponseStatusException

private val LOGGER = KotlinLogging.logger {}

@RestControllerAdvice
class DefaultRestControllerAdvice {

    @ExceptionHandler(HttpStatusCodeException::class)
    fun handleHttpClientErrorException(exception: HttpStatusCodeException): ProblemDetail {
        val errorMessage = getErrorMessage(exception)
        LOGGER.warn(exception) { errorMessage }
        return ProblemDetail.forStatusAndDetail(
            exception.statusCode,
            errorMessage,
        ).apply {
            title = "Feil mot ekstern tjeneste"
        }
    }

    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleUnauthorizedException(exception: JwtTokenUnauthorizedException?): ProblemDetail {
        LOGGER.warn(exception) { "Ugyldig eller manglende sikkerhetstoken" }
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Ugyldig eller manglende sikkerhetstoken",
        ).apply {
            title = "Autentiseringsfeil"
        }
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handlerResponseStatusException(exception: ResponseStatusException): ProblemDetail {
        LOGGER.warn(exception) { exception.message }
        return ProblemDetail.forStatusAndDetail(
            exception.statusCode,
            exception.message,
        ).apply {
            title = "Forespørselsfeil"
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
        errorMessage.append("Det skjedde en feil ved kall mot ekstern tjeneste: ")
        exception.responseHeaders?.get(HttpHeaders.WARNING)?.firstOrNull()?.let { errorMessage.append(it) }
        if (exception.statusText.isNotEmpty()) {
            errorMessage.append(" - ")
            errorMessage.append(exception.statusText)
        }
        return errorMessage.toString()
    }
}
