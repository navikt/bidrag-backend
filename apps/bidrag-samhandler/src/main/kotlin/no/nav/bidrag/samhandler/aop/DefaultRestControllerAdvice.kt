package no.nav.bidrag.samhandler.aop

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.felles.ifTrue
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpStatusCodeException

private val LOGGER = KotlinLogging.logger { }

@RestControllerAdvice
class DefaultRestControllerAdvice {
    @ResponseBody
    @ExceptionHandler(HttpStatusCodeException::class)
    fun handleHttpClientErrorException(exception: HttpStatusCodeException): ResponseEntity<*> {
        val errorMessage = getErrorMessage(exception)
        LOGGER.warn { "Feil ved kall mot ekstern tjeneste med status ${exception.statusCode}" }
        val hasBody = exception.responseBodyAsString.isNotEmpty()
        val payloadFeilmelding =
            exception.responseBodyAsString.isEmpty().ifTrue { exception.message }
                ?: exception.responseBodyAsString
        secureLogger.warn(exception) { "Feil ved kall mot ekstern tjeneste: $payloadFeilmelding" }
        return ResponseEntity
            .status(exception.statusCode)
            .header(HttpHeaders.WARNING, errorMessage)
            .header(
                HttpHeaders.CONTENT_TYPE,
                if (hasBody) MediaType.APPLICATION_JSON_VALUE else MediaType.TEXT_PLAIN_VALUE,
            ).body(payloadFeilmelding)
    }

    private fun getErrorMessage(exception: HttpStatusCodeException): String {
        val errorMessage = StringBuilder()
        exception.responseHeaders
            ?.get(HttpHeaders.WARNING)
            ?.firstOrNull()
            ?.let { errorMessage.append(it) }
        if (exception.statusText.isNotEmpty()) {
            errorMessage.append(exception.statusText)
        }
        return errorMessage.toString()
    }

    @ResponseBody
    @ExceptionHandler(Exception::class)
    fun handleOtherExceptions(exception: Exception): ResponseEntity<*> {
        LOGGER.warn { "Det skjedde en ukjent feil av type ${exception.javaClass.simpleName}" }
        secureLogger.warn(exception) { "Det skjedde en ukjent feil" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.WARNING, "Det skjedde en ukjent feil: ${exception.message}")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleUnauthorizedException(exception: JwtTokenUnauthorizedException): ResponseEntity<*> {
        LOGGER.warn { "Ugyldig eller manglende sikkerhetstoken" }
        secureLogger.warn(exception) { "Ugyldig eller manglende sikkerhetstoken" }
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.WARNING, "Ugyldig eller manglende sikkerhetstoken")
            .build<Any>()
    }
}
