package no.nav.bidrag.person.aop

import com.fasterxml.jackson.databind.JsonMappingException
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.person.model.HttpStatusException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException

@RestControllerAdvice
class HttpStatusRestControllerAdvice {
    private val logger = KotlinLogging.logger {}

    @ResponseBody
    @ExceptionHandler
    fun handleOtherExceptions(exception: Exception): ResponseEntity<*> {
        logger.warn { "Det skjedde en ukjent feil ${exception.message}" }
        secureLogger.warn { exception.stackTraceToString() }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header(HttpHeaders.WARNING, exception.message ?: "Ukjent feil")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleHttpStatusException(exception: HttpStatusException): ResponseEntity<*> {
        logger.warn { exception.message }
        secureLogger.warn { exception.stackTraceToString() }
        return ResponseEntity
            .status(exception.status)
            .header(HttpHeaders.WARNING, exception.message ?: "Ukjent feil")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleJwtTokenUnauthorizedException(exception: JwtTokenUnauthorizedException): ResponseEntity<*> {
        logger.warn { exception.message }
        secureLogger.warn { exception.stackTraceToString() }
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.WARNING, exception.message ?: "Ukjent feil")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleHttClientErrorException(exception: HttpClientErrorException): ResponseEntity<*> = ResponseEntity
        .status(exception.statusCode)
        .header(HttpHeaders.WARNING, "Http client says: " + exception.message)
        .build<Any>()

    @ResponseBody
    @ExceptionHandler
    fun handleMissingKotlinParameterException(exception: JsonMappingException): ResponseEntity<*> {
        logger.warn { "Det skjedde en ukjent feil ${exception.message}" }
        secureLogger.warn { exception.stackTraceToString() }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.WARNING, exception.message ?: "Ukjent feil")
            .build<Any>()
    }

    @ResponseBody
    @ExceptionHandler
    fun handleMissingKotlinParameterException(exception: HttpMessageNotReadableException): ResponseEntity<*> {
        logger.warn { "Det skjedde en ukjent feil ${exception.message}" }
        secureLogger.warn { exception.stackTraceToString() }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .header(HttpHeaders.WARNING, exception.message ?: "Ukjent feil")
            .build<Any>()
    }
}
