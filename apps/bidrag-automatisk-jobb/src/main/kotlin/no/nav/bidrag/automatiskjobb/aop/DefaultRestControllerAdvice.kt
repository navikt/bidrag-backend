package no.nav.bidrag.automatiskjobb.aop

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.postgresql.util.PSQLException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpStatusCodeException

@RestControllerAdvice
class DefaultRestControllerAdvice {
    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    @ResponseBody
    @ExceptionHandler(HttpStatusCodeException::class)
    fun handleHttpClientErrorException(exception: HttpStatusCodeException): ResponseEntity<*> {
        val errorMessage = getErrorMessage(exception)
        LOGGER.warn(exception) { errorMessage }
        return ResponseEntity
            .status(exception.statusCode)
            .build<Any>()
    }

    private fun getErrorMessage(exception: HttpStatusCodeException): String {
        val errorMessage = StringBuilder()
        errorMessage.append("Det skjedde en feil ved kall mot ekstern tjeneste: ")
        exception.responseHeaders
            ?.get(HttpHeaders.WARNING)
            ?.firstOrNull()
            ?.let { errorMessage.append(it) }
        if (exception.statusText.isNotEmpty()) {
            errorMessage.append(" - ")
            errorMessage.append(exception.statusText)
        }
        return errorMessage.toString()
    }

    @ResponseBody
    @ExceptionHandler(Exception::class)
    fun handleOtherExceptions(exception: Exception): ResponseEntity<*> {
        LOGGER.warn(exception) { "Det skjedde en ukjent feil" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.WARNING, "Det skjedde en ukjent feil")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun handleUnauthorizedException(exception: JwtTokenUnauthorizedException): ResponseEntity<*> {
        LOGGER.warn(exception) { "Ugyldig eller manglende sikkerhetstoken" }
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.WARNING, "Ugyldig eller manglende sikkerhetstoken")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler(PSQLException::class)
    fun handlePSQLException(exception: PSQLException): ResponseEntity<*> {
        LOGGER.error(exception) { "Brudd på constraint ved insert i database" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.WARNING, "Brudd på constraint ved insert i database, se logger for mer info.")
            .build<Any>()
    }
}
